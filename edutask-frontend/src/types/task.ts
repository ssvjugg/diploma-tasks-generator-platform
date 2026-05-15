export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type TaskSummary = {
  id: string;
  title: string;
  difficulty: TaskDifficulty;
};

export type TaskTopicSummary = {
  id: string;
  name: string;
  parentId: string | null;
};

export type ProgrammingLanguageSummary = {
  id: number;
  name: string;
  code: string;
  judge0LanguageId: number;
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
  supportedLanguages: ProgrammingLanguageSummary[];
  createdAt: string;
  updatedAt: string;
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
