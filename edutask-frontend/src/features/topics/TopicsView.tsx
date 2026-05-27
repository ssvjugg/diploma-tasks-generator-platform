import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ChevronLeft, ChevronRight, ClipboardList, LoaderCircle, Search } from 'lucide-react';
import { getTopic, getTopics } from '../../api/topics';
import type { PageResponse } from '../../types/page';
import type { Topic } from '../../types/topic';

const TOPICS_PAGE_SIZE = 12;

export function TopicsView() {
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
