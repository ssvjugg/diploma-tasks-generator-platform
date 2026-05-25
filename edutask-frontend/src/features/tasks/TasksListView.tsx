import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Filter, LoaderCircle, Plus, Search } from 'lucide-react';
import type { PageResponse } from '../../types/page';
import type { TaskDifficulty, TaskSummary } from '../../types/task';
import { difficultyClassNames, difficultyLabels, TASK_PAGE_SIZE_OPTIONS } from './taskFormModel';

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

export function TasksListView({
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
