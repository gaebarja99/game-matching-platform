import { useCallback, useEffect, useRef } from 'react';

const DEFAULT_MAX_PX = 168;

/** DM 입력 등 — 값이 바뀔 때마다 높이를 scrollHeight에 맞춤 (최대 maxPx) */
export function useAutoResizeTextarea(value: string, maxPx = DEFAULT_MAX_PX) {
  const ref = useRef<HTMLTextAreaElement>(null);
  const resize = useCallback(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, maxPx)}px`;
  }, [maxPx]);

  useEffect(() => {
    resize();
  }, [value, resize]);

  return { ref, resize };
}
