import { type FormEvent, lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  BotMessageSquare,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Code2,
  LoaderCircle,
  MessageSquarePlus,
  Plus,
  Play,
  Search,
  Send,
  Sparkles,
  Tags,
  Terminal,
  X,
} from 'lucide-react';
import * as markdownCommands from '@uiw/react-md-editor/commands';
import '@uiw/react-md-editor/markdown-editor.css';
import { Link, Navigate, NavLink, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { createTask, getTask, getTasks } from './api/tasks';
import { getTopics } from './api/topics';
import { resolveCurrentAuthorId } from './auth/currentUser';
import type { PageResponse } from './types/page';
import type { TaskCreateRequest, TaskDifficulty, TaskResponse, TaskSummary } from './types/task';
import type { Topic } from './types/topic';

const TASKS_PAGE_SIZE = 20;
const TOPICS_PAGE_SIZE = 12;
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
  topicIds: string;
  languageIds: string;
};

const emptyTaskForm: TaskFormState = {
  title: '',
  statement: '',
  inputFormat: '',
  outputFormat: '',
  difficulty: 'EASY',
  topicIds: '',
  languageIds: '',
};

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

function TopicsView() {
  const [topicsPage, setTopicsPage] = useState<PageResponse<Topic> | null>(null);
  const [topicQuery, setTopicQuery] = useState('');
  const [topicPageNumber, setTopicPageNumber] = useState(0);
  const [isTopicsLoading, setIsTopicsLoading] = useState(true);
  const [topicsError, setTopicsError] = useState<string | null>(null);

  const loadTopics = useCallback(async (page: number, query: string) => {
    setIsTopicsLoading(true);
    setTopicsError(null);

    try {
      const data = await getTopics({ page, query, size: TOPICS_PAGE_SIZE });
      setTopicsPage(data);
    } catch (requestError) {
      setTopicsError(requestError instanceof Error ? requestError.message : 'Не удалось получить темы');
    } finally {
      setIsTopicsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTopics(topicPageNumber, topicQuery);
  }, [loadTopics, topicPageNumber, topicQuery]);

  const topics = topicsPage?.content ?? [];
  const totalPages = topicsPage?.totalPages ?? 0;
  const displayedPageNumber = totalPages === 0 ? 0 : topicPageNumber + 1;
  const isLastPage = topicsPage?.last ?? true;
  const canGoBack = !isTopicsLoading && !topicsError && topicPageNumber > 0;
  const canGoForward = !isTopicsLoading && !topicsError && !isLastPage;

  return (
    <>
      <header className="workspace__header">
        <div>
          <h1 id="page-title">Темы</h1>
          <p className="workspace__subtitle">
            Темы помогают группировать задачи по разделам программирования и быстрее находить нужный материал.
          </p>
        </div>
      </header>

      <div className="list-toolbar topics-toolbar">
        <label className="search-field">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Поиск по теме"
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
            <button className="text-button" type="button" onClick={() => loadTopics(topicPageNumber, topicQuery)}>
              Повторить
            </button>
          </div>
        )}

        {!isTopicsLoading && !topicsError && topics.length === 0 && (
          <div className="state-view">
            <span>Темы не найдены</span>
          </div>
        )}

        {!isTopicsLoading && !topicsError && topics.length > 0 && (
          <div className="topics-grid">
            {topics.map((topic) => (
              <article className="topic-card" key={topic.id}>
                <h2>
                  <span>{topic.name}</span>
                </h2>
              </article>
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

function TaskDetailView() {
  const navigate = useNavigate();
  const { taskId } = useParams<{ taskId: string }>();
  const [task, setTask] = useState<TaskResponse | null>(null);
  const [isTaskLoading, setIsTaskLoading] = useState(true);
  const [taskError, setTaskError] = useState<string | null>(null);
  const [solutionCode, setSolutionCode] = useState('');

  const loadTask = useCallback(async () => {
    if (!taskId) {
      setTaskError('Не указан идентификатор задачи');
      setIsTaskLoading(false);
      return;
    }

    setIsTaskLoading(true);
    setTaskError(null);

    try {
      const data = await getTask(taskId);
      setTask(data);
    } catch (requestError) {
      setTaskError(requestError instanceof Error ? requestError.message : 'Не удалось получить задачу');
    } finally {
      setIsTaskLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    void loadTask();
  }, [loadTask]);

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
        <button className="text-button" type="button" onClick={loadTask}>
          Повторить
        </button>
      </div>
    );
  }

  const primaryLanguage = task.supportedLanguages[0];

  return (
    <section className="task-detail" aria-labelledby="page-title">
      <header className="task-detail__topbar">
        <button className="task-detail__back" type="button" onClick={() => navigate('/tasks')}>
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Список задач</span>
        </button>

        <div className="task-detail__actions" aria-label="Действия с задачей">
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
            <select aria-label="Язык решения" defaultValue={primaryLanguage?.id ?? ''}>
              {task.supportedLanguages.length === 0 && <option value="">Language</option>}
              {task.supportedLanguages.map((language) => (
                <option value={language.id} key={language.id}>
                  {language.name}
                </option>
              ))}
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
    </section>
  );
}

function App() {
  const [tasksPage, setTasksPage] = useState<PageResponse<TaskSummary> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [isCreateFormOpen, setIsCreateFormOpen] = useState(false);
  const [taskForm, setTaskForm] = useState<TaskFormState>(emptyTaskForm);
  const [isCreatingTask, setIsCreatingTask] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formNote, setFormNote] = useState<string | null>(null);

  const closeCreateForm = useCallback(() => {
    setIsCreateFormOpen(false);
    setTaskForm(emptyTaskForm);
    setFormError(null);
    setFormNote(null);
  }, []);

  const loadTasks = useCallback(async (page: number) => {
    setIsLoading(true);
    setError(null);

    try {
      const data = await getTasks({ page, size: TASKS_PAGE_SIZE });
      setTasksPage(data);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Не удалось получить задачи');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTasks(currentPage);
  }, [currentPage, loadTasks]);

  useEffect(() => {
    if (!isCreateFormOpen) {
      return undefined;
    }

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeCreateForm();
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener('keydown', handleEscape);
    };
  }, [closeCreateForm, isCreateFormOpen]);

  const tasks = useMemo(() => {
    const source = tasksPage?.content ?? [];
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return source;
    }

    return source.filter((task) => task.title.toLowerCase().includes(normalizedQuery));
  }, [query, tasksPage]);

  const totalPages = tasksPage?.totalPages ?? 0;
  const displayedPageNumber = totalPages === 0 ? 0 : currentPage + 1;
  const isLastPage = tasksPage?.last ?? true;
  const canGoBack = !isLoading && !error && currentPage > 0;
  const canGoForward = !isLoading && !error && !isLastPage;

  const goToPreviousPage = () => {
    setCurrentPage((page) => Math.max(page - 1, 0));
  };

  const goToNextPage = () => {
    if (!isLastPage) {
      setCurrentPage((page) => page + 1);
    }
  };

  const openCreateForm = () => {
    setIsCreateFormOpen(true);
    setFormError(null);
    setFormNote(null);
  };

  const updateTaskForm = (field: keyof TaskFormState, value: string) => {
    setTaskForm((form) => ({ ...form, [field]: value }));
  };

  const parseUuidList = (value: string) =>
    value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);

  const parseLanguageIds = (value: string) =>
    value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map(Number)
      .filter(Number.isFinite);

  const buildCreateTaskPayload = (): TaskCreateRequest => {
    const authorId = resolveCurrentAuthorId();

    if (!authorId) {
      throw new Error('Не удалось определить текущего пользователя. Для dev-режима задайте VITE_DEV_AUTHOR_ID.');
    }

    return {
      title: taskForm.title.trim(),
      statement: taskForm.statement.trim(),
      inputFormat: taskForm.inputFormat.trim() || undefined,
      outputFormat: taskForm.outputFormat.trim() || undefined,
      difficulty: taskForm.difficulty,
      authorId,
      topicIds: parseUuidList(taskForm.topicIds),
      languageIds: parseLanguageIds(taskForm.languageIds),
    };
  };

  const handleGenerateDraftClick = () => {
    setFormNote('AI-предложение позже заполнит эти поля, а преподаватель сможет принять или поправить значения.');
  };

  const handleCreateTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsCreatingTask(true);
    setFormError(null);
    setFormNote(null);

    try {
      await createTask(buildCreateTaskPayload());
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

  return (
    <main className="app-shell">
      <section className="workspace" aria-labelledby="page-title">
        <div className="top-bar">
          <img className="brand-mark" src="/brand/sfedu-logo.svg" alt="Южный федеральный университет" />
          <span className="top-bar__name">EduTask</span>
        </div>

        <Routes>
          <Route path="/" element={<Navigate to="/tasks" replace />} />
          <Route
            path="/tasks"
            element={
              <>
            <header className="workspace__header">
              <div>
                <h1 id="page-title">Банк задач</h1>
                <p className="workspace__subtitle">Задачи для уроков программирования и самостоятельной практики.</p>
              </div>

              <button
                className="icon-button icon-button--labeled icon-button--primary"
                type="button"
                onClick={openCreateForm}
              >
                <Plus size={18} aria-hidden="true" />
                <span>Создать</span>
              </button>
            </header>

            <div className="list-toolbar">
              <label className="search-field">
                <Search size={18} aria-hidden="true" />
                <input
                  type="search"
                  placeholder="Поиск по названию"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                />
              </label>
              <div className="list-toolbar__stats" aria-label="Сводка банка задач">
                <span>{tasksPage?.totalElements ?? 0} всего</span>
                <span>{tasks.length} показано</span>
                <span>{TASKS_PAGE_SIZE} на странице</span>
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
                  <button className="text-button" type="button" onClick={() => loadTasks(currentPage)}>
                    Повторить
                  </button>
                </div>
              )}

              {!isLoading && !error && tasks.length === 0 && !query && (
                <div className="state-view">
                  <span>Задач пока нет</span>
                </div>
              )}

              {!isLoading && !error && tasks.length === 0 && query && (
                <div className="state-view">
                  <span>По этому запросу задач не найдено</span>
                </div>
              )}

              {!isLoading && !error && tasks.length > 0 && (
                <div className="task-table" role="table" aria-label="Задачи">
                  <div className="task-table__header" role="row">
                    <span role="columnheader">Название</span>
                    <span role="columnheader">Сложность</span>
                  </div>

                  {tasks.map((task) => (
                    <Link
                      className="task-row task-row--interactive"
                      key={task.id}
                      to={`/tasks/${task.id}`}
                    >
                      <h2>{task.title}</h2>
                      <span className={difficultyClassNames[task.difficulty]}>
                        {difficultyLabels[task.difficulty]}
                      </span>
                    </Link>
                  ))}
                </div>
              )}
            </section>

            <nav className="pagination" aria-label="Пагинация задач">
              <button
                className="pagination__button"
                type="button"
                onClick={goToPreviousPage}
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
                onClick={goToNextPage}
                disabled={!canGoForward}
                aria-label="Следующая страница"
                title="Следующая страница"
              >
                <ChevronRight size={18} aria-hidden="true" />
              </button>
            </nav>
              </>
            }
          />
          <Route path="/tasks/:taskId" element={<TaskDetailView />} />
          <Route path="/topics" element={<TopicsView />} />
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
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
            closeCreateForm();
          }
        }}>
          <section className="task-modal" role="dialog" aria-modal="true" aria-labelledby="create-task-title">
            <header className="task-modal__header">
              <div>
                <h2 id="create-task-title">Новая задача</h2>
                <p>Заполните поля задачи для банка.</p>
              </div>

              <div className="task-modal__actions">
                <button
                  className="ai-action"
                  type="button"
                  onClick={handleGenerateDraftClick}
                  aria-label="Предложить заполнение через AI"
                  title="Предложить заполнение через AI"
                >
                  <Sparkles size={16} aria-hidden="true" />
                  <span>AI</span>
                </button>
                <button className="modal-close" type="button" onClick={closeCreateForm} aria-label="Закрыть форму">
                  <X size={18} aria-hidden="true" />
                </button>
              </div>
            </header>

            <form className="task-form" onSubmit={handleCreateTask}>
              <label className="form-field">
                <span>Название</span>
                <input
                  value={taskForm.title}
                  onChange={(event) => updateTaskForm('title', event.target.value)}
                  maxLength={255}
                  required
                />
              </label>

              <MarkdownField
                label="Условие"
                value={taskForm.statement}
                rows={5}
                required
                onChange={(value) => updateTaskForm('statement', value)}
              />

              <div className="form-grid">
                <MarkdownField
                  label="Формат входных данных"
                  value={taskForm.inputFormat}
                  rows={3}
                  onChange={(value) => updateTaskForm('inputFormat', value)}
                />

                <MarkdownField
                  label="Формат выходных данных"
                  value={taskForm.outputFormat}
                  rows={3}
                  onChange={(value) => updateTaskForm('outputFormat', value)}
                />
              </div>

              <div className="form-grid form-grid--compact">
                <label className="form-field">
                  <span>Сложность</span>
                  <select
                    value={taskForm.difficulty}
                    onChange={(event) => updateTaskForm('difficulty', event.target.value as TaskDifficulty)}
                  >
                    <option value="EASY">Легкая</option>
                    <option value="MEDIUM">Средняя</option>
                    <option value="HARD">Сложная</option>
                  </select>
                </label>

                <label className="form-field">
                  <span>Темы UUID</span>
                  <input
                    value={taskForm.topicIds}
                    onChange={(event) => updateTaskForm('topicIds', event.target.value)}
                    placeholder="через запятую"
                  />
                </label>

                <label className="form-field">
                  <span>Языки ID</span>
                  <input
                    value={taskForm.languageIds}
                    onChange={(event) => updateTaskForm('languageIds', event.target.value)}
                    placeholder="1, 2, 3"
                    inputMode="numeric"
                  />
                </label>
              </div>

              {formNote && <p className="form-note">{formNote}</p>}
              {formError && <p className="form-error">{formError}</p>}

              <footer className="task-form__footer">
                <button className="secondary-button" type="button" onClick={closeCreateForm}>
                  Отмена
                </button>
                <button className="text-button" type="submit" disabled={isCreatingTask}>
                  {isCreatingTask ? 'Создание' : 'Создать'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}

export default App;
