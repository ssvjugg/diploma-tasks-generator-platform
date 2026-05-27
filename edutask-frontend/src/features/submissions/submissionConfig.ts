import type { CodeSubmissionStatus, ProgrammingLanguageOption } from '../../types/submission';

export const SUBMISSION_POLL_INTERVAL_MS = 1800;

export const supportedSubmissionLanguages: ProgrammingLanguageOption[] = [
  { code: 'python', label: 'Python 3' },
  { code: 'java', label: 'Java 21' },
  { code: 'javascript', label: 'JavaScript Node.js' },
  { code: 'cpp', label: 'C++' },
  { code: 'c', label: 'C' },
];

export const supportedSubmissionLanguageCodes: ReadonlySet<string> = new Set(
  supportedSubmissionLanguages.map((language) => language.code),
);

const submissionStatusLabels: Record<CodeSubmissionStatus, string> = {
  QUEUED: 'В очереди',
  PROCESSING: 'Проверяется',
  ACCEPTED: 'Принято',
  WRONG_ANSWER: 'Неверный ответ',
  COMPILATION_ERROR: 'Ошибка компиляции',
  RUNTIME_ERROR: 'Ошибка выполнения',
  TIME_LIMIT_EXCEEDED: 'Превышено время',
  MEMORY_LIMIT_EXCEEDED: 'Превышена память',
  FAILED: 'Сбой проверки',
};

export const submissionStatusClassNames: Record<CodeSubmissionStatus, string> = {
  QUEUED: 'submission-status submission-status--pending',
  PROCESSING: 'submission-status submission-status--pending',
  ACCEPTED: 'submission-status submission-status--accepted',
  WRONG_ANSWER: 'submission-status submission-status--failed',
  COMPILATION_ERROR: 'submission-status submission-status--failed',
  RUNTIME_ERROR: 'submission-status submission-status--failed',
  TIME_LIMIT_EXCEEDED: 'submission-status submission-status--failed',
  MEMORY_LIMIT_EXCEEDED: 'submission-status submission-status--failed',
  FAILED: 'submission-status submission-status--failed',
};

export function formatSubmissionStatus(status: CodeSubmissionStatus): string {
  return `${status} · ${submissionStatusLabels[status] ?? status}`;
}

export function formatSubmissionLanguageLabel(languageCode: string): string {
  return supportedSubmissionLanguages.find((language) => language.code === languageCode)?.label ?? languageCode;
}
