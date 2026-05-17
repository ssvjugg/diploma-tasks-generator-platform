import type { PageResponse } from '../types/page';
import type { Topic } from '../types/topic';
import { apiFetch } from './client';

type GetTopicsParams = {
  page?: number;
  size?: number;
  query?: string;
};

export async function getTopics(params: GetTopicsParams = {}): Promise<PageResponse<Topic>> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 12),
    sort: 'name,asc',
  });

  if (params.query?.trim()) {
    searchParams.set('query', params.query.trim());
  }

  const response = await apiFetch(`/api/v1/topics?${searchParams.toString()}`);

  if (!response.ok) {
    throw new Error(`Не удалось получить темы: ${response.status}`);
  }

  return response.json();
}
