import { apiFetch, apiUrl } from './client';

export interface AuthUser {
  id: number;
  loginId: string;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string;
  profileImageUrl?: string;
  bio?: string;
  role?: string;
  pangBalance?: number;
  createdAt?: string;
  level?: number;
  experienceInCurrentLevelTenths?: number;
  experienceRequiredForNextLevelTenths?: number;
  mileage?: number;
  adFreeUntil?: string;
}

function countHangul(text: string): number {
  return (text.match(/[가-힣]/g) ?? []).length;
}

function countSuspiciousMojibake(text: string): number {
  return (text.match(/[À-ÿ�]/g) ?? []).length;
}

export function repairMojibakeText(value?: string | null): string | undefined {
  if (!value) return value ?? undefined;

  const original = value.trim();
  if (!original) return original;
  if (!/[À-ÿ�]/.test(original)) return original;

  try {
    const bytes = Uint8Array.from(Array.from(original).map((char) => char.charCodeAt(0) & 0xff));
    const repaired = new TextDecoder('utf-8').decode(bytes).trim();
    if (!repaired) return original;

    const originalHangul = countHangul(original);
    const repairedHangul = countHangul(repaired);
    const originalSuspicious = countSuspiciousMojibake(original);
    const repairedSuspicious = countSuspiciousMojibake(repaired);

    if (repairedHangul > originalHangul || repairedSuspicious < originalSuspicious) {
      return repaired;
    }
  } catch {
    return original;
  }

  return original;
}

export function normalizeAuthUser(user: AuthUser | null): AuthUser | null {
  if (!user) return null;

  const username = repairMojibakeText(user.username) ?? user.username;
  const nickname = repairMojibakeText(user.nickname) ?? user.nickname;
  const loginId = repairMojibakeText(user.loginId) ?? user.loginId;
  const bio = repairMojibakeText(user.bio) ?? user.bio;

  return {
    ...user,
    username,
    nickname,
    loginId,
    bio,
  };
}

export async function fetchMe(): Promise<AuthUser | null> {
  const { ok, data } = await apiFetch<AuthUser>('/api/auth/me');
  return ok && data ? normalizeAuthUser(data) : null;
}

export async function fetchMeWithRetry(): Promise<AuthUser | null> {
  const { ok, data, status } = await apiFetch<AuthUser>('/api/auth/me');
  if (ok && data) return normalizeAuthUser(data);
  if (status === 401) {
    await new Promise((resolve) => setTimeout(resolve, 150));
    return fetchMe();
  }
  return null;
}

export async function login(
  loginId: string,
  password: string,
): Promise<{ ok: boolean; data?: AuthUser; message?: string }> {
  const { ok, data, message } = await apiFetch<AuthUser>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ loginId, password }),
  });
  return { ok, data: normalizeAuthUser(data as AuthUser | null) ?? undefined, message };
}

export async function logout(): Promise<void> {
  await apiFetch('/api/auth/logout', { method: 'POST' });
}

export function getGoogleLoginUrl(): string {
  return apiUrl('/oauth2/authorization/google');
}

export function getKakaoLoginUrl(): string {
  return apiUrl('/oauth2/authorization/kakao');
}

export function getNaverLoginUrl(): string {
  return apiUrl('/oauth2/authorization/naver');
}

export async function checkPhoneAvailable(phone: string): Promise<{ available: boolean }> {
  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/phone?value=${encodeURIComponent(phone.trim())}`,
  );
  return { available: ok && data?.available === true };
}

export async function checkNicknameAvailable(nickname: string): Promise<{ ok: boolean; available: boolean }> {
  const value = nickname.trim();
  if (!value) return { ok: false, available: false };

  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/nickname?value=${encodeURIComponent(value)}`,
  );
  if (!ok) return { ok: false, available: false };
  return { ok: true, available: data?.available === true };
}

export async function findLoginIdByPhone(
  phone: string,
  recaptchaToken?: string | null,
): Promise<{ ok: boolean; loginIds?: string[]; message?: string }> {
  const body: Record<string, string> = {
    phone: phone.trim(),
  };
  if (recaptchaToken) body.recaptchaToken = recaptchaToken;

  const { ok, data, message } = await apiFetch<{ loginIds: string[] }>('/api/auth/find-login-id', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return { ok, loginIds: data?.loginIds, message };
}

export async function checkLoginIdExists(loginId: string): Promise<boolean> {
  const value = loginId.trim();
  if (!value) return false;

  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/loginId?value=${encodeURIComponent(value)}`,
  );
  return !!(ok && data && (data as { available?: boolean }).available === false);
}

export async function resetPasswordByPhone(
  loginId: string,
  phone: string,
  newPassword: string,
  recaptchaToken?: string | null,
): Promise<{ ok: boolean; message?: string }> {
  const body: Record<string, string> = {
    loginId: loginId.trim(),
    phone: phone.trim(),
    newPassword,
  };
  if (recaptchaToken) body.recaptchaToken = recaptchaToken;

  const { ok, data, message } = await apiFetch<{ message?: string }>('/api/auth/reset-password-by-phone', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return { ok, message: data?.message ?? message };
}
