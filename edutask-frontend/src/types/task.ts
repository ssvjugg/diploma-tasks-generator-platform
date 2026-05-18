export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type TaskSummary = {
  id: string;
  title: string;
  difficulty: TaskDifficulty;
};

export type TaskTopicSummary = {
  id: string;
  name: string;
};

export type TaskResponse = {
  id: string;
  title: string;
  statement: string;
  inputFormat: string | null;
  outputFormat: string | null;
  difficulty: TaskDifficulty;
  authorId: string;
  topics: TaskTopicSummary[];
  createdAt: string;
  updatedAt: string;
};

export type TaskCreateRequest = {
  title: string;
  statement: string;
  inputFormat?: string;
  outputFormat?: string;
  difficulty: TaskDifficulty;
  topicIds: string[];
};

export type TaskUpdateRequest = Partial<TaskCreateRequest>;
