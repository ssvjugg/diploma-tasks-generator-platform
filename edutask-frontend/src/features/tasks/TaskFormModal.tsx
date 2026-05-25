import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { LoaderCircle, Pencil, Plus, Sparkles, Trash2, X } from 'lucide-react';
import { createTaskGeneration, streamTaskGeneration } from '../../api/generations';
import type { GeneratedTaskDraft } from '../../types/generation';
import type { TaskDifficulty } from '../../types/task';
import type { TopicSummary } from '../../types/topic';
import { MarkdownBlock, MarkdownField } from './MarkdownContent';
import { TestCaseFormModal } from './TestCaseFormModal';
import { TopicMultiSelect } from './TopicMultiSelect';
import {
  applyGeneratedDraftToForm,
  difficultyLabels,
  emptyTestCaseForm,
  type TaskFormMode,
  type TaskFormState,
  type TestCaseFormMode,
  type TestCaseFormState,
} from './taskFormModel';
import { useBodyScrollLock } from './useBodyScrollLock';

type TaskFormModalProps = {
  mode: TaskFormMode;
  initialValue: TaskFormState;
  isSubmitting: boolean;
  error: string | null;
  note: string | null;
  onClose: () => void;
  onNoteChange: (note: string | null) => void;
  onSubmit: (form: TaskFormState) => Promise<void>;
};

export function TaskFormModal({
  mode,
  initialValue,
  isSubmitting,
  error,
  note,
  onClose,
  onNoteChange,
  onSubmit,
}: TaskFormModalProps) {
  const [form, setForm] = useState<TaskFormState>(initialValue);
  const [isAiPromptOpen, setIsAiPromptOpen] = useState(false);
  const [aiPrompt, setAiPrompt] = useState('');
  const [isGeneratingDraft, setIsGeneratingDraft] = useState(false);
  const [generationError, setGenerationError] = useState<string | null>(null);
  const [generatedDraft, setGeneratedDraft] = useState<GeneratedTaskDraft | null>(null);
  const [isGeneratedDraftDialogOpen, setIsGeneratedDraftDialogOpen] = useState(false);
  const [isTestCaseFormOpen, setIsTestCaseFormOpen] = useState(false);
  const [testCaseFormMode, setTestCaseFormMode] = useState<TestCaseFormMode>('create');
  const [editingTestCaseIndex, setEditingTestCaseIndex] = useState<number | null>(null);
  const generationAbortControllerRef = useRef<AbortController | null>(null);
  const isCreateMode = mode === 'create';
  const title = isCreateMode ? 'Новая задача' : 'Редактирование задачи';
  const description = isCreateMode ? 'Заполните поля задачи для банка.' : 'Обновите поля задачи и сохраните изменения.';
  const submitLabel = isCreateMode ? 'Создать' : 'Сохранить';
  const pendingLabel = isCreateMode ? 'Создание' : 'Сохранение';

  useBodyScrollLock();

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (isGeneratedDraftDialogOpen) {
          setIsGeneratedDraftDialogOpen(false);
          return;
        }
        if (isTestCaseFormOpen) {
          setIsTestCaseFormOpen(false);
          setEditingTestCaseIndex(null);
          return;
        }
        onClose();
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isGeneratedDraftDialogOpen, isTestCaseFormOpen, onClose]);

  useEffect(() => () => {
    generationAbortControllerRef.current?.abort();
  }, []);

  const updateForm = (field: Exclude<keyof TaskFormState, 'topics' | 'testCases'>, value: string) => {
    setForm((currentForm) => ({ ...currentForm, [field]: value }));
  };

  const updateTopics = (topics: TopicSummary[]) => {
    setForm((currentForm) => ({ ...currentForm, topics }));
  };

  const openCreateTestCaseForm = () => {
    setTestCaseFormMode('create');
    setEditingTestCaseIndex(null);
    setIsTestCaseFormOpen(true);
  };

  const openEditTestCaseForm = (index: number) => {
    setTestCaseFormMode('edit');
    setEditingTestCaseIndex(index);
    setIsTestCaseFormOpen(true);
  };

  const closeTestCaseForm = useCallback(() => {
    setIsTestCaseFormOpen(false);
    setEditingTestCaseIndex(null);
  }, []);

  const handleTestCaseSubmit = async (testCaseForm: TestCaseFormState) => {
    setForm((currentForm) => {
      if (testCaseFormMode === 'edit' && editingTestCaseIndex !== null) {
        return {
          ...currentForm,
          testCases: currentForm.testCases.map((testCase, index) => (
            index === editingTestCaseIndex ? testCaseForm : testCase
          )),
        };
      }

      return {
        ...currentForm,
        testCases: [...currentForm.testCases, testCaseForm],
      };
    });
    closeTestCaseForm();
  };

  const removeTestCase = (indexToRemove: number) => {
    setForm((currentForm) => ({
      ...currentForm,
      testCases: currentForm.testCases.filter((_, index) => index !== indexToRemove),
    }));
  };

  const handleGenerateDraftClick = () => {
    setIsAiPromptOpen(true);
    setGenerationError(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit(form);
  };

  const handleGenerateDraftSubmit = async () => {
    const normalizedPrompt = aiPrompt.trim();

    if (!normalizedPrompt) {
      setGenerationError('Опишите, какую задачу нужно сгенерировать.');
      return;
    }

    const controller = new AbortController();
    generationAbortControllerRef.current?.abort();
    generationAbortControllerRef.current = controller;
    setIsGeneratingDraft(true);
    setGenerationError(null);
    setGeneratedDraft(null);
    setIsGeneratedDraftDialogOpen(false);
    onNoteChange(null);

    try {
      const generation = await createTaskGeneration(
        {
          prompt: normalizedPrompt,
          difficulty: form.difficulty,
          topicIds: form.topics.map((topic) => topic.id),
        },
        controller.signal,
      );

      if (generation.status === 'COMPLETED' && generation.result) {
        setGeneratedDraft(generation.result);
        onNoteChange('AI подготовил черновик. Проверьте его перед применением к форме.');
        return;
      }

      if (generation.status === 'FAILED') {
        throw new Error(generation.errorMessage ?? 'Генерация завершилась ошибкой');
      }

      await streamTaskGeneration(generation.requestId, {
        signal: controller.signal,
        onMessage: (generationUpdate) => {
          if (generationUpdate.status === 'COMPLETED' && generationUpdate.result) {
            setGeneratedDraft(generationUpdate.result);
            onNoteChange('AI подготовил черновик. Проверьте его перед применением к форме.');
          }

          if (generationUpdate.status === 'FAILED') {
            setGenerationError(generationUpdate.errorMessage ?? 'Генерация завершилась ошибкой');
          }
        },
      });
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return;
      }
      setGenerationError(requestError instanceof Error ? requestError.message : 'Не удалось сгенерировать задачу');
    } finally {
      if (generationAbortControllerRef.current === controller) {
        generationAbortControllerRef.current = null;
      }
      if (!controller.signal.aborted) {
        setIsGeneratingDraft(false);
      }
    }
  };

  const applyGeneratedDraft = () => {
    if (!generatedDraft) {
      return;
    }

    const generatedTestCasesCount = generatedDraft.testCases?.length ?? 0;

    setForm((currentForm) => applyGeneratedDraftToForm(currentForm, generatedDraft));
    setIsAiPromptOpen(false);
    setGeneratedDraft(null);
    setGenerationError(null);
    setIsGeneratedDraftDialogOpen(false);
    onNoteChange(
      generatedTestCasesCount > 0
        ? `Черновик применен к форме. Тест-кейсов: ${generatedTestCasesCount}. Проверьте текст и сохраните задачу.`
        : 'Черновик применен к форме. Проверьте текст и сохраните задачу.',
    );
  };

  return (
    <>
      <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}>
        <section className="task-modal" role="dialog" aria-modal="true" aria-labelledby="task-form-title">
          <header className="task-modal__header">
            <div>
              <h2 id="task-form-title">{title}</h2>
              <p>{description}</p>
            </div>

            <div className="task-modal__actions">
              {isCreateMode && (
                <button
                  className="ai-action"
                  type="button"
                  onClick={handleGenerateDraftClick}
                  disabled={isGeneratingDraft}
                  aria-label="Предложить заполнение через AI"
                  title="Предложить заполнение через AI"
                >
                  {isGeneratingDraft ? (
                    <LoaderCircle className="state-view__loader" size={16} aria-hidden="true" />
                  ) : (
                    <Sparkles size={16} aria-hidden="true" />
                  )}
                  <span>AI</span>
                </button>
              )}
              <button className="modal-close" type="button" onClick={onClose} aria-label="Закрыть форму">
                <X size={18} aria-hidden="true" />
              </button>
            </div>
          </header>

          <form className="task-form" onSubmit={handleSubmit}>
            {isAiPromptOpen && (
              <section className="generation-panel" aria-label="AI генерация задачи">
                <div className="generation-panel__form">
                  <label className="form-field">
                    <span>Промпт для AI</span>
                    <textarea
                      value={aiPrompt}
                      onChange={(event) => setAiPrompt(event.target.value)}
                      placeholder="Например: сгенерируй задачу на циклы для 7 класса с примерами ввода и вывода"
                      rows={4}
                      maxLength={4000}
                      disabled={isGeneratingDraft}
                    />
                  </label>

                  <footer className="generation-panel__actions">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => setIsAiPromptOpen(false)}
                      disabled={isGeneratingDraft}
                    >
                      Скрыть
                    </button>
                    <button
                      className="text-button text-button--icon"
                      type="button"
                      onClick={handleGenerateDraftSubmit}
                      disabled={isGeneratingDraft}
                    >
                      {isGeneratingDraft && <LoaderCircle className="state-view__loader" size={16} aria-hidden="true" />}
                      <span>{isGeneratingDraft ? 'Генерация' : 'Отправить'}</span>
                    </button>
                  </footer>
                </div>

                {generationError && <p className="form-error">{generationError}</p>}

                {generatedDraft && (
                  <div className="generation-draft">
                    <div>
                      <strong>{generatedDraft.title}</strong>
                      <span>{difficultyLabels[generatedDraft.difficulty]}</span>
                    </div>
                    <p>{generatedDraft.statement}</p>
                    {generatedDraft.topics?.length > 0 && (
                      <small>AI предложил темы: {generatedDraft.topics.map((topic) => topic.name).join(', ')}</small>
                    )}
                    <footer className="generation-draft__actions">
                      <button
                        className="secondary-button"
                        type="button"
                        onClick={() => setIsGeneratedDraftDialogOpen(true)}
                      >
                        Развернуть
                      </button>
                      <button className="text-button" type="button" onClick={applyGeneratedDraft}>
                        Применить к форме
                      </button>
                    </footer>
                  </div>
                )}
              </section>
            )}

            <label className="form-field">
              <span>Название</span>
              <input
                value={form.title}
                onChange={(event) => updateForm('title', event.target.value)}
                maxLength={255}
                required
              />
            </label>

            <MarkdownField
              label="Условие"
              value={form.statement}
              rows={5}
              required
              onChange={(value) => updateForm('statement', value)}
            />

            <div className="form-grid">
              <MarkdownField
                label="Формат входных данных"
                value={form.inputFormat}
                rows={3}
                onChange={(value) => updateForm('inputFormat', value)}
              />

              <MarkdownField
                label="Формат выходных данных"
                value={form.outputFormat}
                rows={3}
                onChange={(value) => updateForm('outputFormat', value)}
              />
            </div>

            <div className="form-grid form-grid--task-meta">
              <label className="form-field">
                <span>Сложность</span>
                <select
                  value={form.difficulty}
                  onChange={(event) => updateForm('difficulty', event.target.value as TaskDifficulty)}
                >
                  <option value="EASY">Легкая</option>
                  <option value="MEDIUM">Средняя</option>
                  <option value="HARD">Сложная</option>
                </select>
              </label>

              <TopicMultiSelect selectedTopics={form.topics} onChange={updateTopics} />
            </div>

            {isCreateMode && (
              <section className="task-form-test-cases" aria-label="Тест-кейсы задачи">
                <header className="task-form-test-cases__header">
                  <div>
                    <h3>Тест-кейсы</h3>
                    <span>{form.testCases.length} шт.</span>
                  </div>

                  <button
                    className="secondary-button secondary-button--icon"
                    type="button"
                    onClick={openCreateTestCaseForm}
                  >
                    <Plus size={16} aria-hidden="true" />
                    <span>Добавить</span>
                  </button>
                </header>

                {form.testCases.length === 0 && (
                  <div className="task-form-test-cases__empty">
                    <span>Тест-кейсы пока не добавлены</span>
                  </div>
                )}

                {form.testCases.length > 0 && (
                  <div className="task-form-test-cases__list">
                    {form.testCases.map((testCase, index) => (
                      <article className="task-form-test-case" key={`${testCase.inputData}-${index}`}>
                        <header className="task-form-test-case__header">
                          <div className="task-form-test-case__title">
                            <strong>Тест {index + 1}</strong>
                            <span className={`test-case-visibility ${testCase.hidden ? 'test-case-visibility--hidden' : ''}`}>
                              {testCase.hidden ? 'Скрытый' : 'Открытый'}
                            </span>
                            <span className="test-case-points">баллы: {testCase.points}</span>
                          </div>

                          <div className="task-form-test-case__actions">
                            <button
                              className="icon-button test-case-action"
                              type="button"
                              onClick={() => openEditTestCaseForm(index)}
                              aria-label={`Изменить тест ${index + 1}`}
                              title="Изменить"
                            >
                              <Pencil size={15} aria-hidden="true" />
                            </button>
                            <button
                              className="icon-button test-case-action test-case-action--danger"
                              type="button"
                              onClick={() => removeTestCase(index)}
                              aria-label={`Удалить тест ${index + 1}`}
                              title="Удалить"
                            >
                              <Trash2 size={15} aria-hidden="true" />
                            </button>
                          </div>
                        </header>

                        <div className="task-form-test-case__body">
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
            )}

            {note && <p className="form-note">{note}</p>}
            {error && <p className="form-error">{error}</p>}

            <footer className="task-form__footer">
              <button className="secondary-button" type="button" onClick={onClose}>
                Отмена
              </button>
              <button className="text-button" type="submit" disabled={isSubmitting}>
                {isSubmitting ? pendingLabel : submitLabel}
              </button>
            </footer>
          </form>
        </section>
      </div>

      {generatedDraft && isGeneratedDraftDialogOpen && (
        <GeneratedDraftDialog
          draft={generatedDraft}
          onApply={applyGeneratedDraft}
          onClose={() => setIsGeneratedDraftDialogOpen(false)}
        />
      )}

      {isTestCaseFormOpen && (
        <TestCaseFormModal
          mode={testCaseFormMode}
          initialValue={editingTestCaseIndex === null ? emptyTestCaseForm : form.testCases[editingTestCaseIndex] ?? emptyTestCaseForm}
          isSubmitting={false}
          error={null}
          onClose={closeTestCaseForm}
          onSubmit={handleTestCaseSubmit}
        />
      )}
    </>
  );
}

type GeneratedDraftDialogProps = {
  draft: GeneratedTaskDraft;
  onApply: () => void;
  onClose: () => void;
};

function GeneratedDraftDialog({ draft, onApply, onClose }: GeneratedDraftDialogProps) {
  return (
    <div className="generated-draft-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose();
      }
    }}>
      <section
        className="generated-draft-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="generated-draft-title"
      >
        <header className="generated-draft-dialog__header">
          <div>
            <span className="generated-draft-dialog__eyebrow">AI черновик</span>
            <h3 id="generated-draft-title">{draft.title}</h3>
            <p>{difficultyLabels[draft.difficulty]}</p>
          </div>
          <button className="modal-close" type="button" onClick={onClose} aria-label="Закрыть просмотр">
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <div className="generated-draft-dialog__body">
          <section className="generated-draft-section">
            <h4>Условие</h4>
            <MarkdownBlock source={draft.statement} />
          </section>

          <div className="generated-draft-dialog__grid">
            <section className="generated-draft-section">
              <h4>Формат входных данных</h4>
              {draft.inputFormat ? <MarkdownBlock source={draft.inputFormat} /> : <p>Не указан</p>}
            </section>

            <section className="generated-draft-section">
              <h4>Формат выходных данных</h4>
              {draft.outputFormat ? <MarkdownBlock source={draft.outputFormat} /> : <p>Не указан</p>}
            </section>
          </div>

          {draft.topics?.length > 0 && (
            <section className="generated-draft-section">
              <h4>Предложенные темы</h4>
              <div className="generated-draft-topics">
                {draft.topics.map((topic) => (
                  <span key={topic.name}>{topic.name}</span>
                ))}
              </div>
            </section>
          )}

          {draft.testCases?.length > 0 && (
            <section className="generated-draft-section">
              <h4>Тест-кейсы</h4>
              <div className="generated-test-cases">
                {draft.testCases.map((testCase, index) => (
                  <article className="generated-test-case" key={`${testCase.inputData}-${index}`}>
                    <header>
                      <strong>Тест {index + 1}</strong>
                      <span>{testCase.hidden ? 'Скрытый' : 'Открытый'} · баллы: {testCase.points}</span>
                    </header>
                    <div>
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
            </section>
          )}
        </div>

        <footer className="generated-draft-dialog__footer">
          <button className="secondary-button" type="button" onClick={onClose}>
            Закрыть
          </button>
          <button className="text-button" type="button" onClick={onApply}>
            Применить к форме
          </button>
        </footer>
      </section>
    </div>
  );
}
