export type CodeSubmissionStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILATION_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'FAILED';

export type CodeSubmissionVerdict = Exclude<CodeSubmissionStatus, 'QUEUED' | 'PROCESSING'>;

export type CodeSubmissionCreateRequest = {
  language: string;
  sourceCode: string;
};

export type CodeSubmissionTestResultResponse = {
  id: string;
  testCaseId: string | null;
  index: number;
  hidden: boolean;
  status: CodeSubmissionStatus;
  inputData: string | null;
  expectedOutput: string | null;
  actualOutput: string | null;
  stderr: string | null;
  compileOutput: string | null;
  errorMessage: string | null;
  time: number | null;
  memory: number | null;
  points: number;
  createdAt: string;
};

export type CodeSubmissionResponse = {
  submissionId: string;
  taskId: string;
  userId: string;
  language: string;
  sourceCode: string | null;
  status: CodeSubmissionStatus;
  testResults: CodeSubmissionTestResultResponse[];
  passedCount: number;
  totalCount: number;
  score: number;
  maxScore: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ProgrammingLanguageOption = {
  code: string;
  label: string;
};

export const terminalSubmissionStatuses: ReadonlySet<CodeSubmissionStatus> = new Set([
  'ACCEPTED',
  'WRONG_ANSWER',
  'COMPILATION_ERROR',
  'RUNTIME_ERROR',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'FAILED',
]);

export function isTerminalSubmissionStatus(status: CodeSubmissionStatus): boolean {
  return terminalSubmissionStatuses.has(status);
}
