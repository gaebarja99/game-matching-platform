import type { ApplicationVerifier, Auth, ConfirmationResult } from 'firebase/auth';
import { getAuth } from '../firebase';

const LOCAL_DEV_PHONE_AUTH_CODE = (import.meta.env.VITE_PHONE_AUTH_DEV_CODE || '123456').trim();

function isLocalDevHost(): boolean {
  if (typeof window === 'undefined') return false;
  const host = window.location.hostname;
  return host === 'localhost' || host === '127.0.0.1' || host === '::1';
}

function isRetryableFirebasePhoneAuthError(error: unknown): boolean {
  const message = String((error as { message?: string })?.message ?? error ?? '').toLowerCase();
  return message.includes('auth/invalid-app-credential')
    || message.includes('auth/captcha-check-failed')
    || message.includes('failed to initialize recaptcha enterprise');
}

function shouldUseLocalDevFallback(error: unknown): boolean {
  const enabled = String(import.meta.env.VITE_PHONE_AUTH_DEV_FALLBACK ?? 'true').toLowerCase() !== 'false';
  return enabled && isLocalDevHost() && isRetryableFirebasePhoneAuthError(error);
}

function createLocalDevConfirmationResult(phoneNumber: string): ConfirmationResult {
  return {
    verificationId: `local-dev:${phoneNumber}`,
    confirm: async (code: string) => {
      if ((code ?? '').trim() !== LOCAL_DEV_PHONE_AUTH_CODE) {
        throw new Error('auth/invalid-verification-code');
      }
      return {
        user: {
          uid: `local-dev:${phoneNumber}`,
          phoneNumber,
        },
      } as never;
    },
  } as ConfirmationResult;
}

/** 한국 휴대폰 번호를 E.164 형식으로 변환 (예: 01012345678 → +821012345678) */
export function toE164(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  if (digits.startsWith('82') && digits.length >= 10) return `+${digits}`;
  if (digits.startsWith('0') && digits.length >= 10) return '+82' + digits.slice(1);
  if (digits.length >= 9) return '+82' + digits;
  return '+' + digits;
}

/**
 * 기존 reCAPTCHA 검증기 제거. Firebase는 검증기를 한 번만 사용할 수 있으므로
 * 인증번호를 다시 요청할 때마다 호출한 뒤 새 검증기를 생성해야 합니다.
 */
export function clearRecaptchaVerifier(containerId: string, existingVerifier: unknown): void {
  if (!existingVerifier || typeof existingVerifier !== 'object') return;
  const verifier = existingVerifier as { clear?: () => void };
  if (typeof verifier.clear === 'function') {
    try {
      verifier.clear();
    } catch {
      // 무시
    }
  }
  const container = document.getElementById(containerId);
  if (container) {
    container.innerHTML = '';
  }
}

/**
 * reCAPTCHA 검증기 생성 (invisible). 전화번호 인증 요청 시마다 새로 생성해야 합니다.
 * 이전에 사용한 검증기가 있으면 clearRecaptchaVerifier()로 먼저 제거하세요.
 * 컨테이너는 반드시 DOM에 있고, 최소 크기(1x1 이상)를 가져야 reCAPTCHA가 초기화됩니다.
 */
export async function getRecaptchaVerifier(containerId: string): Promise<ApplicationVerifier> {
  const auth = await getAuth();
  if (!auth) throw new Error('Firebase가 설정되지 않았습니다. .env에 VITE_FIREBASE_* 값을 넣어 주세요.');
  const { RecaptchaVerifier } = await import('firebase/auth');
  const container = document.getElementById(containerId);
  if (!container) throw new Error('reCAPTCHA 컨테이너를 찾을 수 없습니다.');
  // DOM 반영 직후 곧바로 생성하면 간헐적으로 초기화 실패가 나서 한 프레임 대기
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
  return new RecaptchaVerifier(auth, container, {
    size: 'invisible',
    callback: () => {},
    'expired-callback': () => {},
  });
}

/**
 * 휴대폰으로 인증 코드 발송.
 */
export async function sendVerificationCode(phoneNumber: string, recaptchaVerifier: ApplicationVerifier) {
  const auth = await getAuth();
  if (!auth) throw new Error('Firebase가 설정되지 않았습니다.');
  return sendVerificationCodeInner(auth, phoneNumber, recaptchaVerifier);
}

async function sendVerificationCodeInner(auth: Auth, phoneNumber: string, recaptchaVerifier: ApplicationVerifier) {
  const { signInWithPhoneNumber } = await import('firebase/auth');
  const number = phoneNumber.startsWith('+') ? phoneNumber : toE164(phoneNumber);
  try {
    return await signInWithPhoneNumber(auth, number, recaptchaVerifier);
  } catch (error) {
    if (shouldUseLocalDevFallback(error)) {
      console.warn(
        `[phoneAuth] Firebase phone auth failed on local dev host. Falling back to local dev code ${LOCAL_DEV_PHONE_AUTH_CODE}.`,
        error,
      );
      return createLocalDevConfirmationResult(number);
    }
    throw error;
  }
}

/**
 * 사용자가 입력한 인증 코드로 휴대폰 인증 완료.
 */
export async function confirmVerificationCode(confirmationResult: ConfirmationResult, code: string) {
  const { signOut } = await import('firebase/auth');
  const cred = await confirmationResult.confirm(code);
  const auth = await getAuth();
  if (auth) await signOut(auth);
  return cred;
}
