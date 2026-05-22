import type { TaskDifficulty } from './task';

export type TaskGenerationStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export type GeneratedTopicDraft = {
  name: string;
};

export type GeneratedTestCaseDraft = {
  inputData: string;
  expectedOutput: string;
  hidden: boolean;
  points: number;
};

export type GeneratedTaskDraft = {
  title: string;
  statement: string;
  inputFormat: string | null;
  outputFormat: string | null;
  difficulty: TaskDifficulty;
  topics: GeneratedTopicDraft[];
  testCases: GeneratedTestCaseDraft[];
};

export type TaskGenerationCreateRequest = {
  prompt: string;
  topicIds: string[];
  difficulty?: TaskDifficulty;
};

export type TaskGenerationResponse = {
  requestId: string;
  status: TaskGenerationStatus;
  provider: string | null;
  model: string | null;
  result: GeneratedTaskDraft | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};
