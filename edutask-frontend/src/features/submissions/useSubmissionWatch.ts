import { type Dispatch, type SetStateAction, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getCodeSubmission, streamCodeSubmission } from '../../api/submissions';
import type { CodeSubmissionResponse } from '../../types/submission';
import { isTerminalSubmissionStatus } from '../../types/submission';
import { SUBMISSION_POLL_INTERVAL_MS } from './submissionConfig';

type UseSubmissionWatchResult = {
  activeSubmission: CodeSubmissionResponse | null;
  setActiveSubmission: Dispatch<SetStateAction<CodeSubmissionResponse | null>>;
  isSubmissionLive: boolean;
  submissionError: string | null;
  setSubmissionError: Dispatch<SetStateAction<string | null>>;
  cancelSubmissionWatch: () => void;
  watchSubmission: (submissionId: string) => void;
  isSubmissionInProgress: boolean;
};

export function useSubmissionWatch(): UseSubmissionWatchResult {
  const [activeSubmission, setActiveSubmission] = useState<CodeSubmissionResponse | null>(null);
  const [isSubmissionLive, setIsSubmissionLive] = useState(false);
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const streamControllerRef = useRef<AbortController | null>(null);
  const pollingTimeoutRef = useRef<number | null>(null);
  const watchVersionRef = useRef(0);

  const cancelWatchResources = useCallback(() => {
    watchVersionRef.current += 1;
    streamControllerRef.current?.abort();
    streamControllerRef.current = null;

    if (pollingTimeoutRef.current !== null) {
      window.clearTimeout(pollingTimeoutRef.current);
      pollingTimeoutRef.current = null;
    }
  }, []);

  const cancelSubmissionWatch = useCallback(() => {
    cancelWatchResources();
    setIsSubmissionLive(false);
  }, [cancelWatchResources]);

  const scheduleSubmissionPolling = useCallback((submissionId: string, watchVersion: number) => {
    if (pollingTimeoutRef.current !== null) {
      window.clearTimeout(pollingTimeoutRef.current);
    }

    pollingTimeoutRef.current = window.setTimeout(() => {
      if (watchVersionRef.current !== watchVersion) {
        return;
      }

      void getCodeSubmission(submissionId)
        .then((submission) => {
          if (watchVersionRef.current !== watchVersion) {
            return;
          }

          setActiveSubmission(submission);

          if (isTerminalSubmissionStatus(submission.status)) {
            setIsSubmissionLive(false);
            return;
          }

          scheduleSubmissionPolling(submissionId, watchVersion);
        })
        .catch((requestError) => {
          if (watchVersionRef.current !== watchVersion) {
            return;
          }

          setSubmissionError(requestError instanceof Error ? requestError.message : 'Не удалось обновить статус проверки');
          setIsSubmissionLive(false);
        });
    }, SUBMISSION_POLL_INTERVAL_MS);
  }, []);

  const watchSubmission = useCallback((submissionId: string) => {
    cancelWatchResources();

    const watchVersion = watchVersionRef.current;
    const controller = new AbortController();
    let latestSubmission: CodeSubmissionResponse | null = null;

    streamControllerRef.current = controller;
    setIsSubmissionLive(true);

    void streamCodeSubmission(submissionId, {
      signal: controller.signal,
      onMessage: (submission) => {
        if (watchVersionRef.current !== watchVersion) {
          return;
        }

        latestSubmission = submission;
        setActiveSubmission(submission);

        if (isTerminalSubmissionStatus(submission.status)) {
          setIsSubmissionLive(false);
          controller.abort();
        }
      },
    })
      .then(() => {
        if (watchVersionRef.current !== watchVersion) {
          return;
        }

        setIsSubmissionLive(false);

        if (!latestSubmission || !isTerminalSubmissionStatus(latestSubmission.status)) {
          setIsSubmissionLive(true);
          scheduleSubmissionPolling(submissionId, watchVersion);
        }
      })
      .catch((requestError) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') {
          return;
        }

        if (watchVersionRef.current !== watchVersion) {
          return;
        }

        setSubmissionError(
          requestError instanceof Error
            ? `${requestError.message}. Переключаюсь на периодическое обновление.`
            : 'SSE поток недоступен. Переключаюсь на периодическое обновление.',
        );
        setIsSubmissionLive(true);
        scheduleSubmissionPolling(submissionId, watchVersion);
      });
  }, [cancelWatchResources, scheduleSubmissionPolling]);

  const isSubmissionInProgress = useMemo(
    () => Boolean(activeSubmission && !isTerminalSubmissionStatus(activeSubmission.status)),
    [activeSubmission],
  );

  useEffect(() => cancelWatchResources, [cancelWatchResources]);

  return {
    activeSubmission,
    setActiveSubmission,
    isSubmissionLive,
    submissionError,
    setSubmissionError,
    cancelSubmissionWatch,
    watchSubmission,
    isSubmissionInProgress,
  };
}
