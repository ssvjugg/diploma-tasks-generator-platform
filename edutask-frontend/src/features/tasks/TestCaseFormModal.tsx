import { type FormEvent, useEffect, useState } from 'react';
import { X } from 'lucide-react';
import type { TestCaseFormMode, TestCaseFormState } from './taskFormModel';
import { useBodyScrollLock } from './useBodyScrollLock';

type TestCaseFormModalProps = {
  mode: TestCaseFormMode;
  initialValue: TestCaseFormState;
  isSubmitting: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (form: TestCaseFormState) => Promise<void>;
};

export function TestCaseFormModal({
  mode,
  initialValue,
  isSubmitting,
  error,
  onClose,
  onSubmit,
}: TestCaseFormModalProps) {
  const [form, setForm] = useState<TestCaseFormState>(initialValue);
  const [localError, setLocalError] = useState<string | null>(null);
  const isCreateMode = mode === 'create';
  const title = isCreateMode ? 'Новый тест-кейс' : 'Редактирование тест-кейса';
  const description = isCreateMode ? 'Данные для проверки решения.' : 'Обновите данные проверки.';
  const submitLabel = isCreateMode ? 'Добавить' : 'Сохранить';
  const pendingLabel = isCreateMode ? 'Добавление' : 'Сохранение';

  useBodyScrollLock();

  useEffect(() => {
    setForm(initialValue);
    setLocalError(null);
  }, [initialValue]);

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      window.removeEventListener('keydown', handleEscape);
    };
  }, [onClose]);

  const updateForm = <Field extends keyof TestCaseFormState>(field: Field, value: TestCaseFormState[Field]) => {
    setForm((currentForm) => ({ ...currentForm, [field]: value }));
    setLocalError(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const points = Number(form.points);

    if (!Number.isInteger(points) || points < 0) {
      setLocalError('Баллы должны быть целым неотрицательным числом.');
      return;
    }

    await onSubmit({ ...form, points: String(points) });
  };

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose();
      }
    }}>
      <section className="task-modal" role="dialog" aria-modal="true" aria-labelledby="test-case-form-title">
        <header className="task-modal__header">
          <div>
            <h2 id="test-case-form-title">{title}</h2>
            <p>{description}</p>
          </div>

          <button className="modal-close" type="button" onClick={onClose} aria-label="Закрыть форму">
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <form className="task-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Входные данные</span>
            <textarea
              value={form.inputData}
              onChange={(event) => updateForm('inputData', event.target.value)}
              rows={8}
              spellCheck={false}
            />
          </label>

          <label className="form-field">
            <span>Ожидаемый вывод</span>
            <textarea
              value={form.expectedOutput}
              onChange={(event) => updateForm('expectedOutput', event.target.value)}
              rows={8}
              spellCheck={false}
            />
          </label>

          <div className="form-grid form-grid--test-case-meta">
            <label className="form-field">
              <span>Баллы</span>
              <input
                type="number"
                min={0}
                step={1}
                value={form.points}
                onChange={(event) => updateForm('points', event.target.value)}
                required
              />
            </label>

            <label className="toggle-field test-case-hidden-toggle">
              <input
                type="checkbox"
                checked={form.hidden}
                onChange={(event) => updateForm('hidden', event.target.checked)}
              />
              <span>Скрытый тест</span>
            </label>
          </div>

          {localError && <p className="form-error">{localError}</p>}
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
  );
}
