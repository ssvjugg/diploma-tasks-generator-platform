import { useEffect } from 'react';

let lockCount = 0;
let previousBodyOverflow = '';

export function useBodyScrollLock(enabled = true) {
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }

    if (lockCount === 0) {
      previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
    }

    lockCount += 1;

    return () => {
      lockCount = Math.max(0, lockCount - 1);

      if (lockCount === 0) {
        document.body.style.overflow = previousBodyOverflow;
        previousBodyOverflow = '';
      }
    };
  }, [enabled]);
}
