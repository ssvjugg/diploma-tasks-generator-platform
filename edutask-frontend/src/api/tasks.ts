import type { PageResponse } from '../types/page';
import type { TaskCreateRequest, TaskResponse, TaskSummary } from '../types/task';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

type GetTasksParams = {
  page?: number;
  size?: number;
};

export async function getTasks(params: GetTasksParams = {}): Promise<PageResponse<TaskSummary>> {
  const searchParams = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });

  const response = await fetch(`${API_BASE_URL}/api/v1/tasks?${searchParams.toString()}`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить задачи: ${response.status}`);
  }

  return response.json();
}

export async function getTask(id: string): Promise<TaskResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/tasks/${id}`, {
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить задачу: ${response.status}`);
  }

  return response.json();
}

export async function createTask(payload: TaskCreateRequest): Promise<unknown> {
  const response = await fetch(`${API_BASE_URL}/api/v1/tasks`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Не удалось создать задачу: ${response.status}`);
  }

  return response.json();
}
