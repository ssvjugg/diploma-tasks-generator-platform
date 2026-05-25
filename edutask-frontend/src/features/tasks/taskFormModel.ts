import type { GeneratedTaskDraft, GeneratedTestCaseDraft } from '../../types/generation';
import type { TaskCreateRequest, TaskDifficulty, TaskResponse, TaskUpdateRequest } from '../../types/task';
import type { TestCaseCreateRequest, TestCaseResponse } from '../../types/testCase';
import type { TopicSummary } from '../../types/topic';

export const DEFAULT_TASKS_PAGE_SIZE = 20;
export const TASK_PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

export type TaskFormState = {
  title: string;
  statement: string;
  inputFormat: string;
  outputFormat: string;
  difficulty: TaskDifficulty;
  topics: TopicSummary[];
  testCases: TestCaseFormState[];
};

export const emptyTaskForm: TaskFormState = {
  title: '',
  statement: '',
  inputFormat: '',
  outputFormat: '',
  difficulty: 'EASY',
  topics: [],
  testCases: [],
};

export type TestCaseFormState = {
  inputData: string;
  expectedOutput: string;
  hidden: boolean;
  points: string;
};

export const emptyTestCaseForm: TestCaseFormState = {
  inputData: '',
  expectedOutput: '',
  hidden: false,
  points: '0',
};

export type TaskFormMode = 'create' | 'edit';
export type TestCaseFormMode = 'create' | 'edit';

export const taskToFormState = (task: TaskResponse): TaskFormState => ({
  title: task.title,
  statement: task.statement,
  inputFormat: task.inputFormat ?? '',
  outputFormat: task.outputFormat ?? '',
  difficulty: task.difficulty,
  topics: task.topics,
  testCases: [],
});

export const testCaseToFormState = (testCase: TestCaseResponse): TestCaseFormState => ({
  inputData: testCase.inputData,
  expectedOutput: testCase.expectedOutput,
  hidden: testCase.hidden,
  points: String(testCase.points),
});

const generatedTestCaseToFormState = (testCase: GeneratedTestCaseDraft): TestCaseFormState => ({
  inputData: testCase.inputData ?? '',
  expectedOutput: testCase.expectedOutput ?? '',
  hidden: Boolean(testCase.hidden),
  points: String(Math.max(0, Number.isFinite(testCase.points) ? testCase.points : 0)),
});

export const buildCreateTaskPayload = (form: TaskFormState): TaskCreateRequest => ({
  title: form.title.trim(),
  statement: form.statement.trim(),
  inputFormat: form.inputFormat.trim() || undefined,
  outputFormat: form.outputFormat.trim() || undefined,
  difficulty: form.difficulty,
  topicIds: form.topics.map((topic) => topic.id),
  testCases: form.testCases.length > 0 ? form.testCases.map(buildTestCasePayload) : undefined,
});

export const buildUpdateTaskPayload = (form: TaskFormState): TaskUpdateRequest => ({
  title: form.title.trim(),
  statement: form.statement.trim(),
  inputFormat: form.inputFormat.trim() || undefined,
  outputFormat: form.outputFormat.trim() || undefined,
  difficulty: form.difficulty,
  topicIds: form.topics.map((topic) => topic.id),
});

export const buildTestCasePayload = (form: TestCaseFormState): TestCaseCreateRequest => ({
  inputData: form.inputData,
  expectedOutput: form.expectedOutput,
  hidden: form.hidden,
  points: Number(form.points),
});

export const applyGeneratedDraftToForm = (form: TaskFormState, draft: GeneratedTaskDraft): TaskFormState => ({
  ...form,
  title: draft.title ?? form.title,
  statement: draft.statement ?? form.statement,
  inputFormat: draft.inputFormat ?? form.inputFormat,
  outputFormat: draft.outputFormat ?? form.outputFormat,
  difficulty: draft.difficulty ?? form.difficulty,
  testCases: draft.testCases?.map(generatedTestCaseToFormState) ?? form.testCases,
});

export const difficultyLabels: Record<TaskDifficulty, string> = {
  EASY: 'Легкая',
  MEDIUM: 'Средняя',
  HARD: 'Сложная',
};

export const difficultyClassNames: Record<TaskDifficulty, string> = {
  EASY: 'task-card__difficulty task-card__difficulty--easy',
  MEDIUM: 'task-card__difficulty task-card__difficulty--medium',
  HARD: 'task-card__difficulty task-card__difficulty--hard',
};
