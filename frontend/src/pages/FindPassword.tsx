import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { checkLoginIdExists, resetPasswordByPhone } from '../api/auth';
import { getFirebaseAuthErrorMessage } from '../lib/firebaseAuthErrorMessages';
import { getRecaptchaEnterpriseToken } from '../lib/recaptchaEnterprise';
import { clearRecaptchaVerifier, confirmVerificationCode, getRecaptchaVerifier, sendVerificationCode } from '../lib/phoneAuth';

const RECAPTCHA_CONTAINER_ID = 'find-password-recaptcha';

export default function FindPassword() {
  const navigate = useNavigate();
  const [loginId, setLoginId] = useState('');
  const [phone, setPhone] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [step, setStep] = useState<'id' | 'verify' | 'password'>('id');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [checkingId, setCheckingId] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const confirmationResultRef = useRef<import('firebase/auth').ConfirmationResult | null>(null);
  const recaptchaVerifierRef = useRef<import('firebase/auth').RecaptchaVerifier | null>(null);

  const handleConfirmLoginId = async (e: React.FormEvent) => {
    e.preventDefault();
    const id = loginId.trim();
    if (!id || id.length < 5) {
      setError('아이디를 5자 이상 입력해 주세요.');
      return;
    }
    setError('');
    setCheckingId(true);
    try {
      const exists = await checkLoginIdExists(id);
      if (exists) {
        setStep('verify');
      } else {
        setError('가입되지 않은 아이디입니다.');
      }
    } finally {
      setCheckingId(false);
    }
  };

  const requestPhoneCode = async () => {
    const p = phone.trim().replace(/\D/g, '');
    if (p.length < 10) {
      setError('전화번호를 정확히 입력해 주세요.');
      return;
    }
    setError('');
    setSendingCode(true);
    try {
      clearRecaptchaVerifier(RECAPTCHA_CONTAINER_ID, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      recaptchaVerifierRef.current = await getRecaptchaVerifier(RECAPTCHA_CONTAINER_ID) as import('firebase/auth').RecaptchaVerifier;
      const result = await sendVerificationCode(phone.trim(), recaptchaVerifierRef.current);
      confirmationResultRef.current = result;
    } catch (err: unknown) {
      clearRecaptchaVerifier(RECAPTCHA_CONTAINER_ID, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setSendingCode(false);
    }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const code = verificationCode.trim();
    if (!code) {
      setError('인증번호를 입력해 주세요.');
      return;
    }
    if (!confirmationResultRef.current) {
      setError('인증번호 요청을 먼저 진행해 주세요.');
      return;
    }
    setVerifyingCode(true);
    try {
      await confirmVerificationCode(confirmationResultRef.current, code);
      confirmationResultRef.current = null;
      setStep('password');
    } catch (err: unknown) {
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setVerifyingCode(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (newPassword.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    if (newPassword !== newPasswordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    setSubmitting(true);
    try {
      const recaptchaToken = await getRecaptchaEnterpriseToken('RESET_PASSWORD');
      const { ok, message } = await resetPasswordByPhone(loginId.trim(), phone.trim(), newPassword, recaptchaToken ?? undefined);
      if (ok) {
        setSuccess(true);
        setTimeout(() => navigate('/login', { replace: true }), 2000);
        return;
      }
      setError(message ?? '비밀번호 변경에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <div className="main-modal-backdrop show">
        <div className="main-modal-box">
          <h2 className="modal-title">비밀번호 변경 완료</h2>
          <p className="modal-desc">비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.</p>
          <div className="modal-actions" style={{ marginTop: 20 }}>
            <Link to="/login" className="btn-primary" style={{ textAlign: 'center', textDecoration: 'none' }}>로그인하기</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="main-modal-backdrop show">
      <div
          id={RECAPTCHA_CONTAINER_ID}
          aria-hidden="true"
          style={{
            position: 'absolute',
            left: -9999,
            width: 1,
            height: 1,
            minWidth: 1,
            minHeight: 1,
            overflow: 'hidden',
          }}
        />
      <div className="main-modal-box">
        <h2 className="modal-title">비밀번호 찾기</h2>
        <p className="modal-desc" style={{ marginBottom: 16 }}>아이디 확인 후 가입 시 등록한 휴대폰 번호로 인증하고 새 비밀번호를 설정하세요.</p>
        {step === 'id' ? (
          <form onSubmit={handleConfirmLoginId}>
            <div className="modal-field">
              <label htmlFor="find-pw-loginId">아이디</label>
              <input
                id="find-pw-loginId"
                type="text"
                value={loginId}
                onChange={(e) => { setLoginId(e.target.value); setError(''); }}
                placeholder="가입한 아이디 입력"
                required
                minLength={5}
                autoComplete="username"
              />
            </div>
            {error && <p className="modal-error">{error}</p>}
            <div className="modal-actions">
              <button type="submit" className="btn-primary" disabled={checkingId}>
                {checkingId ? '확인 중…' : '다음'}
              </button>
              <Link to="/login" className="btn-secondary">취소</Link>
            </div>
          </form>
        ) : step === 'verify' ? (
          <form onSubmit={handleVerify}>
            <div className="modal-field">
              <label htmlFor="find-pw-phone">휴대폰 번호</label>
              <div className="modal-field-row">
                <input
                  id="find-pw-phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="01012345678"
                  required
                  autoComplete="tel"
                />
                <button type="button" className="btn-check" onClick={requestPhoneCode} disabled={sendingCode}>
                  {sendingCode ? '발송 중…' : '인증번호 요청'}
                </button>
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="find-pw-code">인증번호</label>
              <input
                id="find-pw-code"
                type="text"
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value)}
                placeholder="SMS로 받은 인증번호 6자리"
                required
                maxLength={6}
                autoComplete="one-time-code"
              />
            </div>
            {error && <p className="modal-error">{error}</p>}
            <div className="modal-actions">
              <button type="submit" className="btn-primary" disabled={verifyingCode}>
                {verifyingCode ? '인증 중…' : '다음'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => { setStep('id'); setError(''); }}>이전</button>
            </div>
          </form>
        ) : (
          <form onSubmit={handleResetPassword}>
            <div className="modal-field">
              <label htmlFor="find-pw-new">새 비밀번호</label>
              <input
                id="find-pw-new"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="8자 이상"
                required
                minLength={8}
                autoComplete="new-password"
              />
            </div>
            <div className="modal-field">
              <label htmlFor="find-pw-confirm">새 비밀번호 확인</label>
              <input
                id="find-pw-confirm"
                type="password"
                value={newPasswordConfirm}
                onChange={(e) => setNewPasswordConfirm(e.target.value)}
                placeholder="비밀번호 재입력"
                required
                minLength={8}
                autoComplete="new-password"
              />
            </div>
            {error && <p className="modal-error">{error}</p>}
            <div className="modal-actions">
              <button type="submit" className="btn-primary" disabled={submitting}>
                {submitting ? '변경 중…' : '비밀번호 변경'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => { setStep('verify'); setError(''); }}>이전</button>
            </div>
          </form>
        )}
        <p className="recaptcha-disclosure">
          This site is protected by reCAPTCHA and the{' '}
          <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">Google Privacy Policy</a>
          {' '}and{' '}
          <a href="https://policies.google.com/terms" target="_blank" rel="noopener noreferrer">Terms of Service</a> apply.
        </p>
        <p className="modal-footer">
          <Link to="/login">로그인</Link>
          {' · '}
          <Link to="/find-login-id">아이디 찾기</Link>
        </p>
      </div>
    </div>
  );
}
