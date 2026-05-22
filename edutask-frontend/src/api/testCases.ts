import type { TestCaseCreateRequest, TestCaseResponse, TestCaseUpdateRequest } from '../types/testCase';
import { apiFetch } from './client';

type RequestOptions = {
  signal?: AbortSignal;
};

export async function getTaskTestCases(taskId: string, options: RequestOptions = {}): Promise<TestCaseResponse[]> {
  const response = await apiFetch(`/api/v1/tasks/${taskId}/test-cases`, {
    signal: options.signal,
  });

  if (!response.ok) {
    throw new Error(`Не удалось получить тест-кейсы: ${response.status}`);
  }

  return response.json();
}

export async function createTestCase(taskId: string, payload: TestCaseCreateRequest): Promise<TestCaseResponse> {
  const response = await apiFetch(`/api/v1/tasks/${taskId}/test-cases`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Не удалось создать тест-кейс: ${response.status}`);
  }

  return response.json();
}

export async function updateTestCase(
  taskId: string,
  testCaseId: string,
  payload: TestCaseUpdateRequest,
): Promise<TestCaseResponse> {
  const response = await apiFetch(`/api/v1/tasks/${taskId}/test-cases/${testCaseId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Не удалось обновить тест-кейс: ${response.status}`);
  }

  return response.json();
}

export async function deleteTestCase(taskId: string, testCaseId: string): Promise<void> {
  const response = await apiFetch(`/api/v1/tasks/${taskId}/test-cases/${testCaseId}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error(`Не удалось удалить тест-кейс: ${response.status}`);
  }
}
