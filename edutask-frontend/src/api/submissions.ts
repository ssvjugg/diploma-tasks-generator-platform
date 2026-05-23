import type {
  CodeSubmissionCreateRequest,
  CodeSubmissionResponse,
  CodeSubmissionStatus,
  CodeSubmissionTestResultResponse,
} from '../types/submission';
import { apiFetch } from './client';
import { readSseStream } from './sse';

type RequestOptions = {
  signal?: AbortSignal;
};

type StreamCodeSubmissionOptions = RequestOptions & {
  onMessage: (response: CodeSubmissionResponse) => void;
};

export async function createCodeSubmission(
  taskId: string,
  payload: CodeSubmissionCreateRequest,
  options: RequestOptions = {},
): Promise<CodeSubmissionResponse> {
  const response = await apiFetch(`/api/v1/tasks/${taskId}/submissions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
    signal: options.signal,
  });

  if (!response.ok) {
    throw new Error(await buildSubmissionErrorMessage(response, 'Не удалось отправить решение'));
  }

  return response.json();
}

export async function getCodeSubmission(
  submissionId: string,
  options: RequestOptions = {},
): Promise<CodeSubmissionResponse> {
  const response = await apiFetch(`/api/v1/submissions/${submissionId}`, {
    signal: options.signal,
  });

  if (!response.ok) {
    throw new Error(await buildSubmissionErrorMessage(response, 'Не удалось получить сабмит'));
  }

  return response.json();
}

export async function getCodeSubmissionStatus(
  submissionId: string,
  options: RequestOptions = {},
): Promise<CodeSubmissionStatus> {
  const submission = await getCodeSubmission(submissionId, options);
  return submission.status;
}

export async function getCodeSubmissionResults(
  submissionId: string,
  options: RequestOptions = {},
): Promise<CodeSubmissionTestResultResponse[]> {
  const submission = await getCodeSubmission(submissionId, options);
  return submission.testResults;
}

export async function getCodeSubmissionResult(
  submissionId: string,
  resultId: string,
  options: RequestOptions = {},
): Promise<CodeSubmissionTestResultResponse> {
  const results = await getCodeSubmissionResults(submissionId, options);
  const result = results.find((testResult) => testResult.id === resultId);

  if (!result) {
    throw new Error('Результат теста не найден');
  }

  return result;
}

export async function streamCodeSubmission(
  submissionId: string,
  { signal, onMessage }: StreamCodeSubmissionOptions,
): Promise<void> {
  const response = await apiFetch(`/api/v1/submissions/${submissionId}/stream`, {
    headers: {
      Accept: 'text/event-stream',
    },
    signal,
  });

  if (!response.ok) {
    throw new Error(await buildSubmissionErrorMessage(response, 'Не удалось открыть поток проверки'));
  }

  await readSseStream<CodeSubmissionResponse>(response, { onMessage });
}

async function buildSubmissionErrorMessage(response: Response, fallback: string): Promise<string> {
  const statusDetails: Record<number, string> = {
    400: 'проверьте язык, код и наличие тест-кейсов у задачи',
    401: 'нужно войти заново',
    403: 'нет доступа к этому сабмиту',
    404: 'задача или сабмит не найдены',
    500: 'сервер проверки временно недоступен',
  };
  const details = statusDetails[response.status] ?? `HTTP ${response.status}`;
  const serverMessage = await readErrorMessage(response);

  return serverMessage ? `${fallback}: ${details}. ${serverMessage}` : `${fallback}: ${details}`;
}

async function readErrorMessage(response: Response): Promise<string | null> {
  const contentType = response.headers.get('Content-Type') ?? '';

  try {
    if (contentType.includes('application/json')) {
      const body = await response.json() as { message?: unknown; error?: unknown; detail?: unknown };
      const message = body.message ?? body.error ?? body.detail;
      return typeof message === 'string' ? message : null;
    }

    const text = await response.text();
    return text.trim() || null;
  } catch {
    return null;
  }
}
