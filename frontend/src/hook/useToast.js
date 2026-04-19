import { useState, useCallback } from 'react';

let _add = null;

export function useToastManager() {
  const [toasts, setToasts] = useState([]);

  const add = useCallback((msg, type = 'info') => {
    const id = Date.now();
    setToasts(p => [...p, { id, msg, type }]);
    setTimeout(() => setToasts(p => p.filter(t => t.id !== id)), 3000);
  }, []);

  _add = add;
  return { toasts };
}

export const toast = {
  success: (m) => _add?.(m, 'success'),
  error: (m) => _add?.(m, 'error'),
  info: (m) => _add?.(m, 'info'),
};