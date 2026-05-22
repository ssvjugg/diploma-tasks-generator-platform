import { type FocusEvent, type FormEvent, lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  BotMessageSquare,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Code2,
  Filter,
  LogOut,
  LoaderCircle,
  MessageSquarePlus,
  MoreVertical,
  Pencil,
  Plus,
  Play,
  Search,
  Send,
  Sparkles,
  Tags,
  Terminal,
  Trash2,
  UserRound,
  X,
} from 'lucide-react';
import * as markdownCommands from '@uiw/react-md-editor/commands';
import '@uiw/react-md-editor/markdown-editor.css';
import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { createTaskGeneration, streamTaskGeneration } from './api/generations';
import { createTask, deleteTask, getTask, getTasks, updateTask } from './api/tasks';
import { getTopic, getTopics, searchTopics } from './api/topics';
import { useAuth } from './auth/AuthContext';
import type { GeneratedTaskDraft } from './types/generation';
import type { PageResponse } from './types/page';
import type { TaskCreateRequest, TaskDifficulty, TaskResponse, TaskSummary, TaskUpdateRequest } from './types/task';
import type { Topic, TopicSummary } from './types/topic';

const DEFAULT_TASKS_PAGE_SIZE = 20;
const TASK_PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
const TOPICS_PAGE_SIZE = 12;
const TOPIC_SEARCH_LIMIT = 12;
const TOPIC_SEARCH_DEBOUNCE_MS = 250;
const MarkdownEditor = lazy(() => import('@uiw/react-md-editor'));
const MarkdownPreview = lazy(async () => {
  const module = await import('@uiw/react-md-editor');
  return { default: module.default.Markdown };
});

const markdownEditorCommands = [
  markdownCommands.bold,
  markdownCommands.italic,
  markdownCommands.strikethrough,
  markdownCommands.hr,
  markdownCommands.title,
  markdownCommands.divider,
  markdownCommands.link,
  markdownCommands.quote,
  markdownCommands.code,
  markdownCommands.codeBlock,
  markdownCommands.table,
  markdownCommands.divider,
  markdownCommands.unorderedListCommand,
  markdownCommands.orderedListCommand,
];

const markdownEditorExtraCommands = [
  markdownCommands.codeEdit,
  markdownCommands.codePreview,
  markdownCommands.fullscreen,
];

type TaskFormState = {
  title: string;
  statement: string;
  inputFormat: string;
  outputFormat: string;
  difficulty: TaskDifficulty;
  topics: TopicSummary[];
};

const emptyTaskForm: TaskFormState = {
  title: '',
  statement: '',
  inputFormat: '',
  outputFormat: '',
  difficulty: 'EASY',
  topics: [],
};

const taskToFormState = (task: TaskResponse): TaskFormState => ({
  title: task.title,
  statement: task.statement,
  inputFormat: task.inputFormat ?? '',
  outputFormat: task.outputFormat ?? '',
  difficulty: task.difficulty,
  topics: task.topics,
});

const buildCreateTaskPayload = (form: TaskFormState): TaskCreateRequest => ({
  title: form.title.trim(),
  statement: form.statement.trim(),
  inputFormat: form.inputFormat.trim() || undefined,
  outputFormat: form.outputFormat.trim() || undefined,
  difficulty: form.difficulty,
  topicIds: form.topics.map((topic) => topic.id),
});

const buildUpdateTaskPayload = (form: TaskFormState): TaskUpdateRequest => ({
  title: form.title.trim(),
  statement: form.statement.trim(),
  inputFormat: form.inputFormat.trim() || undefined,
  outputFormat: form.outputFormat.trim() || undefined,
  difficulty: form.difficulty,
  topicIds: form.topics.map((topic) => topic.id),
});

const applyGeneratedDraftToForm = (form: TaskFormState, draft: GeneratedTaskDraft): TaskFormState => ({
  ...form,
  title: draft.title ?? form.title,
  statement: draft.statement ?? form.statement,
  inputFormat: draft.inputFormat ?? form.inputFormat,
  outputFormat: draft.outputFormat ?? form.outputFormat,
  difficulty: draft.difficulty ?? form.difficulty,
});

const difficultyLabels: Record<TaskDifficulty, string> = {
  EASY: 'Легкая',
  MEDIUM: 'Средняя',
  HARD: 'Сложная',
};

const difficultyClassNames: Record<TaskDifficulty, string> = {
  EASY: 'task-card__difficulty task-card__difficulty--easy',
  MEDIUM: 'task-card__difficulty task-card__difficulty--medium',
  HARD: 'task-card__difficulty task-card__difficulty--hard',
};

type MarkdownFieldProps = {
  label: string;
  value: string;
  rows?: number;
  required?: boolean;
  onChange: (value: string) => void;
};

function MarkdownField({ label, value, rows = 4, required, onChange }: MarkdownFieldProps) {
  return (
    <label className="form-field markdown-field">
      <span>{label}</span>
      <div className="markdown-field__editor" data-color-mode="light">
        <Suspense fallback={<div className="markdown-field__fallback">Загрузка редактора</div>}>
          <MarkdownEditor
            value={value}
            onChange={(nextValue) => onChange(nextValue ?? '')}
            height={rows * 48}
            preview="edit"
            commands={markdownEditorCommands}
            extraCommands={markdownEditorExtraCommands}
            textareaProps={{
              required,
              'aria-label': label,
            }}
          />
        </Suspense>
      </div>
    </label>
  );
}

type MarkdownBlockProps = {
  source: string;
};

function MarkdownBlock({ source }: MarkdownBlockProps) {
  return (
    <div className="problem-markdown" data-color-mode="light">
      <Suspense fallback={<div className="problem-markdown__fallback">Загрузка текста</div>}>
        <MarkdownPreview source={source} />
      </Suspense>
    </div>
  );
}

type TopicMultiSelectProps = {
  selectedTopics: TopicSummary[];
  onChange: (topics: TopicSummary[]) => void;
};

const normalizeTopicQuery = (query: string) => query.trim().toLowerCase();

function TopicMultiSelect({ selectedTopics, onChange }: TopicMultiSelectProps) {
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<TopicSummary[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const cacheRef = useRef(new Map<string, TopicSummary[]>());
  const debounceRef = useRef<number | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const cancelPendingTopicSearch = useCallback(() => {
    if (debounceRef.current !== null) {
      window.clearTimeout(debounceRef.current);
      debounceRef.current = null;
    }

    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
  }, []);

  const selectedTopicIds = useMemo(
    () => new Set(selectedTopics.map((topic) => topic.id)),
    [selectedTopics],
  );

  const visibleOptions = useMemo(
    () => options.filter((topic) => !selectedTopicIds.has(topic.id)),
    [options, selectedTopicIds],
  );

  useEffect(() => () => {
    cancelPendingTopicSearch();
  }, [cancelPendingTopicSearch]);

  useEffect(() => {
    if (!isOpen) {
      cancelPendingTopicSearch();
      setIsLoading(false);
      return;
    }

    cancelPendingTopicSearch();

    const normalizedQuery = normalizeTopicQuery(query);
    const cachedTopics = cacheRef.current.get(normalizedQuery);

    if (cachedTopics) {
      setOptions(cachedTopics);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    debounceRef.current = window.setTimeout(() => {
      const controller = new AbortController();
      abortControllerRef.current = controller;

      searchTopics({
        query: normalizedQuery,
        limit: TOPIC_SEARCH_LIMIT,
        signal: controller.signal,
      })
        .then((topics) => {
          cacheRef.current.set(normalizedQuery, topics);
          setOptions(topics);
        })
        .catch((requestError) => {
          if (requestError instanceof DOMException && requestError.name === 'AbortError') {
            return;
          }
          setError(requestError instanceof Error ? requestError.message : 'Не удалось найти темы');
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setIsLoading(false);
          }
          if (abortControllerRef.current === controller) {
            abortControllerRef.current = null;
          }
        });
    }, TOPIC_SEARCH_DEBOUNCE_MS);

    return () => {
      cancelPendingTopicSearch();
    };
  }, [cancelPendingTopicSearch, isOpen, query]);

  const handleFocus = () => {
    setIsOpen(true);
  };

  const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
    const nextTarget = event.relatedTarget as HTMLElement | null;
    if (nextTarget && event.currentTarget.contains(nextTarget)) {
      return;
    }
    setIsOpen(false);
  };

  const selectTopic = (topic: TopicSummary) => {
    if (!selectedTopicIds.has(topic.id)) {
      onChange([...selectedTopics, topic]);
    }
    setQuery('');
    setIsOpen(true);
  };

  const removeTopic = (topicId: string) => {
    onChange(selectedTopics.filter((topic) => topic.id !== topicId));
  };

  return (
    <div className="topic-select form-field" onBlur={handleBlur}>
      <span>Темы</span>
      <div className="topic-select__control">
        {selectedTopics.map((topic) => (
          <button className="topic-select__chip" type="button" key={topic.id} onClick={() => removeTopic(topic.id)}>
            <span>{topic.name}</span>
            <X size={14} aria-hidden="true" />
          </button>
        ))}
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onFocus={handleFocus}
          placeholder={selectedTopics.length === 0 ? 'Найти тему' : 'Добавить тему'}
          aria-label="Поиск темы"
        />
      </div>

      {isOpen && (
        <div className="topic-select__popover" role="listbox" aria-label="Найденные темы">
          {isLoading && (
            <div className="topic-select__state">
              <LoaderCircle className="state-view__loader" size={16} aria-hidden="true" />
              <span>Поиск</span>
            </div>
          )}

          {!isLoading && error && <div className="topic-select__state topic-select__state--error">{error}</div>}

          {!isLoading && !error && visibleOptions.length === 0 && (
            <div className="topic-select__state">Темы не найдены</div>
          )}

          {!isLoading && !error && visibleOptions.map((topic) => (
            <button
              className="topic-select__option"
              type="button"
              key={topic.id}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => selectTopic(topic)}
              role="option"
              aria-selected="false"
            >
              <span>{topic.name}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

type TaskFormMode = 'create' | 'edit';

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

function TaskFormModal({
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
  const generationAbortControllerRef = useRef<AbortController | null>(null);
  const isCreateMode = mode === 'create';
  const title = isCreateMode ? 'Новая задача' : 'Редактирование задачи';
  const description = isCreateMode ? 'Заполните поля задачи для банка.' : 'Обновите поля задачи и сохраните изменения.';
  const submitLabel = isCreateMode ? 'Создать' : 'Сохранить';
  const pendingLabel = isCreateMode ? 'Создание' : 'Сохранение';

  useEffect(() => {
    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (isGeneratedDraftDialogOpen) {
          setIsGeneratedDraftDialogOpen(false);
          return;
        }
        onClose();
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isGeneratedDraftDialogOpen, onClose]);

  useEffect(() => () => {
    generationAbortControllerRef.current?.abort();
  }, []);

  const updateForm = (field: Exclude<keyof TaskFormState, 'topics'>, value: string) => {
    setForm((currentForm) => ({ ...currentForm, [field]: value }));
  };

  const updateTopics = (topics: TopicSummary[]) => {
    setForm((currentForm) => ({ ...currentForm, topics }));
  };

  const handleGenerateDraftClick = () => {
    if (!isCreateMode) {
      onNoteChange('AI-предложение для редактирования будет подключено позже.');
      return;
    }

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

    setForm((currentForm) => applyGeneratedDraftToForm(currentForm, generatedDraft));
    setIsAiPromptOpen(false);
    setGeneratedDraft(null);
    setGenerationError(null);
    setIsGeneratedDraftDialogOpen(false);
    onNoteChange('Черновик применен к форме. Проверьте текст и сохраните задачу.');
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

function TopicsView() {
  const { topicId } = useParams<{ topicId: string }>();
  const [topicsPage, setTopicsPage] = useState<PageResponse<Topic> | null>(null);
  const [selectedTopic, setSelectedTopic] = useState<Topic | null>(null);
  const [topicQuery, setTopicQuery] = useState('');
  const [topicPageNumber, setTopicPageNumber] = useState(0);
  const [isTopicsLoading, setIsTopicsLoading] = useState(true);
  const [isSelectedTopicLoading, setIsSelectedTopicLoading] = useState(false);
  const [topicsError, setTopicsError] = useState<string | null>(null);
  const [selectedTopicError, setSelectedTopicError] = useState<string | null>(null);
  const previousTopicIdRef = useRef<string | undefined>(topicId);

  const loadTopics = useCallback(async (page: number, query: string, parentId?: string, signal?: AbortSignal) => {
    setIsTopicsLoading(true);
    setTopicsError(null);

    try {
      const data = await getTopics({
        page,
        query,
        parentId,
        rootOnly: !parentId,
        size: TOPICS_PAGE_SIZE,
        signal,
      });
      setTopicsPage(data);
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return;
      }
      setTopicsError(requestError instanceof Error ? requestError.message : 'Не удалось получить темы');
    } finally {
      if (!signal?.aborted) {
        setIsTopicsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const topicChanged = previousTopicIdRef.current !== topicId;

    if (topicChanged) {
      previousTopicIdRef.current = topicId;

      if (topicPageNumber !== 0 || topicQuery !== '') {
        setTopicPageNumber(0);
        setTopicQuery('');
        return undefined;
      }
    }

    const controller = new AbortController();
    void loadTopics(topicPageNumber, topicQuery, topicId, controller.signal);

    return () => {
      controller.abort();
    };
  }, [loadTopics, topicId, topicPageNumber, topicQuery]);

  useEffect(() => {
    if (!topicId) {
      setSelectedTopic(null);
      setSelectedTopicError(null);
      setIsSelectedTopicLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    setIsSelectedTopicLoading(true);
    setSelectedTopicError(null);

    void getTopic(topicId, { signal: controller.signal })
      .then(setSelectedTopic)
      .catch((requestError) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') {
          return;
        }
        setSelectedTopic(null);
        setSelectedTopicError(requestError instanceof Error ? requestError.message : 'Не удалось получить тему');
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsSelectedTopicLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [topicId]);

  const topics = topicsPage?.content ?? [];
  const totalPages = topicsPage?.totalPages ?? 0;
  const displayedPageNumber = totalPages === 0 ? 0 : topicPageNumber + 1;
  const isLastPage = topicsPage?.last ?? true;
  const canGoBack = !isTopicsLoading && !topicsError && topicPageNumber > 0;
  const canGoForward = !isTopicsLoading && !topicsError && !isLastPage;
  const parentPath = selectedTopic?.parentId ? `/topics/${selectedTopic.parentId}` : '/topics';
  const title = selectedTopic?.name ?? (isSelectedTopicLoading ? 'Загрузка темы' : 'Темы');
  const emptyMessage = topicId ? 'Вложенные темы не найдены' : 'Корневые темы не найдены';

  return (
    <>
      <header className="workspace__header">
        <div>
          <h1 id="page-title">{title}</h1>
          {!topicId && (
            <p className="workspace__subtitle">
              Темы помогают группировать задачи по разделам программирования и быстрее находить нужный материал.
            </p>
          )}
          {selectedTopicError && <p className="form-error topics-header__error">{selectedTopicError}</p>}
        </div>

        {topicId && (
          <div className="topics-header__actions">
            <Link className="secondary-button secondary-button--icon" to={parentPath}>
              <ArrowLeft size={17} aria-hidden="true" />
              <span>К родителю</span>
            </Link>
            <Link className="text-button text-button--icon" to={`/tasks?topicId=${topicId}`}>
              <ClipboardList size={17} aria-hidden="true" />
              <span>Задачи</span>
            </Link>
          </div>
        )}
      </header>

      <div className="list-toolbar topics-toolbar">
        <label className="search-field">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder={topicId ? 'Поиск среди вложенных тем' : 'Поиск по теме'}
            value={topicQuery}
            onChange={(event) => {
              setTopicQuery(event.target.value);
              setTopicPageNumber(0);
            }}
          />
        </label>

        <div className="segmented-control" aria-label="Сортировка тем">
          <button className="segmented-control__item segmented-control__item--active" type="button">
            Имя
          </button>
          <button className="segmented-control__item" type="button" disabled>
            Новые
          </button>
        </div>
      </div>

      <section className="topics-section" aria-label="Список тем">
        {isTopicsLoading && (
          <div className="state-view">
            <LoaderCircle className="state-view__loader" size={28} aria-hidden="true" />
            <span>Загрузка тем</span>
          </div>
        )}

        {!isTopicsLoading && topicsError && (
          <div className="state-view state-view--error">
            <span>{topicsError}</span>
            <button className="text-button" type="button" onClick={() => loadTopics(topicPageNumber, topicQuery, topicId)}>
              Повторить
            </button>
          </div>
        )}

        {!isTopicsLoading && !topicsError && topics.length === 0 && (
          <div className="state-view">
            <span>{emptyMessage}</span>
          </div>
        )}

        {!isTopicsLoading && !topicsError && topics.length > 0 && (
          <div className="topics-grid">
            {topics.map((topic) => (
              <Link className="topic-card topic-card--interactive" to={`/topics/${topic.id}`} key={topic.id}>
                <h2>
                  <span>{topic.name}</span>
                </h2>
              </Link>
            ))}
          </div>
        )}
      </section>

      <nav className="pagination" aria-label="Пагинация тем">
        <button
          className="pagination__button"
          type="button"
          onClick={() => setTopicPageNumber((page) => Math.max(page - 1, 0))}
          disabled={!canGoBack}
          aria-label="Предыдущая страница"
          title="Предыдущая страница"
        >
          <ChevronLeft size={18} aria-hidden="true" />
        </button>

        <span className="pagination__status">
          Страница {displayedPageNumber} из {totalPages}
        </span>

        <button
          className="pagination__button"
          type="button"
          onClick={() => {
            if (!isLastPage) {
              setTopicPageNumber((page) => page + 1);
            }
          }}
          disabled={!canGoForward}
          aria-label="Следующая страница"
          title="Следующая страница"
        >
          <ChevronRight size={18} aria-hidden="true" />
        </button>
      </nav>
    </>
  );
}

type TasksListViewProps = {
  tasksPage: PageResponse<TaskSummary> | null;
  tasks: TaskSummary[];
  query: string;
  difficultyFilter: TaskDifficulty | '';
  pageSize: number;
  mineOnly: boolean;
  canFilterMine: boolean;
  hasTopicFilter: boolean;
  currentPage: number;
  isLoading: boolean;
  error: string | null;
  onQueryChange: (value: string) => void;
  onDifficultyChange: (value: TaskDifficulty | '') => void;
  onPageSizeChange: (value: number) => void;
  onMineOnlyChange: (value: boolean) => void;
  onCreateTaskClick: () => void;
  onLoadTasks: (page: number) => Promise<void>;
  onPreviousPage: () => void;
  onNextPage: () => void;
  canCreateTask: boolean;
};

function TasksListView({
  tasksPage,
  tasks,
  query,
  difficultyFilter,
  pageSize,
  mineOnly,
  canFilterMine,
  hasTopicFilter,
  currentPage,
  isLoading,
  error,
  onQueryChange,
  onDifficultyChange,
  onPageSizeChange,
  onMineOnlyChange,
  onCreateTaskClick,
  onLoadTasks,
  onPreviousPage,
  onNextPage,
  canCreateTask,
}: TasksListViewProps) {
  const totalPages = tasksPage?.totalPages ?? 0;
  const displayedPageNumber = totalPages === 0 ? 0 : currentPage + 1;
  const isLastPage = tasksPage?.last ?? true;
  const canGoBack = !isLoading && !error && currentPage > 0;
  const canGoForward = !isLoading && !error && !isLastPage;
  const hasActiveFilters = Boolean(query.trim() || difficultyFilter || mineOnly || hasTopicFilter);
  const hasPopupFilters = Boolean(difficultyFilter || mineOnly);
  const [isFilterMenuOpen, setIsFilterMenuOpen] = useState(false);
  const filterMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isFilterMenuOpen) {
      return undefined;
    }

    const handleOutsideMouseDown = (event: MouseEvent) => {
      const target = event.target;

      if (target instanceof Node && !filterMenuRef.current?.contains(target)) {
        setIsFilterMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideMouseDown);

    return () => {
      document.removeEventListener('mousedown', handleOutsideMouseDown);
    };
  }, [isFilterMenuOpen]);

  const resetTaskFilters = () => {
    onMineOnlyChange(false);
    onDifficultyChange('');
    setIsFilterMenuOpen(false);
  };

  return (
    <>
      <header className="workspace__header">
        <div>
          <h1 id="page-title">Банк задач</h1>
          <p className="workspace__subtitle">Задачи для уроков программирования и самостоятельной практики.</p>
        </div>

        <button
          className="icon-button icon-button--labeled icon-button--primary"
          type="button"
          onClick={onCreateTaskClick}
          disabled={!canCreateTask}
          title={canCreateTask ? 'Создать задачу' : 'Создание доступно преподавателю или администратору'}
        >
          <Plus size={18} aria-hidden="true" />
          <span>Создать</span>
        </button>
      </header>

      <div className="list-toolbar task-list-toolbar">
        <div className="task-list-toolbar__controls">
          <label className="search-field">
            <Search size={18} aria-hidden="true" />
            <input
              type="search"
              placeholder="Поиск по названию и условию"
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
            />
          </label>

          <div className="task-filter" ref={filterMenuRef}>
            <button
              className={`filter-button ${hasPopupFilters ? 'filter-button--active' : ''}`}
              type="button"
              onClick={() => setIsFilterMenuOpen((isOpen) => !isOpen)}
              aria-expanded={isFilterMenuOpen}
              aria-controls="task-filter-panel"
              aria-label="Фильтры задач"
              title="Фильтры задач"
            >
              <Filter size={19} aria-hidden="true" />
            </button>

            {isFilterMenuOpen && (
              <div className="task-filter__menu" id="task-filter-panel" aria-label="Фильтры задач">
                <label className="toggle-field" title={canFilterMine ? 'Показать только мои задачи' : 'Профиль пользователя еще загружается'}>
                  <input
                    type="checkbox"
                    checked={mineOnly}
                    disabled={!canFilterMine}
                    onChange={(event) => onMineOnlyChange(event.target.checked)}
                  />
                  <span>Мои задачи</span>
                </label>

                <label className="select-field">
                  <span>Сложность</span>
                  <select
                    value={difficultyFilter}
                    onChange={(event) => onDifficultyChange(event.target.value as TaskDifficulty | '')}
                  >
                    <option value="">Любая</option>
                    <option value="EASY">Легкая</option>
                    <option value="MEDIUM">Средняя</option>
                    <option value="HARD">Сложная</option>
                  </select>
                </label>

                <label className="select-field">
                  <span>На странице</span>
                  <select value={pageSize} onChange={(event) => onPageSizeChange(Number(event.target.value))}>
                    {TASK_PAGE_SIZE_OPTIONS.map((option) => (
                      <option value={option} key={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </label>

                <button className="secondary-button filter-reset" type="button" onClick={resetTaskFilters} disabled={!hasPopupFilters}>
                  Сбросить
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="list-toolbar__stats" aria-label="Сводка банка задач">
          <span>{tasks.length} из {tasksPage?.totalElements ?? 0}</span>
        </div>
      </div>

      <section className="task-list" aria-label="Список задач">
        {isLoading && (
          <div className="state-view">
            <LoaderCircle className="state-view__loader" size={28} aria-hidden="true" />
            <span>Загрузка задач</span>
          </div>
        )}

        {!isLoading && error && (
          <div className="state-view state-view--error">
            <span>{error}</span>
            <button className="text-button" type="button" onClick={() => onLoadTasks(currentPage)}>
              Повторить
            </button>
          </div>
        )}

        {!isLoading && !error && tasks.length === 0 && !hasActiveFilters && (
          <div className="state-view">
            <span>Задач пока нет</span>
          </div>
        )}

        {!isLoading && !error && tasks.length === 0 && hasActiveFilters && (
          <div className="state-view">
            <span>По этим фильтрам задач не найдено</span>
          </div>
        )}

        {!isLoading && !error && tasks.length > 0 && (
          <div className="task-table" aria-label="Задачи">
            <div className="task-table__header" aria-hidden="true">
              <span>Название</span>
              <span>Сложность</span>
            </div>

            {tasks.map((task) => (
              <Link className="task-row task-row--interactive" key={task.id} to={`/tasks/${task.id}`}>
                <h2>{task.title}</h2>
                <span className={difficultyClassNames[task.difficulty]}>{difficultyLabels[task.difficulty]}</span>
              </Link>
            ))}
          </div>
        )}
      </section>

      <nav className="pagination" aria-label="Пагинация задач">
        <button
          className="pagination__button"
          type="button"
          onClick={onPreviousPage}
          disabled={!canGoBack}
          aria-label="Предыдущая страница"
          title="Предыдущая страница"
        >
          <ChevronLeft size={18} aria-hidden="true" />
        </button>

        <span className="pagination__status">
          Страница {displayedPageNumber} из {totalPages}
        </span>

        <button
          className="pagination__button"
          type="button"
          onClick={onNextPage}
          disabled={!canGoForward}
          aria-label="Следующая страница"
          title="Следующая страница"
        >
          <ChevronRight size={18} aria-hidden="true" />
        </button>
      </nav>
    </>
  );
}

function TaskDetailView() {
  const auth = useAuth();
  const navigate = useNavigate();
  const { taskId } = useParams<{ taskId: string }>();
  const [task, setTask] = useState<TaskResponse | null>(null);
  const [isTaskLoading, setIsTaskLoading] = useState(true);
  const [taskError, setTaskError] = useState<string | null>(null);
  const [solutionCode, setSolutionCode] = useState('');
  const [isTaskMenuOpen, setIsTaskMenuOpen] = useState(false);
  const [isEditFormOpen, setIsEditFormOpen] = useState(false);
  const [isUpdatingTask, setIsUpdatingTask] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  const [editNote, setEditNote] = useState<string | null>(null);
  const [isDeletingTask, setIsDeletingTask] = useState(false);
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
            <MessageSquarePlus size={17} aria-hidden="true" />
            <span>AI</span>
          </button>
          <button className="secondary-button secondary-button--icon" type="button" disabled>
            <Play size={17} aria-hidden="true" />
            <span>Run</span>
          </button>
          <button className="text-button text-button--icon" type="button" disabled>
            <Send size={17} aria-hidden="true" />
            <span>Submit</span>
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
              Обсуждение с AI
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
          </div>
        </article>

        <section className="solution-panel" aria-label="Решение задачи">
          <header className="solution-panel__header">
            <div className="solution-panel__title">
              <Code2 size={19} aria-hidden="true" />
              <span>Code</span>
            </div>
            <select aria-label="Язык решения" defaultValue="" disabled>
              <option value="">Language</option>
            </select>
          </header>

          <textarea
            className="code-editor"
            value={solutionCode}
            onChange={(event) => setSolutionCode(event.target.value)}
            spellCheck={false}
            placeholder="Напишите решение здесь. Отправка в Judge0 будет подключена следующим шагом."
          />

          <footer className="judge-panel">
            <div className="judge-panel__tabs">
              <button className="judge-tab judge-tab--active" type="button">
                <Terminal size={16} aria-hidden="true" />
                Testcase
              </button>
              <button className="judge-tab" type="button" disabled>
                Test Result
              </button>
            </div>
            <div className="judge-panel__body">
              <span>Тест-кейсы будут загружаться вместе с проверкой решений.</span>
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
    </section>
  );
}

function App() {
  const auth = useAuth();
  const location = useLocation();
  const [tasksPage, setTasksPage] = useState<PageResponse<TaskSummary> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState<TaskDifficulty | ''>('');
  const [pageSize, setPageSize] = useState(DEFAULT_TASKS_PAGE_SIZE);
  const [mineOnly, setMineOnly] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [isCreateFormOpen, setIsCreateFormOpen] = useState(false);
  const [isCreatingTask, setIsCreatingTask] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formNote, setFormNote] = useState<string | null>(null);
  const taskTopicId = useMemo(() => {
    if (location.pathname !== '/tasks') {
      return undefined;
    }

    return new URLSearchParams(location.search).get('topicId') ?? undefined;
  }, [location.pathname, location.search]);
  const previousTaskTopicIdRef = useRef<string | undefined>(taskTopicId);

  const closeCreateForm = useCallback(() => {
    setIsCreateFormOpen(false);
    setFormError(null);
    setFormNote(null);
  }, []);

  const loadTasks = useCallback(async (page: number, signal?: AbortSignal) => {
    if (!auth.isAuthenticated) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await getTasks({
        page,
        size: pageSize,
        query,
        difficulty: difficultyFilter || undefined,
        authorId: mineOnly ? auth.profile?.id : undefined,
        topicId: taskTopicId,
        signal,
      });
      setTasksPage(data);
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return;
      }
      setError(requestError instanceof Error ? requestError.message : 'Не удалось получить задачи');
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false);
      }
    }
  }, [auth.isAuthenticated, auth.profile?.id, difficultyFilter, mineOnly, pageSize, query, taskTopicId]);

  useEffect(() => {
    if (!auth.isAuthenticated || location.pathname !== '/tasks') {
      return undefined;
    }

    const topicChanged = previousTaskTopicIdRef.current !== taskTopicId;

    if (topicChanged) {
      previousTaskTopicIdRef.current = taskTopicId;

      if (currentPage !== 0) {
        setCurrentPage(0);
        return undefined;
      }
    }

    const controller = new AbortController();
    void loadTasks(currentPage, controller.signal);

    return () => {
      controller.abort();
    };
  }, [auth.isAuthenticated, currentPage, loadTasks, location.pathname, taskTopicId]);

  const { authError, isAuthenticated, isInitializing, login } = auth;

  useEffect(() => {
    if (!isInitializing && !isAuthenticated && !authError) {
      void login();
    }
  }, [authError, isAuthenticated, isInitializing, login]);

  const tasks = tasksPage?.content ?? [];

  const isLastPage = tasksPage?.last ?? true;

  const goToPreviousPage = () => {
    setCurrentPage((page) => Math.max(page - 1, 0));
  };

  const goToNextPage = () => {
    if (!isLastPage) {
      setCurrentPage((page) => page + 1);
    }
  };

  const handleTaskQueryChange = (value: string) => {
    setQuery(value);
    setCurrentPage(0);
  };

  const handleDifficultyFilterChange = (value: TaskDifficulty | '') => {
    setDifficultyFilter(value);
    setCurrentPage(0);
  };

  const handlePageSizeChange = (value: number) => {
    setPageSize(value);
    setCurrentPage(0);
  };

  const handleMineOnlyChange = (value: boolean) => {
    setMineOnly(value);
    setCurrentPage(0);
  };

  const openCreateForm = () => {
    if (!auth.hasRole('TEACHER') && !auth.hasRole('ADMIN')) {
      setFormError('Создание задач доступно преподавателю или администратору.');
      return;
    }
    setIsCreateFormOpen(true);
    setFormError(null);
    setFormNote(null);
  };

  const handleCreateTask = async (form: TaskFormState) => {
    setIsCreatingTask(true);
    setFormError(null);
    setFormNote(null);

    try {
      await createTask(buildCreateTaskPayload(form));
      closeCreateForm();

      if (currentPage === 0) {
        await loadTasks(0);
      } else {
        setCurrentPage(0);
      }
    } catch (requestError) {
      setFormError(requestError instanceof Error ? requestError.message : 'Не удалось создать задачу');
    } finally {
      setIsCreatingTask(false);
    }
  };

  if (auth.isInitializing) {
    return (
      <main className="app-shell app-shell--auth">
        <section className="auth-screen" aria-live="polite">
          <img className="brand-mark auth-screen__mark" src="/brand/sfedu-logo.svg" alt="Южный федеральный университет" />
          <div className="state-view">
            <LoaderCircle className="state-view__loader" size={28} aria-hidden="true" />
            <span>Проверка входа</span>
          </div>
        </section>
      </main>
    );
  }

  if (!auth.isAuthenticated) {
    return (
      <main className="app-shell app-shell--auth">
        <section className="auth-screen" aria-live="polite">
          <img className="brand-mark auth-screen__mark" src="/brand/sfedu-logo.svg" alt="Южный федеральный университет" />
          {auth.authError ? (
            <div className="auth-panel">
              <h1>EduTask</h1>
              <p>Не удалось выполнить вход через Keycloak.</p>
              <p className="form-error">{auth.authError}</p>
            </div>
          ) : (
            <div className="state-view">
              <LoaderCircle className="state-view__loader" size={28} aria-hidden="true" />
              <span>Переход к Keycloak</span>
            </div>
          )}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <section className="workspace" aria-labelledby="page-title">
        <div className="top-bar">
          <div className="top-bar__brand">
            <img className="brand-mark" src="/brand/sfedu-logo.svg" alt="Южный федеральный университет" />
            <span className="top-bar__name">EduTask</span>
          </div>
          <div className="top-bar__account">
            <span className="account-chip">
              <UserRound size={16} aria-hidden="true" />
              <span>{auth.user?.name ?? auth.user?.username ?? auth.user?.email ?? 'Пользователь'}</span>
              {auth.profile && <strong>{auth.profile.role}</strong>}
            </span>
            <button className="icon-button" type="button" onClick={auth.logout} aria-label="Выйти" title="Выйти">
              <LogOut size={18} aria-hidden="true" />
            </button>
          </div>
        </div>

        <Routes>
          <Route path="/" element={<Navigate to="/tasks" replace />} />
          <Route
            path="/tasks"
            element={
              <TasksListView
                tasksPage={tasksPage}
                tasks={tasks}
                query={query}
                difficultyFilter={difficultyFilter}
                pageSize={pageSize}
                mineOnly={mineOnly}
                canFilterMine={Boolean(auth.profile?.id)}
                hasTopicFilter={Boolean(taskTopicId)}
                currentPage={currentPage}
                isLoading={isLoading}
                error={error}
                onQueryChange={handleTaskQueryChange}
                onDifficultyChange={handleDifficultyFilterChange}
                onPageSizeChange={handlePageSizeChange}
                onMineOnlyChange={handleMineOnlyChange}
                onCreateTaskClick={openCreateForm}
                onLoadTasks={loadTasks}
                onPreviousPage={goToPreviousPage}
                onNextPage={goToNextPage}
                canCreateTask={auth.hasRole('TEACHER') || auth.hasRole('ADMIN')}
              />
            }
          />
          <Route path="/tasks/:taskId" element={<TaskDetailView />} />
          <Route path="/topics" element={<TopicsView />} />
          <Route path="/topics/:topicId" element={<TopicsView />} />
          <Route path="*" element={<Navigate to="/tasks" replace />} />
        </Routes>
      </section>

      <aside className="side-nav" aria-label="Основные разделы">
        <NavLink
          className={({ isActive }) => `side-nav__item ${isActive ? 'side-nav__item--active' : ''}`}
          to="/tasks"
          aria-label="Банк задач"
          title="Банк задач"
        >
          <ClipboardList size={22} aria-hidden="true" />
        </NavLink>
        <NavLink
          className={({ isActive }) => `side-nav__item ${isActive ? 'side-nav__item--active' : ''}`}
          to="/topics"
          aria-label="Темы"
          title="Темы"
        >
          <Tags size={22} aria-hidden="true" />
        </NavLink>
        <button className="side-nav__item" type="button" aria-label="Чат с LLM" title="Чат с LLM" disabled>
          <BotMessageSquare size={22} aria-hidden="true" />
        </button>
      </aside>

      {isCreateFormOpen && (
        <TaskFormModal
          mode="create"
          initialValue={emptyTaskForm}
          isSubmitting={isCreatingTask}
          error={formError}
          note={formNote}
          onClose={closeCreateForm}
          onNoteChange={setFormNote}
          onSubmit={handleCreateTask}
        />
      )}
    </main>
  );
}

export default App;
