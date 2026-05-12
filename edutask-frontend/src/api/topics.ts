import type { PageResponse } from '../types/page';
import type { Topic } from '../types/topic';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

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

  const response = await fetch(`${API_BASE_URL}/api/v1/topics?${searchParams.toString()}`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить темы: ${response.status}`);
  }

  return response.json();
}
