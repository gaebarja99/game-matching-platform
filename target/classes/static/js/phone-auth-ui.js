/**
 * 로그인 모달 내 휴대폰 인증 UI 연결 (+82, 인증번호 발송, 인증 완료)
 */
import { sendPhoneVerificationCode, confirmPhoneCode, resetRecaptcha } from '/js/firebase-phone-auth.js';

function initPhoneAuthUI() {
  var sendBtn = document.getElementById('phone-auth-send');
  var confirmBtn = document.getElementById('phone-auth-confirm');
  var phoneInput = document.getElementById('phone-auth-number');
  var codeWrap = document.getElementById('phone-auth-code-wrap');
  var codeInput = document.getElementById('phone-auth-code');
  var errEl = document.getElementById('phone-auth-error');
  var modalLogin = document.getElementById('modal-login');

  function showError(msg) {
    if (errEl) errEl.textContent = msg || '';
  }

  function setSendLoading(loading) {
    if (sendBtn) {
      sendBtn.disabled = !!loading;
      sendBtn.textContent = loading ? '발송 중...' : '인증번호 발송';
    }
  }

  function setConfirmLoading(loading) {
    if (confirmBtn) {
      confirmBtn.disabled = !!loading;
      confirmBtn.textContent = loading ? '확인 중...' : '인증 완료';
    }
  }

  if (sendBtn && phoneInput) {
    sendBtn.addEventListener('click', function () {
      var raw = phoneInput.value.trim();
      showError('');
      if (!raw) {
        showError('휴대폰 번호를 입력해 주세요.');
        return;
      }
      setSendLoading(true);
      sendPhoneVerificationCode(raw)
        .then(function () {
          setSendLoading(false);
          if (codeWrap) codeWrap.style.display = 'block';
          if (codeInput) { codeInput.value = ''; codeInput.focus(); }
          showError('');
        })
        .catch(function (err) {
          setSendLoading(false);
          var msg = (err && err.message) || '인증번호 발송에 실패했습니다.';
          if (err && err.code === 'auth/too-many-requests') msg = '요청이 너무 많습니다. 나중에 다시 시도해 주세요.';
          if (err && err.code === 'auth/invalid-phone-number') msg = '올바른 휴대폰 번호를 입력해 주세요.';
          showError(msg);
        });
    });
  }

  if (confirmBtn && codeInput) {
    confirmBtn.addEventListener('click', function () {
      var code = codeInput.value.trim();
      showError('');
      if (!code) {
        showError('인증 코드를 입력해 주세요.');
        return;
      }
      setConfirmLoading(true);
      confirmPhoneCode(code)
        .then(function (result) {
          setConfirmLoading(false);
          showError('');
          if (result && result.user) {
            if (modalLogin) modalLogin.classList.remove('show');
            if (typeof window.onPhoneAuthSuccess === 'function') {
              window.onPhoneAuthSuccess(result.user);
            } else {
              alert('휴대폰 인증이 완료되었습니다. (Firebase UID: ' + (result.user.uid || '') + ')');
            }
          }
        })
        .catch(function (err) {
          setConfirmLoading(false);
          var msg = (err && err.message) || '인증에 실패했습니다.';
          if (err && err.code === 'auth/invalid-verification-code') msg = '잘못된 인증 코드입니다. 다시 확인해 주세요.';
          if (err && err.code === 'auth/code-expired') msg = '인증 코드가 만료되었습니다. 인증번호를 다시 발송해 주세요.';
          showError(msg);
        });
    });
  }

  if (modalLogin) {
    modalLogin.addEventListener('click', function () {});
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initPhoneAuthUI);
} else {
  initPhoneAuthUI();
}
