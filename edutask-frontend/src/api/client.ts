const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

type AuthConfiguration = {
  getAccessToken: () => Promise<string | null>;
  onUnauthorized: () => void;
};

let authConfiguration: AuthConfiguration | null = null;

export function configureApiAuth(configuration: AuthConfiguration): void {
  authConfiguration = configuration;
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Accept', headers.get('Accept') ?? 'application/json');

  const accessToken = await authConfiguration?.getAccessToken();
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401) {
    authConfiguration?.onUnauthorized();
  }

  return response;
}
