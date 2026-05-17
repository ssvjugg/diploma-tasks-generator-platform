import { apiFetch } from './client';
import type { UserProfile } from '../types/user';

export async function registerCurrentUser(): Promise<UserProfile> {
  const response = await apiFetch('/api/v1/users/me/register', {
    method: 'POST',
  });

  if (!response.ok) {
    throw new Error(`Не удалось зарегистрировать пользователя: ${response.status}`);
  }

  return response.json();
}

export async function getCurrentUser(): Promise<UserProfile> {
  const response = await apiFetch('/api/v1/users/me');

  if (!response.ok) {
    throw new Error(`Не удалось получить пользователя: ${response.status}`);
  }

  return response.json();
}
