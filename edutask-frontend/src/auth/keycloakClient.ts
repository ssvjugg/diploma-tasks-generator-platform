const KEYCLOAK_BASE_URL = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8085';
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? 'edutask';
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'edutask-frontend';

const TOKEN_STORAGE_KEY = 'edutask.auth.tokens';
const LOGIN_STORAGE_KEY = 'edutask.auth.login';
const TOKEN_REFRESH_THRESHOLD_MS = 30_000;

const SHA256_INITIAL_HASH = [
  0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
  0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
] as const;

const SHA256_K = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
  0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
  0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
  0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
  0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
  0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
  0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
  0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
  0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
] as const;

type LoginState = {
  state: string;
  codeVerifier: string;
  redirectUri: string;
  returnTo: string;
};

type StoredTokens = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  expiresAt: number;
  refreshExpiresAt?: number;
};

type TokenResponse = {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
  refresh_expires_in?: number;
  id_token?: string;
};

export type AuthenticatedUser = {
  subject: string;
  username?: string;
  email?: string;
  name?: string;
  roles: string[];
};

type JwtPayload = {
  sub?: string;
  preferred_username?: string;
  email?: string;
  name?: string;
  realm_access?: {
    roles?: unknown;
  };
  resource_access?: Record<string, {
    roles?: unknown;
  }>;
};

const realmUrl = `${KEYCLOAK_BASE_URL.replace(/\/$/, '')}/realms/${KEYCLOAK_REALM}`;
const authorizationEndpoint = `${realmUrl}/protocol/openid-connect/auth`;
const tokenEndpoint = `${realmUrl}/protocol/openid-connect/token`;
const logoutEndpoint = `${realmUrl}/protocol/openid-connect/logout`;

export function hasAuthCallback(search = window.location.search): boolean {
  const params = new URLSearchParams(search);
  return params.has('code') && params.has('state');
}

export function getStoredUser(): AuthenticatedUser | null {
  const tokens = readTokens();
  if (!tokens) {
    return null;
  }
  return parseUser(tokens.accessToken);
}

export async function handleAuthCallback(): Promise<AuthenticatedUser | null> {
  const callbackParams = new URLSearchParams(window.location.search);
  const callbackError = callbackParams.get('error');
  if (callbackError) {
    clearAuth();
    window.history.replaceState({}, document.title, window.location.pathname || '/');
    throw new Error(callbackParams.get('error_description') ?? callbackError);
  }

  if (!hasAuthCallback()) {
    return getStoredUser();
  }

  const loginState = readLoginState();
  const code = callbackParams.get('code');
  const state = callbackParams.get('state');

  if (!code || !state || !loginState || state !== loginState.state) {
    clearAuth();
    throw new Error('Не удалось подтвердить ответ Keycloak.');
  }

  const tokenResponse = await requestTokens({
    grant_type: 'authorization_code',
    client_id: KEYCLOAK_CLIENT_ID,
    code,
    code_verifier: loginState.codeVerifier,
    redirect_uri: loginState.redirectUri,
  });

  const tokens = persistTokens(tokenResponse);
  sessionStorage.removeItem(LOGIN_STORAGE_KEY);
  window.history.replaceState({}, document.title, loginState.returnTo || '/');
  return parseUser(tokens.accessToken);
}

export async function login(): Promise<void> {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await createCodeChallenge(codeVerifier);
  const state = base64UrlEncode(getRandomBytes(16));
  const redirectUri = `${window.location.origin}${window.location.pathname}`;
  const returnTo = `${window.location.pathname}${window.location.search}${window.location.hash}`;

  const loginState: LoginState = {
    state,
    codeVerifier,
    redirectUri,
    returnTo,
  };
  sessionStorage.setItem(LOGIN_STORAGE_KEY, JSON.stringify(loginState));

  const authorizationUrl = new URL(authorizationEndpoint);
  authorizationUrl.searchParams.set('client_id', KEYCLOAK_CLIENT_ID);
  authorizationUrl.searchParams.set('redirect_uri', redirectUri);
  authorizationUrl.searchParams.set('response_type', 'code');
  authorizationUrl.searchParams.set('scope', 'openid profile email');
  authorizationUrl.searchParams.set('state', state);
  authorizationUrl.searchParams.set('code_challenge', codeChallenge);
  authorizationUrl.searchParams.set('code_challenge_method', 'S256');

  window.location.assign(authorizationUrl.toString());
}

export async function getAccessToken(): Promise<string | null> {
  const tokens = readTokens();
  if (!tokens) {
    return null;
  }

  if (tokens.expiresAt - Date.now() > TOKEN_REFRESH_THRESHOLD_MS) {
    return tokens.accessToken;
  }

  if (!tokens.refreshToken || (tokens.refreshExpiresAt && tokens.refreshExpiresAt <= Date.now())) {
    clearAuth();
    return null;
  }

  try {
    const tokenResponse = await requestTokens({
      grant_type: 'refresh_token',
      client_id: KEYCLOAK_CLIENT_ID,
      refresh_token: tokens.refreshToken,
    });
    return persistTokens(tokenResponse).accessToken;
  } catch {
    clearAuth();
    return null;
  }
}

export function logout(): void {
  const tokens = readTokens();
  clearAuth();

  const logoutUrl = new URL(logoutEndpoint);
  logoutUrl.searchParams.set('client_id', KEYCLOAK_CLIENT_ID);
  logoutUrl.searchParams.set('post_logout_redirect_uri', window.location.origin);
  if (tokens?.idToken) {
    logoutUrl.searchParams.set('id_token_hint', tokens.idToken);
  }

  window.location.assign(logoutUrl.toString());
}

export function clearAuth(): void {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  sessionStorage.removeItem(LOGIN_STORAGE_KEY);
}

function readLoginState(): LoginState | null {
  const rawValue = sessionStorage.getItem(LOGIN_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }
  try {
    return JSON.parse(rawValue) as LoginState;
  } catch {
    return null;
  }
}

function readTokens(): StoredTokens | null {
  const rawValue = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }
  try {
    return JSON.parse(rawValue) as StoredTokens;
  } catch {
    return null;
  }
}

function persistTokens(tokenResponse: TokenResponse): StoredTokens {
  const now = Date.now();
  const tokens: StoredTokens = {
    accessToken: tokenResponse.access_token,
    refreshToken: tokenResponse.refresh_token,
    idToken: tokenResponse.id_token,
    expiresAt: now + tokenResponse.expires_in * 1000,
    refreshExpiresAt: tokenResponse.refresh_expires_in
      ? now + tokenResponse.refresh_expires_in * 1000
      : undefined,
  };
  sessionStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
  return tokens;
}

async function requestTokens(parameters: Record<string, string>): Promise<TokenResponse> {
  const response = await fetch(tokenEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(parameters),
  });

  if (!response.ok) {
    throw new Error(`Keycloak token request failed: ${response.status}`);
  }

  return response.json();
}

function parseUser(accessToken: string): AuthenticatedUser | null {
  const payload = parseJwtPayload(accessToken);
  if (!payload?.sub) {
    return null;
  }

  return {
    subject: payload.sub,
    username: payload.preferred_username,
    email: payload.email,
    name: payload.name,
    roles: extractRoles(payload),
  };
}

function parseJwtPayload(accessToken: string): JwtPayload | null {
  const [, encodedPayload] = accessToken.split('.');
  if (!encodedPayload) {
    return null;
  }

  try {
    const normalizedPayload = addBase64Padding(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'));
    const jsonPayload = decodeURIComponent(
      atob(normalizedPayload)
        .split('')
        .map((character) => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join(''),
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

function addBase64Padding(value: string): string {
  const missingPadding = value.length % 4;
  return missingPadding === 0 ? value : value.padEnd(value.length + 4 - missingPadding, '=');
}

function extractRoles(payload: JwtPayload): string[] {
  const realmRoles = Array.isArray(payload.realm_access?.roles) ? payload.realm_access.roles : [];
  const clientRoles = Array.isArray(payload.resource_access?.[KEYCLOAK_CLIENT_ID]?.roles)
    ? payload.resource_access[KEYCLOAK_CLIENT_ID].roles
    : [];
  return [...new Set([...realmRoles, ...clientRoles])]
    .filter((role): role is string => typeof role === 'string')
    .map((role) => role.toUpperCase());
}

function generateCodeVerifier(): string {
  return base64UrlEncode(getRandomBytes(32));
}

async function createCodeChallenge(codeVerifier: string): Promise<string> {
  const data = new TextEncoder().encode(codeVerifier);
  const digest = await sha256(data);
  return base64UrlEncode(digest);
}

async function sha256(data: Uint8Array): Promise<Uint8Array> {
  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', data);
    return new Uint8Array(digest);
  }

  return sha256Fallback(data);
}

function sha256Fallback(data: Uint8Array): Uint8Array {
  const bitLength = data.length * 8;
  const paddedLength = Math.ceil((data.length + 9) / 64) * 64;
  const paddedData = new Uint8Array(paddedLength);
  paddedData.set(data);
  paddedData[data.length] = 0x80;

  const paddedView = new DataView(paddedData.buffer);
  paddedView.setUint32(paddedLength - 8, Math.floor(bitLength / 0x1_0000_0000));
  paddedView.setUint32(paddedLength - 4, bitLength >>> 0);

  const hash: number[] = [...SHA256_INITIAL_HASH];
  const words = new Uint32Array(64);

  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let index = 0; index < 16; index += 1) {
      words[index] = paddedView.getUint32(offset + index * 4);
    }

    for (let index = 16; index < 64; index += 1) {
      const s0 = rotateRight(words[index - 15], 7) ^ rotateRight(words[index - 15], 18) ^ (words[index - 15] >>> 3);
      const s1 = rotateRight(words[index - 2], 17) ^ rotateRight(words[index - 2], 19) ^ (words[index - 2] >>> 10);
      words[index] = (words[index - 16] + s0 + words[index - 7] + s1) >>> 0;
    }

    let [a, b, c, d, e, f, g, h] = hash;

    for (let index = 0; index < 64; index += 1) {
      const s1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25);
      const choice = (e & f) ^ (~e & g);
      const temp1 = (h + s1 + choice + SHA256_K[index] + words[index]) >>> 0;
      const s0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
      const majority = (a & b) ^ (a & c) ^ (b & c);
      const temp2 = (s0 + majority) >>> 0;

      h = g;
      g = f;
      f = e;
      e = (d + temp1) >>> 0;
      d = c;
      c = b;
      b = a;
      a = (temp1 + temp2) >>> 0;
    }

    hash[0] = (hash[0] + a) >>> 0;
    hash[1] = (hash[1] + b) >>> 0;
    hash[2] = (hash[2] + c) >>> 0;
    hash[3] = (hash[3] + d) >>> 0;
    hash[4] = (hash[4] + e) >>> 0;
    hash[5] = (hash[5] + f) >>> 0;
    hash[6] = (hash[6] + g) >>> 0;
    hash[7] = (hash[7] + h) >>> 0;
  }

  const digest = new Uint8Array(32);
  const digestView = new DataView(digest.buffer);
  hash.forEach((value, index) => digestView.setUint32(index * 4, value));
  return digest;
}

function rotateRight(value: number, bits: number): number {
  return (value >>> bits) | (value << (32 - bits));
}

function getRandomBytes(length: number): Uint8Array {
  if (!globalThis.crypto?.getRandomValues) {
    throw new Error('Браузер не поддерживает безопасную генерацию случайных значений для входа.');
  }

  const values = new Uint8Array(length);
  globalThis.crypto.getRandomValues(values);
  return values;
}

function base64UrlEncode(values: Uint8Array): string {
  let binaryValue = '';
  values.forEach((value) => {
    binaryValue += String.fromCharCode(value);
  });
  return btoa(binaryValue)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
