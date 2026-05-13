import { type FormEvent, lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import {
  BotMessageSquare,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  LoaderCircle,
  Plus,
  Search,
  Sparkles,
  Tags,
  X,
} from 'lucide-react';
import '@uiw/react-md-editor/markdown-editor.css';
import { createTask, getTasks } from './api/tasks';
import { getTopics } from './api/topics';
import { resolveCurrentAuthorId } from './auth/currentUser';
import type { PageResponse } from './types/page';
import type { TaskCreateRequest, TaskDifficulty, TaskSummary } from './types/task';
import type { Topic } from './types/topic';

const TASKS_PAGE_SIZE = 20;
const TOPICS_PAGE_SIZE = 12;
const MarkdownEditor = lazy(() => import('@uiw/react-md-editor'));

type AppSection = 'tasks' | 'topics';

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

function App() {
  const [activeSection, setActiveSection] = useState<AppSection>('tasks');
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

        {activeSection === 'tasks' ? (
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
                    <article className="task-row" key={task.id} role="row">
                      <h2 role="cell">{task.title}</h2>
                      <span className={difficultyClassNames[task.difficulty]} role="cell">
                        {difficultyLabels[task.difficulty]}
                      </span>
                    </article>
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
        ) : (
          <TopicsView />
        )}
      </section>

      <aside className="side-nav" aria-label="Основные разделы">
        <button
          className={`side-nav__item ${activeSection === 'tasks' ? 'side-nav__item--active' : ''}`}
          type="button"
          onClick={() => setActiveSection('tasks')}
          aria-label="Банк задач"
          title="Банк задач"
        >
          <ClipboardList size={22} aria-hidden="true" />
        </button>
        <button
          className={`side-nav__item ${activeSection === 'topics' ? 'side-nav__item--active' : ''}`}
          type="button"
          onClick={() => setActiveSection('topics')}
          aria-label="Темы"
          title="Темы"
        >
          <Tags size={22} aria-hidden="true" />
        </button>
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
