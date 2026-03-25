/**
 * API 베이스 URL. 로컬 개발(127.0.0.1 / localhost)에서는 항상 현재 창 host + 8080 사용해
 * same-site로 세션 쿠키 전송. .env의 VITE_API_URL이 localhost면 127.0.0.1 접속 시 쿠키가 안 붙어
 * 로그인 유지가 안 되므로, 로컬일 때는 hostname 기준으로 통일.
 */
function getApiBase(): string {
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location;
    if (hostname === '127.0.0.1' || hostname === 'localhost') {
      return `${protocol}//${hostname}:8080`;
    }
  }
  const env = import.meta.env.VITE_API_URL;
  if (env != null && String(env).trim() !== '') return String(env).trim();
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location;
    return `${protocol}//${hostname}:8080`;
  }
  return 'http://localhost:8080';
}

export function apiUrl(path: string): string {
  const base = getApiBase().replace(/\/+$/, '');
  const p = path.startsWith('/') ? path : `/${path}`;
  return `${base}${p}`;
}

/** 채팅 WebSocket(STOMP) 엔드포인트. SockJS 사용 시 이 URL로 연결 */
export function getWsUrl(): string {
  return `${getApiBase().replace(/\/+$/, '')}/ws`;
}

/**
 * 프로필 이미지 URL을 브라우저에서 로드 가능한 절대 URL로 변환.
 * 백엔드는 상대 경로(/uploads/profile/...)를 반환하므로, 프론트 오리진이 아닌 API 주소를 붙여야 함.
 */
export function resolveProfileImageUrl(url: string | null | undefined): string | null {
  if (url == null || url === '') return null;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return apiUrl(url.replace(/^\//, ''));
}

/**
 * credentials: 'include' 로 세션 쿠키 전송 (분리형에서 CORS + 쿠키 필수).
 */
export async function apiFetch<T = unknown>(
  path: string,
  init?: RequestInit
): Promise<{ ok: boolean; status: number; data?: T; message?: string }> {
  const url = apiUrl(path);
  const isFormData = typeof FormData !== 'undefined' && init?.body instanceof FormData;
  const headers = new Headers(init?.headers || undefined);
  if (!isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const res = await fetch(url, {
    ...init,
    credentials: 'include',
    headers,
  });
  let data: T | { message?: string } | null = null;
  const ct = res.headers.get('content-type');
  if (ct?.includes('application/json')) {
    try {
      data = (await res.json()) as T | { message?: string };
    } catch {
      // ignore
    }
  }
  const message = data && typeof data === 'object' && 'message' in data ? (data as { message?: string }).message : undefined;
  return {
    ok: res.ok,
    status: res.status,
    data: data as T | undefined,
    message,
  };
}
