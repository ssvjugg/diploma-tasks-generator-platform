import { LoaderCircle } from 'lucide-react';
import type { CodeSubmissionResponse, CodeSubmissionTestResultResponse } from '../../types/submission';
import { isTerminalSubmissionStatus } from '../../types/submission';
import type { TestCaseResponse } from '../../types/testCase';
import {
  formatSubmissionLanguageLabel,
  formatSubmissionStatus,
  submissionStatusClassNames,
} from './submissionConfig';

export type JudgePanelTab = 'testcases' | 'results';

type SubmissionSummaryProps = {
  submission: CodeSubmissionResponse;
  isLive: boolean;
};

function SubmissionSummary({ submission, isLive }: SubmissionSummaryProps) {
  const isTerminal = isTerminalSubmissionStatus(submission.status);

  return (
    <section className="submission-summary" aria-label="Статус проверки">
      <div>
        <span className={submissionStatusClassNames[submission.status]}>{formatSubmissionStatus(submission.status)}</span>
        {isLive && !isTerminal && (
          <span className="submission-live">
            <LoaderCircle className="state-view__loader" size={14} aria-hidden="true" />
            Live
          </span>
        )}
      </div>
      <div className="submission-summary__metrics">
        <span>{submission.passedCount}/{submission.totalCount} тестов</span>
        <span>{submission.score}/{submission.maxScore} баллов</span>
        <span>{formatSubmissionLanguageLabel(submission.language)}</span>
      </div>
      {submission.errorMessage && <p>{submission.errorMessage}</p>}
    </section>
  );
}

type PublicTestCasesPanelProps = {
  testCases: TestCaseResponse[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
};

export function PublicTestCasesPanel({ testCases, isLoading, error, onRetry }: PublicTestCasesPanelProps) {
  const publicTestCases = testCases.filter((testCase) => !testCase.hidden);
  const hiddenCount = testCases.length - publicTestCases.length;

  if (isLoading) {
    return (
      <div className="judge-panel-state">
        <LoaderCircle className="state-view__loader" size={17} aria-hidden="true" />
        <span>Загрузка тестов</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="judge-panel-state judge-panel-state--error">
        <span>{error}</span>
        <button className="text-button" type="button" onClick={onRetry}>
          Повторить
        </button>
      </div>
    );
  }

  if (publicTestCases.length === 0) {
    return (
      <div className="judge-panel-state">
        <span>{hiddenCount > 0 ? 'Все тесты скрыты' : 'Открытые тесты пока не добавлены'}</span>
      </div>
    );
  }

  return (
    <div className="judge-public-tests">
      {publicTestCases.map((testCase, index) => (
        <article className="judge-public-test" key={testCase.id}>
          <header>
            <strong>Открытый тест {index + 1}</strong>
            <span>{testCase.points} баллов</span>
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

      {hiddenCount > 0 && <p className="judge-panel-note">Скрытых тестов: {hiddenCount}</p>}
    </div>
  );
}

type SubmissionResultsPanelProps = {
  submission: CodeSubmissionResponse | null;
  isSubmitting: boolean;
  isLive: boolean;
  error: string | null;
};

export function SubmissionResultsPanel({ submission, isSubmitting, isLive, error }: SubmissionResultsPanelProps) {
  if (isSubmitting && !submission) {
    return (
      <div className="judge-panel-state">
        <LoaderCircle className="state-view__loader" size={17} aria-hidden="true" />
        <span>Отправляем решение</span>
      </div>
    );
  }

  if (!submission) {
    if (error) {
      return (
        <div className="judge-panel-state judge-panel-state--error">
          <span>{error}</span>
        </div>
      );
    }

    return (
      <div className="judge-panel-state">
        <span>Отправьте решение, чтобы увидеть статус и результаты тестов.</span>
      </div>
    );
  }

  return (
    <div className="submission-results">
      <SubmissionSummary submission={submission} isLive={isLive} />

      {error && <p className="form-error">{error}</p>}

      {submission.testResults.length === 0 && (
        <div className="judge-panel-state">
          <span>Результаты тестов появятся после начала проверки.</span>
        </div>
      )}

      {submission.testResults.length > 0 && (
        <div className="submission-results__list">
          {submission.testResults.map((result) => (
            <SubmissionResultItem result={result} key={result.id} />
          ))}
        </div>
      )}
    </div>
  );
}

type SubmissionResultItemProps = {
  result: CodeSubmissionTestResultResponse;
};

function SubmissionResultItem({ result }: SubmissionResultItemProps) {
  const isAccepted = result.status === 'ACCEPTED';
  const isPending = !isTerminalSubmissionStatus(result.status);
  const className = [
    'submission-result',
    isAccepted ? 'submission-result--accepted' : '',
    !isAccepted && !isPending ? 'submission-result--failed' : '',
    isPending ? 'submission-result--pending' : '',
  ].filter(Boolean).join(' ');

  return (
    <article className={className}>
      <header className="submission-result__header">
        <div>
          <strong>Тест {result.index + 1}</strong>
          <span className={`test-case-visibility ${result.hidden ? 'test-case-visibility--hidden' : ''}`}>
            {result.hidden ? 'Скрытый' : 'Открытый'}
          </span>
          <span className={submissionStatusClassNames[result.status]}>{formatSubmissionStatus(result.status)}</span>
        </div>
        <span>{result.points} баллов</span>
      </header>

      <div className="submission-result__meta">
        {result.time !== null && <span>Время: {result.time} c</span>}
        {result.memory !== null && <span>Память: {result.memory} KB</span>}
        {result.hidden && result.errorMessage && <span>{result.errorMessage}</span>}
      </div>

      {!result.hidden && (
        <div className="submission-result__io">
          <label>
            <span>Ввод</span>
            <pre>{result.inputData ?? 'Пустой ввод'}</pre>
          </label>
          <label>
            <span>Ожидаемый вывод</span>
            <pre>{result.expectedOutput ?? 'Пустой вывод'}</pre>
          </label>
          <label>
            <span>Фактический вывод</span>
            <pre>{result.actualOutput ?? 'Пустой вывод'}</pre>
          </label>
          {(result.stderr || result.compileOutput || result.errorMessage) && (
            <label>
              <span>Ошибка</span>
              <pre>{result.stderr ?? result.compileOutput ?? result.errorMessage}</pre>
            </label>
          )}
        </div>
      )}
    </article>
  );
}
