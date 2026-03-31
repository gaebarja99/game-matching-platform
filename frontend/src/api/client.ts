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
 * HTTPS로 연 페이지에서 http API URL을 쓰면 브라우저가 혼합 콘텐츠로 막음(로그인 fetch가 빨간색·프리플라이트 실패처럼 보임).
 * VITE_API_URL이 http://공인IP:8080 처럼 박혀 있어도, 실제 접속이 https://…nip.io 이면 같은 오리진으로 맞춘다.
 */
function envWouldBreakHttpsPage(apiBaseEnv: string): boolean {
  if (typeof window === 'undefined') return false;
  if (window.location.protocol !== 'https:') return false;
  try {
    const u = new URL(apiBaseEnv);
    return u.protocol === 'http:';
  } catch {
    return false;
  }
}

/**
 * nginx(443 TLS) → 내부만 8080(평문) 인 배포에서 흔한 실수: VITE_API_URL에 https://도메인:8080 을 넣음.
 * 브라우저는 8080에 TLS 핸드셰이크를 보내고 Tomcat은 평문이라 요청이 막히거나 "Provisional headers" 만 보임.
 * 주소창이 https://도메인/ (포트 생략=443)일 때는 API도 같은 오리진(포트 없음)만 쓴다.
 */
function envHttpsSameHostWrongPublicPort8080(apiBaseEnv: string): boolean {
  if (typeof window === 'undefined') return false;
  try {
    const u = new URL(apiBaseEnv);
    const p = new URL(window.location.href);
    if (u.hostname !== p.hostname) return false;
    if (u.port !== '8080') return false;
    if (p.protocol !== 'https:') return false;
    const pagePort = p.port === '' ? '443' : p.port;
    return pagePort === '443';
  } catch {
    return false;
  }
}

/**
 * `https://…:8080` 은 이 배포(nginx 443 TLS → 내부 8080 평문)에서 거의 항상 오설정이다.
 * 예전에는 https 페이지에서만 :8080을 뗐는데, Vite(http://5173)·Simple Browser·HTTP로 열면
 * 조건이 안 걸려 OAuth가 `https://…:8080/oauth2/…` 로 그대로 나가 ERR_SSL_PROTOCOL_ERROR 가 났다.
 * 페이지 프로토콜과 무관하게 https 베이스의 :8080만 제거한다. (http://localhost:8080 은 그대로 둠)
 */
function stripMisleadingHttpsPort8080FromApiBase(base: string): string {
  try {
    const u = new URL(base);
    if (u.protocol !== 'https:' || u.port !== '8080') return base;
    const pathPart = u.pathname === '/' ? '' : `${u.pathname}`;
    return `https://${u.hostname}${pathPart}${u.search}`;
  } catch {
    return base;
  }
}

/**
 * 브라우저 주소 기준 API 베이스.
 * - 로컬: Vite(5173 등)에서 열었을 때 백엔드는 보통 :8080 → 호스트:8080.
 * - 배포: https://3.37.67.151.nip.io 처럼 443(포트 생략)으로 열렸으면 API도 같은 호스트·같은 포트만 쓴다(nginx가 8080으로 프록시).
 *   여기서 :8080을 붙이면 TLS 없는 Tomcat으로 가서 ERR_SSL_PROTOCOL_ERROR 가 난다.
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
function resolveApiBase(): string {
  if (typeof window !== 'undefined') {
    const { hostname } = window.location;
    if (isLoopbackHost(hostname)) {
      return apiBaseFromBrowserLocation();
    }
  }
  const envRaw = import.meta.env.VITE_API_URL;
  const envTrim = envRaw != null ? String(envRaw).trim() : '';
  if (envTrim !== '') {
    if (typeof window !== 'undefined') {
      if (envPointsToLoopback(envTrim)) {
        const { hostname } = window.location;
        if (!isLoopbackHost(hostname)) {
          return apiBaseFromBrowserLocation();
        }
      } else if (envWouldBreakHttpsPage(envTrim)) {
        return apiBaseFromBrowserLocation();
      } else if (envHttpsSameHostWrongPublicPort8080(envTrim)) {
        return apiBaseFromBrowserLocation();
      }
    }
    return envTrim.replace(/\/+$/, '');
  }
  if (typeof window !== 'undefined') {
    return apiBaseFromBrowserLocation();
  }
  return 'https://3.37.67.151.nip.io';
}

function getApiBase(): string {
  return stripMisleadingHttpsPort8080FromApiBase(resolveApiBase());
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
