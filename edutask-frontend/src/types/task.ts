export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type TaskSummary = {
  id: string;
  title: string;
  difficulty: TaskDifficulty;
};

export type TaskCreateRequest = {
  title: string;
  statement: string;
  inputFormat?: string;
  outputFormat?: string;
  difficulty: TaskDifficulty;
  authorId: string;
  topicIds: string[];
  languageIds: number[];
};
