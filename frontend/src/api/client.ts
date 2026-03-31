function isLoopbackHost(hostname: string): boolean {
  return hostname === '127.0.0.1' || hostname === 'localhost';
}

/** VITE_API_URL이 localhost/127.0.0.1을 가리키는지 (빌드 시 .env 고정값) */
function envPointsToLoopback(apiBaseEnv: string): boolean {
  try {
    const u = new URL(apiBaseEnv);
    return isLoopbackHost(u.hostname);
  } catch {
    return false;
  }
}

/**
 * 브라우저 주소 기준 API 베이스.
 * - 로컬: Vite(5173 등)에서 열었을 때 백엔드는 보통 :8080 → 호스트:8080.
 * - 배포: https://nip.io 처럼 443(포트 생략)으로 열렸으면 API도 같은 호스트·같은 포트만 쓴다(nginx가 8080으로 프록시).
 *   여기서 :8080을 붙이면 https://nip.io:8080 이 되어, Tomcat은 TLS가 없어 ERR_SSL_PROTOCOL_ERROR 가 난다.
 */
function apiBaseFromBrowserLocation(): string {
  const { protocol, hostname, port } = window.location;
  if (isLoopbackHost(hostname)) {
    return `${protocol}//${hostname}:8080`;
  }
  if (!port) {
    return `${protocol}//${hostname}`;
  }
  return `${protocol}//${hostname}:${port}`;
}

/**
 * API 베이스 URL.
 * - localhost/127.0.0.1 로 접속 시: 같은 호스트:8080 (세션 쿠키 same-site).
 * - 그 외 호스트(예: EC2)인데 VITE_API_URL이 localhost로 박혀 있으면: 빌드값 대신 현재 창과 같은 오리진 베이스 사용.
 * - 그 밖에는 VITE_API_URL 또는 브라우저 위치 기반.
 */
function getApiBase(): string {
  if (typeof window !== 'undefined') {
    const { hostname } = window.location;
    if (isLoopbackHost(hostname)) {
      return apiBaseFromBrowserLocation();
    }
  }
  const envRaw = import.meta.env.VITE_API_URL;
  const envTrim = envRaw != null ? String(envRaw).trim() : '';
  if (envTrim !== '') {
    if (typeof window !== 'undefined' && envPointsToLoopback(envTrim)) {
      const { hostname } = window.location;
      if (!isLoopbackHost(hostname)) {
        return apiBaseFromBrowserLocation();
      }
    }
    return envTrim.replace(/\/+$/, '');
  }
  if (typeof window !== 'undefined') {
    return apiBaseFromBrowserLocation();
  }
  return 'http://localhost:8080';
}

/** 외부 전적 API 429 / Rate limit 시 사용자 안내 (백엔드와 동일 문구) */
export const RATE_LIMIT_USER_MESSAGE_KO =
  '전적 API 요청이 많아 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.';

const RATE_LIMIT_TEXT_PATTERN = /429|too\s*many\s*requests|rate\s*limit/i;

/**
 * JSON 문자열에 백슬래시+n 이 그대로 들어온 경우(실제 줄바꿈이 아님) 화면 표시용으로 치환.
 * 이미 진짜 줄바꿈(0x0A)만 있는 문자열은 그대로 둔다.
 */
export function decodeApiTextNewlines(text: string): string {
  return text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    .replace(/\\t/g, '\t');
}

export function normalizeRateLimitUserMessage(
  text: string | undefined | null,
  httpStatus?: number,
): string | undefined | null {
  if (httpStatus === 429) {
    return RATE_LIMIT_USER_MESSAGE_KO;
  }
  if (text != null && text !== '' && RATE_LIMIT_TEXT_PATTERN.test(text)) {
    return RATE_LIMIT_USER_MESSAGE_KO;
  }
  return text;
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
  const rawMessage = data && typeof data === 'object' && 'message' in data ? (data as { message?: string }).message : undefined;
  const decoded = rawMessage != null && rawMessage !== '' ? decodeApiTextNewlines(rawMessage) : rawMessage;
  const message = normalizeRateLimitUserMessage(decoded, res.status) ?? decoded ?? undefined;
  return {
    ok: res.ok,
    status: res.status,
    data: data as T | undefined,
    message,
  };
}
