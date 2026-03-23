import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { AuthUser } from '../api/auth';
import { fetchMe, fetchMeWithRetry, getGoogleLoginUrl, getKakaoLoginUrl, getNaverLoginUrl, login as apiLogin, logout as apiLogout } from '../api/auth';
import { clearPushToken, setupPushNotifications } from '../lib/pushNotifications';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (loginId: string, password: string) => Promise<{ ok: boolean; message?: string }>;
  logout: () => Promise<void>;
  googleLoginUrl: string;
  kakaoLoginUrl: string;
  naverLoginUrl: string;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const u = await fetchMe();
    setUser(u);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const u = await fetchMeWithRetry();
      if (!cancelled) {
        setUser(u);
      }
      if (!cancelled) setLoading(false);
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!user) return;
    setupPushNotifications().catch(() => {});
  }, [user?.id]);

  const login = useCallback(async (loginId: string, password: string) => {
    const { ok, data, message } = await apiLogin(loginId, password);
    if (ok && data) setUser(data);
    return { ok, message };
  }, []);

  const logout = useCallback(async () => {
    await clearPushToken();
    await apiLogout();
    setUser(null);
  }, []);

  const value: AuthContextValue = {
    user,
    loading,
    login,
    logout,
    googleLoginUrl: getGoogleLoginUrl(),
    kakaoLoginUrl: getKakaoLoginUrl(),
    naverLoginUrl: getNaverLoginUrl(),
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.');
  return ctx;
}
