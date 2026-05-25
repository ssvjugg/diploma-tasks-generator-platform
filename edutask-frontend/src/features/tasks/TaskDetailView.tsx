import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Code2,
  LoaderCircle,
  MoreVertical,
  Pencil,
  Play,
  Plus,
  Send,
  Terminal,
  Trash2,
} from 'lucide-react';
import { createCodeSubmission } from '../../api/submissions';
import { createTestCase, deleteTestCase, getTaskTestCases, updateTestCase } from '../../api/testCases';
import { deleteTask, getTask, updateTask } from '../../api/tasks';
import { useAuth } from '../../auth/AuthContext';
import { PublicTestCasesPanel, SubmissionResultsPanel, type JudgePanelTab } from '../submissions/CodeJudgePanels';
import {
  supportedSubmissionLanguageCodes,
  supportedSubmissionLanguages,
} from '../submissions/submissionConfig';
import { useSubmissionWatch } from '../submissions/useSubmissionWatch';
import { isTerminalSubmissionStatus } from '../../types/submission';
import type { TaskResponse } from '../../types/task';
import type { TestCaseResponse } from '../../types/testCase';
import { MarkdownBlock } from './MarkdownContent';
import { TaskFormModal } from './TaskFormModal';
import { TestCaseFormModal } from './TestCaseFormModal';
import {
  buildTestCasePayload,
  buildUpdateTaskPayload,
  difficultyClassNames,
  difficultyLabels,
  emptyTestCaseForm,
  taskToFormState,
  testCaseToFormState,
  type TaskFormState,
  type TestCaseFormMode,
  type TestCaseFormState,
} from './taskFormModel';

export function TaskDetailView() {
  const auth = useAuth();
  const navigate = useNavigate();
  const { taskId } = useParams<{ taskId: string }>();
  const [task, setTask] = useState<TaskResponse | null>(null);
  const [testCases, setTestCases] = useState<TestCaseResponse[]>([]);
  const [isTaskLoading, setIsTaskLoading] = useState(true);
  const [isTestCasesLoading, setIsTestCasesLoading] = useState(false);
  const [taskError, setTaskError] = useState<string | null>(null);
  const [testCasesError, setTestCasesError] = useState<string | null>(null);
  const [selectedLanguage, setSelectedLanguage] = useState(supportedSubmissionLanguages[0]?.code ?? '');
  const [solutionCode, setSolutionCode] = useState('');
  const [judgePanelTab, setJudgePanelTab] = useState<JudgePanelTab>('testcases');
  const [isSubmittingSolution, setIsSubmittingSolution] = useState(false);
  const [isTaskMenuOpen, setIsTaskMenuOpen] = useState(false);
  const [isEditFormOpen, setIsEditFormOpen] = useState(false);
  const [isUpdatingTask, setIsUpdatingTask] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  const [editNote, setEditNote] = useState<string | null>(null);
  const [isDeletingTask, setIsDeletingTask] = useState(false);
  const [testCaseFormMode, setTestCaseFormMode] = useState<TestCaseFormMode>('create');
  const [editingTestCase, setEditingTestCase] = useState<TestCaseResponse | null>(null);
  const [isTestCaseFormOpen, setIsTestCaseFormOpen] = useState(false);
  const [isSavingTestCase, setIsSavingTestCase] = useState(false);
  const [testCaseFormError, setTestCaseFormError] = useState<string | null>(null);
  const [deletingTestCaseId, setDeletingTestCaseId] = useState<string | null>(null);
  const {
    activeSubmission,
    setActiveSubmission,
    isSubmissionLive,
    submissionError,
    setSubmissionError,
    cancelSubmissionWatch,
    watchSubmission,
    isSubmissionInProgress,
  } = useSubmissionWatch();
  const taskMenuRef = useRef<HTMLDivElement | null>(null);

  const loadTask = useCallback(async (signal?: AbortSignal, shouldApplyResult: () => boolean = () => true) => {
    if (!taskId) {
      if (shouldApplyResult()) {
        setTaskError('Не указан идентификатор задачи');
        setIsTaskLoading(false);
      }
      return;
    }

    if (shouldApplyResult()) {
      setIsTaskLoading(true);
      setTaskError(null);
    }

    try {
      const data = await getTask(taskId, { signal });
      if (shouldApplyResult()) {
        setTask(data);
      }
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return;
      }
      if (shouldApplyResult()) {
        setTaskError(requestError instanceof Error ? requestError.message : 'Не удалось получить задачу');
      }
    } finally {
      if (!signal?.aborted && shouldApplyResult()) {
        setIsTaskLoading(false);
      }
    }
  }, [taskId]);

  const loadTestCases = useCallback(async (signal?: AbortSignal, shouldApplyResult: () => boolean = () => true) => {
    if (!taskId) {
      return;
    }

    if (shouldApplyResult()) {
      setIsTestCasesLoading(true);
      setTestCasesError(null);
    }

    try {
      const data = await getTaskTestCases(taskId, { signal });
      if (shouldApplyResult()) {
        setTestCases(data);
      }
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return;
      }
      if (shouldApplyResult()) {
        setTestCasesError(requestError instanceof Error ? requestError.message : 'Не удалось получить тест-кейсы');
      }
    } finally {
      if (!signal?.aborted && shouldApplyResult()) {
        setIsTestCasesLoading(false);
      }
    }
  }, [taskId]);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;
    void loadTask(controller.signal, () => isActive);

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [loadTask]);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;
    void loadTestCases(controller.signal, () => isActive);

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [loadTestCases]);

  useEffect(() => {
    if (!isTaskMenuOpen) {
      return undefined;
    }

    const handleOutsideMouseDown = (event: MouseEvent) => {
      const target = event.target;

      if (target instanceof Node && !taskMenuRef.current?.contains(target)) {
        setIsTaskMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideMouseDown);

    return () => {
      document.removeEventListener('mousedown', handleOutsideMouseDown);
    };
  }, [isTaskMenuOpen]);

  const canManageTask = auth.hasRole('TEACHER') || auth.hasRole('ADMIN');

  const openEditForm = () => {
    setIsTaskMenuOpen(false);
    setEditError(null);
    setEditNote(null);
    setIsEditFormOpen(true);
  };

  const closeEditForm = useCallback(() => {
    setIsEditFormOpen(false);
    setEditError(null);
    setEditNote(null);
  }, []);

  const openCreateTestCaseForm = () => {
    setTestCaseFormMode('create');
    setEditingTestCase(null);
    setTestCaseFormError(null);
    setIsTestCaseFormOpen(true);
  };

  const openEditTestCaseForm = (testCase: TestCaseResponse) => {
    setTestCaseFormMode('edit');
    setEditingTestCase(testCase);
    setTestCaseFormError(null);
    setIsTestCaseFormOpen(true);
  };

  const closeTestCaseForm = useCallback(() => {
    setIsTestCaseFormOpen(false);
    setEditingTestCase(null);
    setTestCaseFormError(null);
  }, []);

  const handleUpdateTask = async (form: TaskFormState) => {
    if (!task) {
      return;
    }

    setIsUpdatingTask(true);
    setEditError(null);
    setEditNote(null);

    try {
      const updatedTask = await updateTask(task.id, buildUpdateTaskPayload(form));
      setTask(updatedTask);
      closeEditForm();
    } catch (requestError) {
      setEditError(requestError instanceof Error ? requestError.message : 'Не удалось обновить задачу');
    } finally {
      setIsUpdatingTask(false);
    }
  };

  const handleDeleteTask = async () => {
    if (!task || isDeletingTask) {
      return;
    }

    const shouldDelete = window.confirm(`Удалить задачу "${task.title}"? Это действие нельзя отменить.`);

    if (!shouldDelete) {
      setIsTaskMenuOpen(false);
      return;
    }

    setIsDeletingTask(true);
    setTaskError(null);

    try {
      await deleteTask(task.id);
      navigate('/tasks');
    } catch (requestError) {
      setTaskError(requestError instanceof Error ? requestError.message : 'Не удалось удалить задачу');
      setIsTaskMenuOpen(false);
    } finally {
      setIsDeletingTask(false);
    }
  };

  const handleSaveTestCase = async (form: TestCaseFormState) => {
    if (!task) {
      return;
    }

    setIsSavingTestCase(true);
    setTestCaseFormError(null);

    try {
      const payload = buildTestCasePayload(form);

      if (testCaseFormMode === 'edit' && editingTestCase) {
        const updatedTestCase = await updateTestCase(task.id, editingTestCase.id, payload);
        setTestCases((currentTestCases) => currentTestCases.map((testCase) => (
          testCase.id === updatedTestCase.id ? updatedTestCase : testCase
        )));
      } else {
        const createdTestCase = await createTestCase(task.id, payload);
        setTestCases((currentTestCases) => [...currentTestCases, createdTestCase]);
      }

      closeTestCaseForm();
    } catch (requestError) {
      setTestCaseFormError(requestError instanceof Error ? requestError.message : 'Не удалось сохранить тест-кейс');
    } finally {
      setIsSavingTestCase(false);
    }
  };

  const handleDeleteTestCase = async (testCase: TestCaseResponse) => {
    if (!task || deletingTestCaseId) {
      return;
    }

    const shouldDelete = window.confirm('Удалить тест-кейс? Это действие нельзя отменить.');

    if (!shouldDelete) {
      return;
    }

    setDeletingTestCaseId(testCase.id);
    setTestCasesError(null);

    try {
      await deleteTestCase(task.id, testCase.id);
      setTestCases((currentTestCases) => currentTestCases.filter((currentTestCase) => (
        currentTestCase.id !== testCase.id
      )));
    } catch (requestError) {
      setTestCasesError(requestError instanceof Error ? requestError.message : 'Не удалось удалить тест-кейс');
    } finally {
      setDeletingTestCaseId(null);
    }
  };

  const handleSubmitSolution = async () => {
    if (!task || isSubmittingSolution) {
      return;
    }

    if (!selectedLanguage || !supportedSubmissionLanguageCodes.has(selectedLanguage)) {
      setSubmissionError('Выберите поддерживаемый язык программирования.');
      setJudgePanelTab('results');
      return;
    }

    if (!solutionCode.trim()) {
      setSubmissionError('Добавьте исходный код перед отправкой решения.');
      setJudgePanelTab('results');
      return;
    }

    cancelSubmissionWatch();
    setActiveSubmission(null);
    setIsSubmittingSolution(true);
    setSubmissionError(null);
    setJudgePanelTab('results');

    try {
      const submission = await createCodeSubmission(task.id, {
        language: selectedLanguage,
        sourceCode: solutionCode,
      });

      setActiveSubmission(submission);

      if (!isTerminalSubmissionStatus(submission.status)) {
        watchSubmission(submission.submissionId);
      }
    } catch (requestError) {
      setSubmissionError(requestError instanceof Error ? requestError.message : 'Не удалось отправить решение');
    } finally {
      setIsSubmittingSolution(false);
    }
  };

  const canSubmitSolution = !isSubmittingSolution && !isSubmissionInProgress && Boolean(task);

  if (isTaskLoading) {
    return (
      <div className="state-view task-detail-state">
        <LoaderCircle className="state-view__loader" size={28} aria-hidden="true" />
        <span>Загрузка задачи</span>
      </div>
    );
  }

  if (taskError || !task) {
    return (
      <div className="state-view state-view--error task-detail-state">
        <span>{taskError ?? 'Задача не найдена'}</span>
        <button className="text-button" type="button" onClick={() => loadTask()}>
          Повторить
        </button>
      </div>
    );
  }

  return (
    <section className="task-detail" aria-labelledby="page-title">
      <header className="task-detail__topbar">
        <button className="task-detail__back" type="button" onClick={() => navigate('/tasks')}>
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Список задач</span>
        </button>

        <div className="task-detail__actions" aria-label="Действия с задачей">
          {canManageTask && (
            <div className="task-detail__menu" ref={taskMenuRef}>
              <button
                className="secondary-button secondary-button--icon secondary-button--square"
                type="button"
                onClick={() => setIsTaskMenuOpen((isOpen) => !isOpen)}
                aria-expanded={isTaskMenuOpen}
                aria-controls="task-actions-panel"
                aria-label="Управление задачей"
                title="Управление задачей"
              >
                <MoreVertical size={21} strokeWidth={2.4} aria-hidden="true" />
              </button>

              {isTaskMenuOpen && (
                <div className="action-menu" id="task-actions-panel" aria-label="Управление задачей">
                  <button className="action-menu__item" type="button" onClick={openEditForm}>
                    <Pencil size={16} aria-hidden="true" />
                    <span>Изменить</span>
                  </button>
                  <button
                    className="action-menu__item action-menu__item--danger"
                    type="button"
                    onClick={handleDeleteTask}
                    disabled={isDeletingTask}
                  >
                    <Trash2 size={16} aria-hidden="true" />
                    <span>{isDeletingTask ? 'Удаление' : 'Удалить'}</span>
                  </button>
                </div>
              )}
            </div>
          )}
          <button className="secondary-button secondary-button--icon" type="button" disabled>
            <Play size={17} aria-hidden="true" />
            <span>Run</span>
          </button>
          <button
            className="text-button text-button--icon"
            type="button"
            onClick={handleSubmitSolution}
            disabled={!canSubmitSolution}
            title={isSubmissionInProgress ? 'Дождитесь завершения текущей проверки' : 'Отправить решение'}
          >
            {isSubmittingSolution ? (
              <LoaderCircle className="state-view__loader" size={17} aria-hidden="true" />
            ) : (
              <Send size={17} aria-hidden="true" />
            )}
            <span>{isSubmittingSolution ? 'Отправка' : 'Submit'}</span>
          </button>
        </div>
      </header>

      <div className="task-detail__grid">
        <article className="problem-panel">
          <div className="panel-tabs" aria-label="Разделы задачи">
            <button className="panel-tab panel-tab--active" type="button">
              Описание
            </button>
            <button className="panel-tab" type="button" disabled>
              Отправки
            </button>
          </div>

          <div className="problem-content">
            <h1 id="page-title">{task.title}</h1>
            <div className="problem-chips">
              <span className={difficultyClassNames[task.difficulty]}>{difficultyLabels[task.difficulty]}</span>
              {task.topics.map((topic) => (
                <span className="problem-chip" key={topic.id}>
                  {topic.name}
                </span>
              ))}
            </div>

            <section className="problem-section">
              <h2>Условие</h2>
              <MarkdownBlock source={task.statement} />
            </section>

            {task.inputFormat && (
              <section className="problem-section">
                <h2>Входные данные</h2>
                <MarkdownBlock source={task.inputFormat} />
              </section>
            )}

            {task.outputFormat && (
              <section className="problem-section">
                <h2>Выходные данные</h2>
                <MarkdownBlock source={task.outputFormat} />
              </section>
            )}

            <section className="problem-section test-cases-section" aria-labelledby="test-cases-title">
              <header className="test-cases-section__header">
                <div>
                  <h2 id="test-cases-title">Тест-кейсы</h2>
                  <span>{testCases.length} шт.</span>
                </div>

                {canManageTask && (
                  <button
                    className="secondary-button secondary-button--icon"
                    type="button"
                    onClick={openCreateTestCaseForm}
                    title="Добавить тест-кейс"
                  >
                    <Plus size={16} aria-hidden="true" />
                    <span>Добавить</span>
                  </button>
                )}
              </header>

              {isTestCasesLoading && (
                <div className="test-cases-state">
                  <LoaderCircle className="state-view__loader" size={18} aria-hidden="true" />
                  <span>Загрузка тест-кейсов</span>
                </div>
              )}

              {!isTestCasesLoading && testCasesError && (
                <div className="test-cases-state test-cases-state--error">
                  <span>{testCasesError}</span>
                  <button className="text-button" type="button" onClick={() => loadTestCases()}>
                    Повторить
                  </button>
                </div>
              )}

              {!isTestCasesLoading && !testCasesError && testCases.length === 0 && (
                <div className="test-cases-state">
                  <span>Тест-кейсы пока не добавлены</span>
                </div>
              )}

              {!isTestCasesLoading && !testCasesError && testCases.length > 0 && (
                <div className="test-cases-list">
                  {testCases.map((testCase, index) => (
                    <article className="test-case-item" key={testCase.id}>
                      <header className="test-case-item__header">
                        <div className="test-case-item__title">
                          <strong>Тест {index + 1}</strong>
                          <span className={`test-case-visibility ${testCase.hidden ? 'test-case-visibility--hidden' : ''}`}>
                            {testCase.hidden ? 'Скрытый' : 'Открытый'}
                          </span>
                          <span className="test-case-points">баллы: {testCase.points}</span>
                        </div>

                        {canManageTask && (
                          <div className="test-case-item__actions">
                            <button
                              className="icon-button test-case-action"
                              type="button"
                              onClick={() => openEditTestCaseForm(testCase)}
                              aria-label={`Изменить тест ${index + 1}`}
                              title="Изменить"
                            >
                              <Pencil size={15} aria-hidden="true" />
                            </button>
                            <button
                              className="icon-button test-case-action test-case-action--danger"
                              type="button"
                              onClick={() => handleDeleteTestCase(testCase)}
                              disabled={deletingTestCaseId === testCase.id}
                              aria-label={`Удалить тест ${index + 1}`}
                              title="Удалить"
                            >
                              {deletingTestCaseId === testCase.id ? (
                                <LoaderCircle className="state-view__loader" size={15} aria-hidden="true" />
                              ) : (
                                <Trash2 size={15} aria-hidden="true" />
                              )}
                            </button>
                          </div>
                        )}
                      </header>

                      <div className="test-case-item__body">
                        <label>
                          <span>Ввод</span>
                          <pre>{testCase.inputData || 'Пустой ввод'}</pre>
                        </label>
                        <label>
                          <span>Ожидаемый вывод</span>
                          <pre>{testCase.expectedOutput || 'Пустой вывод'}</pre>
                        </label>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </article>

        <section className="solution-panel" aria-label="Решение задачи">
          <header className="solution-panel__header">
            <div className="solution-panel__title">
              <Code2 size={19} aria-hidden="true" />
              <span>Code</span>
            </div>
            <select
              aria-label="Язык решения"
              value={selectedLanguage}
              onChange={(event) => {
                setSelectedLanguage(event.target.value);
                setSubmissionError(null);
              }}
              disabled={isSubmittingSolution || isSubmissionInProgress}
            >
              {supportedSubmissionLanguages.map((language) => (
                <option value={language.code} key={language.code}>
                  {language.label}
                </option>
              ))}
            </select>
          </header>

          <textarea
            className="code-editor"
            value={solutionCode}
            onChange={(event) => setSolutionCode(event.target.value)}
            spellCheck={false}
            placeholder="Напишите решение здесь и отправьте его на проверку."
          />

          <footer className="judge-panel">
            <div className="judge-panel__tabs">
              <button
                className={`judge-tab ${judgePanelTab === 'testcases' ? 'judge-tab--active' : ''}`}
                type="button"
                onClick={() => setJudgePanelTab('testcases')}
              >
                <Terminal size={16} aria-hidden="true" />
                Testcase
              </button>
              <button
                className={`judge-tab ${judgePanelTab === 'results' ? 'judge-tab--active' : ''}`}
                type="button"
                onClick={() => setJudgePanelTab('results')}
              >
                Test Result
              </button>
            </div>
            <div className="judge-panel__body">
              {judgePanelTab === 'testcases' ? (
                <PublicTestCasesPanel
                  testCases={testCases}
                  isLoading={isTestCasesLoading}
                  error={testCasesError}
                  onRetry={() => loadTestCases()}
                />
              ) : (
                <SubmissionResultsPanel
                  submission={activeSubmission}
                  isSubmitting={isSubmittingSolution}
                  isLive={isSubmissionLive}
                  error={submissionError}
                />
              )}
            </div>
          </footer>
        </section>
      </div>

      {isEditFormOpen && (
        <TaskFormModal
          mode="edit"
          initialValue={taskToFormState(task)}
          isSubmitting={isUpdatingTask}
          error={editError}
          note={editNote}
          onClose={closeEditForm}
          onNoteChange={setEditNote}
          onSubmit={handleUpdateTask}
        />
      )}

      {isTestCaseFormOpen && (
        <TestCaseFormModal
          mode={testCaseFormMode}
          initialValue={editingTestCase ? testCaseToFormState(editingTestCase) : emptyTestCaseForm}
          isSubmitting={isSavingTestCase}
          error={testCaseFormError}
          onClose={closeTestCaseForm}
          onSubmit={handleSaveTestCase}
        />
      )}
    </section>
  );
}
