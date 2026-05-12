const DEV_AUTHOR_ID_FALLBACK = '11111111-1111-1111-1111-111111111111';

export function resolveCurrentAuthorId(): string | null {
  return import.meta.env.VITE_DEV_AUTHOR_ID || DEV_AUTHOR_ID_FALLBACK;
}
