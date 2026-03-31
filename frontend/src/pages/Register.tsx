import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { checkPhoneAvailable, type AuthUser } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';
import { getAuth } from '../firebase';
import { getFirebaseAuthErrorMessage } from '../lib/firebaseAuthErrorMessages';
import { clearRecaptchaVerifier, confirmVerificationCode, getRecaptchaVerifier, sendVerificationCode } from '../lib/phoneAuth';

const RECAPTCHA_CONTAINER_ID = 'register-recaptcha';

export default function Register() {
  const navigate = useNavigate();
  const { refreshUser } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [username, setUsername] = useState('');
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [loginIdChecked, setLoginIdChecked] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [phoneStep, setPhoneStep] = useState<'input' | 'code' | 'verified'>('input');
  const [verificationCode, setVerificationCode] = useState('');
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const confirmationResultRef = useRef<import('firebase/auth').ConfirmationResult | null>(null);
  const recaptchaVerifierRef = useRef<import('firebase/auth').RecaptchaVerifier | null>(null);

  const checkLoginId = async () => {
    const v = loginId.trim();
    if (!v || v.length < 5) {
      setError('아이디는 5자 이상이어야 합니다.');
      return;
    }
    const { ok, data } = await apiFetch<{ available?: boolean }>(`/api/auth/check/loginId?value=${encodeURIComponent(v)}`);
    if (ok && data && (data as { available?: boolean }).available) {
      setLoginIdChecked(true);
      setError('');
    } else {
      setLoginIdChecked(false);
      setError('이미 사용 중인 아이디입니다.');
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
      const auth = await getAuth();
      if (auth) {
        recaptchaVerifierRef.current = await getRecaptchaVerifier(RECAPTCHA_CONTAINER_ID) as import('firebase/auth').RecaptchaVerifier;
      }
      const result = await sendVerificationCode(phone.trim(), recaptchaVerifierRef.current);
      confirmationResultRef.current = result;
      setPhoneStep('code');
    } catch (err: unknown) {
      clearRecaptchaVerifier(RECAPTCHA_CONTAINER_ID, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setSendingCode(false);
    }
  };

  const verifyPhoneCode = async () => {
    const code = verificationCode.trim();
    if (!code) {
      setError('인증번호를 입력해 주세요.');
      return;
    }
    if (!confirmationResultRef.current) {
      setError('인증 요청을 먼저 진행해 주세요.');
      return;
    }
    setError('');
    setVerifyingCode(true);
    try {
      await confirmVerificationCode(confirmationResultRef.current, code);
      const { available } = await checkPhoneAvailable(phone.trim());
      if (!available) {
        setError('이미 가입된 전화번호입니다. 로그인해 주세요.');
        setVerifyingCode(false);
        return;
      }
      setPhoneStep('verified');
      confirmationResultRef.current = null;
    } catch (err: unknown) {
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setVerifyingCode(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (phoneStep !== 'verified') {
      setError('전화번호 인증을 완료해 주세요.');
      return;
    }
    if (password !== password2) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    if (!loginIdChecked) {
      setError('아이디 중복 확인을 해 주세요.');
      return;
    }
    setSubmitting(true);
    try {
      const { ok, data, message } = await apiFetch<AuthUser>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          loginId: loginId.trim(),
          password,
          username: username.trim(),
          nickname: nickname.trim() || undefined,
          email: email.trim(),
          phone: phone.trim(),
        }),
      });
      if (ok && data) {
        await refreshUser();
        navigate('/', { replace: true });
        return;
      }
      setError(message ?? '회원가입에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

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
        <h2 className="modal-title">회원가입</h2>
        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label htmlFor="main-reg-loginId">아이디</label>
            <div className="modal-field-row">
              <input
                id="main-reg-loginId"
                type="text"
                value={loginId}
                onChange={(e) => { setLoginId(e.target.value); setLoginIdChecked(false); }}
                required
                minLength={5}
                placeholder="5자 이상"
              />
              <button type="button" className="btn-check" onClick={checkLoginId}>중복확인</button>
            </div>
            {loginIdChecked && <p className="check-msg ok">사용 가능한 아이디입니다.</p>}
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-password">비밀번호</label>
            <input id="main-reg-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} placeholder="8자 이상" />
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-password2">비밀번호 확인</label>
            <input id="main-reg-password2" type="password" value={password2} onChange={(e) => setPassword2(e.target.value)} required minLength={8} placeholder="비밀번호 재입력" />
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-username">이름</label>
            <input id="main-reg-username" type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-nickname">닉네임</label>
            <input id="main-reg-nickname" type="text" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="표시될 이름" />
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-email">이메일</label>
            <input id="main-reg-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="이메일" />
          </div>
          <div className="modal-field">
            <label htmlFor="main-reg-phone">전화번호</label>
            <div className="modal-field-row">
              <input
                id="main-reg-phone"
                type="tel"
                value={phone}
                onChange={(e) => { setPhone(e.target.value); setPhoneStep('input'); setError(''); }}
                placeholder="010-0000-0000"
                required
                readOnly={phoneStep === 'verified'}
                disabled={phoneStep === 'verified'}
              />
              {phoneStep === 'input' && (
                <button type="button" className="btn-check" onClick={requestPhoneCode} disabled={sendingCode}>
                  {sendingCode ? '발송 중…' : '인증번호 요청'}
                </button>
              )}
            </div>
            {phoneStep === 'code' && (
              <div className="modal-field-row" style={{ marginTop: 8 }}>
                <input
                  type="text"
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value)}
                  placeholder="SMS 인증번호 6자리"
                  maxLength={6}
                  autoComplete="one-time-code"
                />
                <button type="button" className="btn-check" onClick={verifyPhoneCode} disabled={verifyingCode}>
                  {verifyingCode ? '확인 중…' : '인증하기'}
                </button>
              </div>
            )}
            {phoneStep === 'verified' && <p className="check-msg ok">전화번호 인증이 완료되었습니다.</p>}
          </div>
          {error && <p className="modal-error">{error}</p>}
          <div className="modal-actions">
            <button type="submit" className="btn-primary" disabled={submitting || phoneStep !== 'verified'}>
              {submitting ? '가입 중…' : '가입하기'}
            </button>
            <Link to="/login" className="btn-secondary">취소</Link>
          </div>
        </form>
        <p className="recaptcha-disclosure">
          This site is protected by reCAPTCHA and the{' '}
          <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">Google Privacy Policy</a>
          {' '}and{' '}
          <a href="https://policies.google.com/terms" target="_blank" rel="noopener noreferrer">Terms of Service</a> apply.
        </p>
        <p className="modal-footer">이미 계정이 있으신가요? <Link to="/login">로그인</Link></p>
      </div>
    </div>
  );
}
