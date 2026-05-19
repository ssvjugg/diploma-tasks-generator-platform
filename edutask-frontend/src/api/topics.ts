import type { PageResponse } from '../types/page';
import type { Topic, TopicSummary } from '../types/topic';
import { apiFetch } from './client';

type GetTopicsParams = {
  page?: number;
  size?: number;
  query?: string;
  parentId?: string;
  rootOnly?: boolean;
  signal?: AbortSignal;
};

type SearchTopicsParams = {
  query?: string;
  limit?: number;
  signal?: AbortSignal;
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

  if (params.parentId) {
    searchParams.set('parentId', params.parentId);
  }

  if (params.rootOnly) {
    searchParams.set('rootOnly', 'true');
  }

  const response = await apiFetch(`/api/v1/topics?${searchParams.toString()}`, {
    signal: params.signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить темы: ${response.status}`);
  }

  return response.json();
}

export async function getTopic(id: string, options: { signal?: AbortSignal } = {}): Promise<Topic> {
  const response = await apiFetch(`/api/v1/topics/${id}`, {
    signal: options.signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить тему: ${response.status}`);
  }

  return response.json();
}

export async function searchTopics(params: SearchTopicsParams = {}): Promise<TopicSummary[]> {
  const searchParams = new URLSearchParams({
    limit: String(params.limit ?? 12),
  });

  if (params.query?.trim()) {
    searchParams.set('query', params.query.trim());
  }

  const response = await apiFetch(`/api/v1/topics/search?${searchParams.toString()}`, {
    signal: params.signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось найти темы: ${response.status}`);
  }

  return response.json();
}
