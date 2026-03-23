import { apiFetch, apiUrl } from './client';

export interface AuthUser {
  id: number;
  loginId: string;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string;
  profileImageUrl?: string;
  /** 자기소개 (백엔드 /api/auth/me 응답) */
  bio?: string;
  role?: string;
  /** 팡 잔액 (백엔드 /api/auth/me 응답) */
  pangBalance?: number;
  /** 가입일 (백엔드 응답) */
  createdAt?: string;
  /** 레벨 (1~9999) */
  level?: number;
  /** 현재 레벨 내 경험치 0.1 단위. 경험치 바 표시용 */
  experienceInCurrentLevelTenths?: number;
  /** 다음 레벨까지 필요한 경험치 0.1 단위. 경험치 바 표시용 */
  experienceRequiredForNextLevelTenths?: number;
  /** 마일리지(원). 팡 결제 금액의 5% 적립 */
  mileage?: number;
  /** 광고 제거 만료일시 */
  adFreeUntil?: string;
}

export async function fetchMe(): Promise<AuthUser | null> {
  const { ok, data } = await apiFetch<AuthUser>('/api/auth/me');
  return ok && data ? data : null;
}

/** 초기 로드용: 401이면 한 번 재시도 (새로고침 직후 쿠키 전달 지연 완화) */
export async function fetchMeWithRetry(): Promise<AuthUser | null> {
  const { ok, data, status } = await apiFetch<AuthUser>('/api/auth/me');
  if (ok && data) return data;
  if (status === 401) {
    await new Promise((r) => setTimeout(r, 150));
    return fetchMe();
  }
  return null;
}

export async function login(loginId: string, password: string): Promise<{ ok: boolean; data?: AuthUser; message?: string }> {
  const { ok, data, message } = await apiFetch<AuthUser>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ loginId, password }),
  });
  return { ok, data: data as AuthUser | undefined, message };
}

export async function logout(): Promise<void> {
  await apiFetch('/api/auth/logout', { method: 'POST' });
}

/**
 * 구글 로그인: API 서버의 OAuth2 진입점으로 이동. 로그인 완료 후 백엔드가 app.frontend.url 로 리다이렉트.
 */
export function getGoogleLoginUrl(): string {
  return apiUrl('/oauth2/authorization/google');
}

/**
 * 카카오 로그인: API 서버의 OAuth2 진입점으로 이동. 로그인 완료 후 백엔드가 app.frontend.url 로 리다이렉트.
 */
export function getKakaoLoginUrl(): string {
  return apiUrl('/oauth2/authorization/kakao');
}

/**
 * 네이버 로그인: API 서버의 OAuth2 진입점으로 이동. 로그인 완료 후 백엔드가 app.frontend.url 로 리다이렉트.
 */
export function getNaverLoginUrl(): string {
  return apiUrl('/oauth2/authorization/naver');
}

/** 전화번호 중복 확인. 회원가입 시 사용. available=true 면 사용 가능 */
export async function checkPhoneAvailable(phone: string): Promise<{ available: boolean }> {
  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/phone?value=${encodeURIComponent(phone.trim())}`
  );
  return { available: ok && data?.available === true };
}

/**
 * 닉네임 중복 확인 (프로필 편집). 로그인 시 본인이 쓰는 닉네임은 사용 가능으로 처리됨.
 * GET /api/auth/check/nickname?value=...
 */
export async function checkNicknameAvailable(nickname: string): Promise<{ ok: boolean; available: boolean }> {
  const v = nickname.trim();
  if (!v) return { ok: false, available: false };
  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/nickname?value=${encodeURIComponent(v)}`
  );
  if (!ok) return { ok: false, available: false };
  return { ok: true, available: data?.available === true };
}

/** 아이디 찾기: 휴대폰 번호 + 인증번호로 가입된 아이디 목록 조회. recaptchaToken 있으면 백엔드에서 검증 */
export async function findLoginIdByPhone(
  phone: string,
  verificationCode: string,
  recaptchaToken?: string | null
): Promise<{ ok: boolean; loginIds?: string[]; message?: string }> {
  const body: Record<string, string> = { phone: phone.trim(), verificationCode: (verificationCode || '').trim() };
  if (recaptchaToken) body.recaptchaToken = recaptchaToken;
  const { ok, data, message } = await apiFetch<{ loginIds: string[] }>('/api/auth/find-login-id', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return { ok, loginIds: data?.loginIds, message };
}

/** 아이디 존재 여부 (비밀번호 찾기용). true면 가입된 아이디임 */
export async function checkLoginIdExists(loginId: string): Promise<boolean> {
  const v = loginId.trim();
  if (!v) return false;
  const { ok, data } = await apiFetch<{ available?: boolean }>(
    `/api/auth/check/loginId?value=${encodeURIComponent(v)}`
  );
  return !!(ok && data && (data as { available?: boolean }).available === false);
}

/** 비밀번호 찾기: 아이디·휴대폰 인증 후 새 비밀번호로 재설정. recaptchaToken 있으면 백엔드에서 검증 */
export async function resetPasswordByPhone(
  loginId: string,
  phone: string,
  verificationCode: string,
  newPassword: string,
  recaptchaToken?: string | null
): Promise<{ ok: boolean; message?: string }> {
  const body: Record<string, string> = {
    loginId: loginId.trim(),
    phone: phone.trim(),
    verificationCode: (verificationCode || '').trim(),
    newPassword,
  };
  if (recaptchaToken) body.recaptchaToken = recaptchaToken;
  const { ok, data, message } = await apiFetch<{ message?: string }>('/api/auth/reset-password-by-phone', {
    method: 'POST',
    body: JSON.stringify(body),
  });
  return { ok, message: data?.message ?? message };
}
