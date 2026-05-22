import type { TaskGenerationCreateRequest, TaskGenerationResponse } from '../types/generation';
import { apiFetch } from './client';

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

  const reader = response.body?.getReader();

  if (!reader) {
    throw new Error('Поток генерации недоступен');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();

    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    buffer = consumeSseBuffer(buffer, onMessage);
  }

  buffer += decoder.decode();
  consumeSseBuffer(`${buffer}\n\n`, onMessage);
}

function consumeSseBuffer(
  buffer: string,
  onMessage: (response: TaskGenerationResponse) => void,
): string {
  const normalizedBuffer = buffer.replace(/\r\n/g, '\n');
  const events = normalizedBuffer.split('\n\n');
  const rest = events.pop() ?? '';

  events.forEach((eventText) => {
    const data = eventText
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n');

    if (!data) {
      return;
    }

    onMessage(JSON.parse(data) as TaskGenerationResponse);
  });

  return rest;
}
