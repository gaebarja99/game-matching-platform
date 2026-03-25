/**
 * reCAPTCHA Enterprise 스크립트를 미리 로드합니다.
 * Firebase 전화 인증이 reCAPTCHA Enterprise를 사용할 때,
 * 스크립트가 먼저 로드되어 있으면 "Failed to initialize reCAPTCHA enterprise verification" / 타임아웃을 줄일 수 있습니다.
 * .env에 VITE_RECAPTCHA_ENTERPRISE_SITE_KEY 를 설정하면 앱 시작 시 자동으로 로드됩니다.
 */
const SCRIPT_ID = 'recaptcha-enterprise-script';

export function loadRecaptchaEnterpriseScript(): void {
  const key = import.meta.env.VITE_RECAPTCHA_ENTERPRISE_SITE_KEY;
  if (!key || String(key).trim() === '') return;
  if (document.getElementById(SCRIPT_ID)) return;

  const script = document.createElement('script');
  script.id = SCRIPT_ID;
  script.src = `https://www.google.com/recaptcha/enterprise.js?render=${encodeURIComponent(key.trim())}`;
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
}

declare global {
  interface Window {
    grecaptcha?: {
      enterprise?: {
        ready: (cb: () => void) => void;
        execute: (siteKey: string, options: { action: string }) => Promise<string>;
      };
    };
  }
}

/**
 * reCAPTCHA Enterprise 토큰 발급. 백엔드 assessment API 검증용.
 * @param action 예: FIND_LOGIN_ID, RESET_PASSWORD
 * @returns 토큰 또는 실패 시 null
 */
export function getRecaptchaEnterpriseToken(action: string): Promise<string | null> {
  const siteKey = import.meta.env.VITE_RECAPTCHA_ENTERPRISE_SITE_KEY;
  if (!siteKey || String(siteKey).trim() === '') return Promise.resolve(null);

  return new Promise((resolve) => {
    if (typeof window.grecaptcha?.enterprise?.ready !== 'function') {
      resolve(null);
      return;
    }
    window.grecaptcha.enterprise.ready(async () => {
      try {
        const token = await window.grecaptcha!.enterprise!.execute(siteKey.trim(), { action });
        resolve(token || null);
      } catch {
        resolve(null);
      }
    });
  });
}
