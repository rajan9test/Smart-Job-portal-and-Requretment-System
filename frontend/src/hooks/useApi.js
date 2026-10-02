import { useCallback, useEffect, useState } from 'react';

/**
 * Loads data on mount and whenever `deps` change.
 * Returns { data, error, loading, reload }.
 */
export function useApi(load, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: null }));
    load()
      .then((data) => !cancelled && setState({ data, error: null, loading: false }))
      .catch((error) => !cancelled && setState({ data: null, error, loading: false }));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { ...state, reload };
}

/**
 * For button clicks and form submits. `run(fn)` captures errors and a busy flag.
 * Returns true if fn succeeded, false if it threw.
 */
export function useAction() {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const run = useCallback(async (fn) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      return true;
    } catch (e) {
      setError(e);
      return false;
    } finally {
      setBusy(false);
    }
  }, []);

  return { run, error, busy, clearError: () => setError(null) };
}
