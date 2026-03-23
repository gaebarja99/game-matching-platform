/**
 * Firebase 휴대폰 인증: 인증 코드 발송 및 코드 확인
 * +82(한국) 형식 지원, 오류 시 reCAPTCHA 재설정
 */
import { getAuth, signInWithPhoneNumber } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-auth.js';
import { app } from '/js/firebase-init.js';
import { initRecaptchaVerifier } from '/js/firebase-recaptcha.js';

const auth = getAuth(app);

const CONTAINER_ID = 'sign-in-button';

/**
 * 입력값을 E.164 형식으로 정규화 (한국 +82).
 * 예: "010-1234-5678" -> "+821012345678", "010 1234 5678" -> "+821012345678"
 */
export function normalizePhoneNumber(input) {
  if (!input || typeof input !== 'string') return '';
  const digits = input.replace(/\D/g, '');
  if (digits.length === 9 && digits.startsWith('10')) return '+82' + digits;      // 010...
  if (digits.length === 10 && digits.startsWith('010')) return '+82' + digits.slice(1);
  if (digits.length === 11 && digits.startsWith('82')) return '+' + digits;
  if (digits.length === 10 && digits.startsWith('10')) return '+82' + digits;
  if (digits.startsWith('82')) return '+' + digits;
  if (digits.startsWith('0')) return '+82' + digits.slice(1);
  return '+82' + digits;
}

/**
 * reCAPTCHA 재설정 (오류 시 재시도 가능하도록).
 * clear 후 새 RecaptchaVerifier 생성.
 */
export function resetRecaptcha() {
  try {
    if (window.recaptchaVerifier) {
      window.recaptchaVerifier.clear();
      window.recaptchaVerifier = null;
    }
  } catch (e) {}
  var container = document.getElementById(CONTAINER_ID);
  if (container) container.innerHTML = '';
  initRecaptchaVerifier(CONTAINER_ID, null);
}

/**
 * 휴대폰 번호로 인증 코드(SMS) 발송.
 * @param {string} phoneNumber - E.164 형식 (예: +821012345678). normalizePhoneNumber 사용 권장.
 * @returns {Promise<import('firebase/auth').ConfirmationResult>}
 */
export function sendPhoneVerificationCode(phoneNumber) {
  const normalized = phoneNumber.startsWith('+') ? phoneNumber : normalizePhoneNumber(phoneNumber);
  if (!normalized || normalized.length < 10) {
    return Promise.reject(new Error('올바른 휴대폰 번호를 입력해 주세요.'));
  }
  if (!window.recaptchaVerifier) {
    initRecaptchaVerifier(CONTAINER_ID, null);
  }
  return signInWithPhoneNumber(auth, normalized, window.recaptchaVerifier).then(function (confirmationResult) {
    window.confirmationResult = confirmationResult;
    return confirmationResult;
  }).catch(function (error) {
    resetRecaptcha();
    return Promise.reject(error);
  });
}

/**
 * 사용자가 입력한 인증 코드로 로그인 완료.
 * @param {string} code - SMS로 받은 6자리 코드
 * @returns {Promise<import('firebase/auth').UserCredential>}
 */
export function confirmPhoneCode(code) {
  const c = (code || '').trim().replace(/\s/g, '');
  if (!c) return Promise.reject(new Error('인증 코드를 입력해 주세요.'));
  if (!window.confirmationResult) return Promise.reject(new Error('먼저 인증번호 발송을 진행해 주세요.'));
  return window.confirmationResult.confirm(c);
}

// 전역 노출 (다른 스크립트에서 호출 가능)
if (typeof window !== 'undefined') {
  window.firebasePhoneAuth = {
    normalizePhoneNumber: normalizePhoneNumber,
    sendPhoneVerificationCode: sendPhoneVerificationCode,
    confirmPhoneCode: confirmPhoneCode,
    resetRecaptcha: resetRecaptcha
  };
}
