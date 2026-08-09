import { useQuery } from '@tanstack/react-query';
import { searchApi } from '../api/searchApi';
import { useState, useEffect, useRef } from 'react';

export function useSearch(keyword: string) {
  return useQuery({
    queryKey: ['search', keyword],
    queryFn: () => searchApi.search(keyword),
    enabled: keyword.length >= 2,
  });
}

export function useAutocomplete(keyword: string) {
  return useQuery({
    queryKey: ['autocomplete', keyword],
    queryFn: () => searchApi.autocomplete(keyword),
    enabled: keyword.length >= 2,
    staleTime: 30_000,
  });
}

export function useDebouncedValue<T>(value: T, delay: number = 300): T {
  const [debounced, setDebounced] = useState(value);
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    timerRef.current = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timerRef.current);
  }, [value, delay]);

  return debounced;
}
