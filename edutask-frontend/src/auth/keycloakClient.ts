const KEYCLOAK_BASE_URL = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8085';
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? 'edutask';
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'edutask-frontend';

const TOKEN_STORAGE_KEY = 'edutask.auth.tokens';
const LOGIN_STORAGE_KEY = 'edutask.auth.login';
const TOKEN_REFRESH_THRESHOLD_MS = 30_000;

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
  const state = crypto.randomUUID();
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
  const values = new Uint8Array(32);
  crypto.getRandomValues(values);
  return base64UrlEncode(values);
}

async function createCodeChallenge(codeVerifier: string): Promise<string> {
  const data = new TextEncoder().encode(codeVerifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
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
