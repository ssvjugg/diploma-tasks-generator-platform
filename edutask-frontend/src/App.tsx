import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { BotMessageSquare, ClipboardList, LoaderCircle, LogOut, Tags, UserRound } from 'lucide-react';
import { Navigate, NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { createTask, getTasks } from './api/tasks';
import { useAuth } from './auth/AuthContext';
import { TaskDetailView } from './features/tasks/TaskDetailView';
import { TaskFormModal } from './features/tasks/TaskFormModal';
import { TasksListView } from './features/tasks/TasksListView';
import {
  buildCreateTaskPayload,
  DEFAULT_TASKS_PAGE_SIZE,
  emptyTaskForm,
  type TaskFormState,
} from './features/tasks/taskFormModel';
import { TopicsView } from './features/topics/TopicsView';
import type { PageResponse } from './types/page';
import type { TaskDifficulty, TaskSummary } from './types/task';

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
