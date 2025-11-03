import { useCallback, useEffect, useState } from "react";

export function useAsync(asyncFn, deps = []) {
  const [state, setState] = useState({ data: null, isLoading: true, error: null });

  const execute = useCallback(async () => {
    setState({ data: null, isLoading: true, error: null });
    try {
      const data = await asyncFn();
      setState({ data, isLoading: false, error: null });
    } catch (error) {
      setState({ data: null, isLoading: false, error });
    }
  }, deps);

  useEffect(() => {
    execute();
  }, [execute]);

  return { ...state, refresh: execute };
}
