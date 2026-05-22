export type TestCaseResponse = {
  id: string;
  taskId: string;
  inputData: string;
  expectedOutput: string;
  hidden: boolean;
  points: number;
  createdAt: string;
  updatedAt: string;
};

export type TestCaseCreateRequest = {
  inputData: string;
  expectedOutput: string;
  hidden?: boolean;
  points?: number;
};

export type TestCaseUpdateRequest = Partial<TestCaseCreateRequest>;
