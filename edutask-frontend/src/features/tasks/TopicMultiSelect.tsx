import { type FocusEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { LoaderCircle, X } from 'lucide-react';
import { searchTopics } from '../../api/topics';
import type { TopicSummary } from '../../types/topic';

const TOPIC_SEARCH_LIMIT = 12;
const TOPIC_SEARCH_DEBOUNCE_MS = 250;

type TopicMultiSelectProps = {
  selectedTopics: TopicSummary[];
  onChange: (topics: TopicSummary[]) => void;
};

const normalizeTopicQuery = (query: string) => query.trim().toLowerCase();

export function TopicMultiSelect({ selectedTopics, onChange }: TopicMultiSelectProps) {
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
