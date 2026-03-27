import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

const THEME_KEY = 'gamematcher-theme';
type Theme = 'dark' | 'light';

interface ThemeContextValue {
  theme: Theme;
  setTheme: (t: Theme) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    try {
      const s = localStorage.getItem(THEME_KEY);
      if (s === 'light' || s === 'dark') return s;
      if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: light)').matches) {
        return 'light';
      }
      return 'dark';
    } catch {
      if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: light)').matches) {
        return 'light';
      }
      return 'dark';
    }
  });

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    let hasManualPreference = false;
    try {
      const s = localStorage.getItem(THEME_KEY);
      hasManualPreference = s === 'light' || s === 'dark';
    } catch {
      hasManualPreference = false;
    }
    if (hasManualPreference) return;
    const mql = window.matchMedia('(prefers-color-scheme: light)');
    const onChange = (e: MediaQueryListEvent) => {
      setThemeState(e.matches ? 'light' : 'dark');
    };
    if (mql.addEventListener) mql.addEventListener('change', onChange);
    else mql.addListener(onChange);
    return () => {
      if (mql.removeEventListener) mql.removeEventListener('change', onChange);
      else mql.removeListener(onChange);
    };
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  const setTheme = useCallback((t: Theme) => {
    setThemeState(t);
    try {
      localStorage.setItem(THEME_KEY, t);
    } catch {}
  }, []);
  const toggleTheme = useCallback(() => {
    setThemeState((prev) => {
      const next = prev === 'dark' ? 'light' : 'dark';
      try {
        localStorage.setItem(THEME_KEY, next);
      } catch {}
      return next;
    });
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme는 ThemeProvider 내부에서만 사용할 수 있습니다.');
  return ctx;
}
