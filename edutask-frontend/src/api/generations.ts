import type { TaskGenerationCreateRequest, TaskGenerationResponse } from '../types/generation';
import { apiFetch } from './client';
import { readSseStream } from './sse';

export async function createTaskGeneration(
  payload: TaskGenerationCreateRequest,
  signal?: AbortSignal,
): Promise<TaskGenerationResponse> {
  const response = await apiFetch('/api/v1/tasks/generations', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось запустить генерацию: ${response.status}`);
  }

  return response.json();
}

type StreamTaskGenerationOptions = {
  signal?: AbortSignal;
  onMessage: (response: TaskGenerationResponse) => void;
};

export async function streamTaskGeneration(
  requestId: string,
  { signal, onMessage }: StreamTaskGenerationOptions,
): Promise<void> {
  const response = await apiFetch(`/api/v1/tasks/generations/${requestId}/stream`, {
    headers: {
      Accept: 'text/event-stream',
    },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось открыть поток генерации: ${response.status}`);
  }

  await readSseStream<TaskGenerationResponse>(response, { onMessage });
}
