/**
 * 보이지 않는 reCAPTCHA (Firebase Auth 전화 인증 등에 사용)
 * https://firebase.google.com/docs/auth/web/phone-auth#web-version-9_2
 */
import { getAuth, RecaptchaVerifier } from 'https://www.gstatic.com/firebasejs/10.7.0/firebase-auth.js';
import { app } from '/js/firebase-init.js';

const auth = getAuth(app);

/**
 * 보이지 않는 RecaptchaVerifier 초기화.
 * @param {string} containerId - reCAPTCHA가 바인딩될 요소의 id (예: 'sign-in-button')
 * @param {() => void} [callback] - reCAPTCHA 해결 후 호출할 콜백 (예: signInWithPhoneNumber 진행)
 */
export function initRecaptchaVerifier(containerId, callback) {
  const container = typeof containerId === 'string' ? document.getElementById(containerId) : containerId;
  if (!container) {
    console.warn('[Firebase reCAPTCHA] 컨테이너 요소를 찾을 수 없습니다:', containerId);
    return null;
  }
  if (window.recaptchaVerifier) {
    try { window.recaptchaVerifier.clear(); } catch (e) {}
    window.recaptchaVerifier = null;
  }
  container.innerHTML = '';
  window.recaptchaVerifier = new RecaptchaVerifier(auth, container, {
    size: 'invisible',
    callback: function (response) {
      if (typeof callback === 'function') callback(response);
      if (typeof window.onRecaptchaSuccess === 'function') window.onRecaptchaSuccess(response);
    }
  });
  return window.recaptchaVerifier;
}

// 로그인 모달 등에 쓰일 기본 컨테이너(id="sign-in-button")가 있으면 자동 초기화
function tryAutoInit() {
  var el = document.getElementById('sign-in-button');
  if (el && !window.recaptchaVerifier) {
    initRecaptchaVerifier('sign-in-button', null);
  }
}
if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', tryAutoInit);
  } else {
    tryAutoInit();
  }
}
