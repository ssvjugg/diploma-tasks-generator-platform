import type { PageResponse } from '../types/page';
import type { TaskCreateRequest, TaskResponse, TaskSummary, TaskUpdateRequest } from '../types/task';
import { apiFetch } from './client';

type GetTasksParams = {
  page?: number;
  size?: number;
  query?: string;
  difficulty?: string;
  authorId?: string;
  topicId?: string;
};

export async function getTasks(params: GetTasksParams = {}): Promise<PageResponse<TaskSummary>> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });

  if (params.query?.trim()) {
    searchParams.set('query', params.query.trim());
  }

  if (params.difficulty) {
    searchParams.set('difficulty', params.difficulty);
  }

  if (params.authorId) {
    searchParams.set('authorId', params.authorId);
  }

  if (params.topicId) {
    searchParams.set('topicId', params.topicId);
  }

  const response = await apiFetch(`/api/v1/tasks?${searchParams.toString()}`);

  if (!response.ok) {
    throw new Error(`Не удалось получить задачи: ${response.status}`);
  }

  return response.json();
}

type RequestOptions = {
  signal?: AbortSignal;
};

export async function getTask(id: string, options: RequestOptions = {}): Promise<TaskResponse> {
  const response = await apiFetch(`/api/v1/tasks/${id}`, {
    signal: options.signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить задачу: ${response.status}`);
  }

  return response.json();
}

export async function createTask(payload: TaskCreateRequest): Promise<unknown> {
  const response = await apiFetch('/api/v1/tasks', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Не удалось создать задачу: ${response.status}`);
  }

  return response.json();
}

export async function updateTask(id: string, payload: TaskUpdateRequest): Promise<TaskResponse> {
  const response = await apiFetch(`/api/v1/tasks/${id}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Не удалось обновить задачу: ${response.status}`);
  }

  return response.json();
}

export async function deleteTask(id: string): Promise<void> {
  const response = await apiFetch(`/api/v1/tasks/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error(`Не удалось удалить задачу: ${response.status}`);
  }
}
