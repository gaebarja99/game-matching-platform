import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { findLoginIdByPhone } from '../api/auth';
import { getAuth } from '../firebase';
import { getFirebaseAuthErrorMessage } from '../lib/firebaseAuthErrorMessages';
import { getRecaptchaEnterpriseToken } from '../lib/recaptchaEnterprise';
import {
  clearRecaptchaVerifier,
  confirmVerificationCode,
  getRecaptchaVerifier,
  isPhoneAuthDevBypassEnabled,
  sendVerificationCode,
} from '../lib/phoneAuth';

const RECAPTCHA_CONTAINER_ID = 'find-login-id-recaptcha';
const DEV_PHONE_AUTH_CODE = (import.meta.env.VITE_PHONE_AUTH_DEV_CODE || '123456').trim();

export default function FindLoginId() {
  const [phone, setPhone] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [error, setError] = useState('');
  const [loginIds, setLoginIds] = useState<string[] | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [codeRequested, setCodeRequested] = useState(false);
  const confirmationResultRef = useRef<import('firebase/auth').ConfirmationResult | null>(null);
  const recaptchaVerifierRef = useRef<import('firebase/auth').RecaptchaVerifier | null>(null);

  const requestPhoneCode = async () => {
    const p = phone.trim().replace(/\D/g, '');
    if (p.length < 10) {
      setError('전화번호를 정확히 입력해 주세요.');
      return;
    }
    setError('');
    setSendingCode(true);
    try {
      // reCAPTCHA 검증기는 한 번만 사용 가능. 매 요청마다 제거 후 새로 생성 (Firebase 있을 때만).
      clearRecaptchaVerifier(RECAPTCHA_CONTAINER_ID, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      const auth = await getAuth();
      if (auth) {
        recaptchaVerifierRef.current = await getRecaptchaVerifier(RECAPTCHA_CONTAINER_ID) as import('firebase/auth').RecaptchaVerifier;
      }
      const result = await sendVerificationCode(phone.trim(), recaptchaVerifierRef.current);
      confirmationResultRef.current = result;
      setPhoneVerified(false);
      setCodeRequested(true);
    } catch (err: unknown) {
      clearRecaptchaVerifier(RECAPTCHA_CONTAINER_ID, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setSendingCode(false);
    }
  };

  const verifyCodeOnly = async () => {
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
      setPhoneVerified(true);
      confirmationResultRef.current = null;
    } catch (err: unknown) {
      setError(getFirebaseAuthErrorMessage(err));
    } finally {
      setVerifyingCode(false);
    }
  };

  const verifyAndFindId = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoginIds(null);
    const code = verificationCode.trim();
    if (!code) {
      setError('인증번호를 입력해 주세요.');
      return;
    }
    // 같은 핸들러 안에서는 setPhoneVerified 직후에도 phoneVerified 값이 바로 바뀌지 않으므로 로컬 플래그 사용
    let verified = phoneVerified;
    if (!confirmationResultRef.current && !verified) {
      setError('인증번호 요청 후 인증을 완료해 주세요. (확인 버튼 또는 아래 아이디 찾기)');
      return;
    }
    if (!verified && confirmationResultRef.current) {
      setVerifyingCode(true);
      try {
        await confirmVerificationCode(confirmationResultRef.current, code);
        verified = true;
        setPhoneVerified(true);
        confirmationResultRef.current = null;
      } catch (err: unknown) {
        setError(getFirebaseAuthErrorMessage(err));
        setVerifyingCode(false);
        return;
      }
      setVerifyingCode(false);
    }
    if (!verified) return;
    setSubmitting(true);
    try {
      const recaptchaToken = await getRecaptchaEnterpriseToken('FIND_LOGIN_ID');
      const { ok, loginIds: ids, message } = await findLoginIdByPhone(phone.trim(), recaptchaToken ?? undefined);
      if (ok && ids && ids.length > 0) {
        setLoginIds(ids);
        return;
      }
      setError(message ?? '해당 휴대폰 번호로 가입된 계정이 없습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="main-modal-backdrop show">
      {/* reCAPTCHA는 최소 크기가 있어야 초기화됨. 화면 밖에 배치 */}
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
        <h2 className="modal-title">아이디 찾기</h2>
        <p className="modal-desc" style={{ marginBottom: 16 }}>가입 시 등록한 휴대폰 번호로 인증 후 아이디를 조회합니다.</p>
        {import.meta.env.DEV && isPhoneAuthDevBypassEnabled() ? (
          <p className="modal-desc" style={{ marginBottom: 12, fontSize: '0.85rem', opacity: 0.9 }}>
            로컬 개발: Firebase 미설정 시 SMS 대신 고정 인증번호(<strong>{DEV_PHONE_AUTH_CODE}</strong>)로 테스트할 수 있습니다.
          </p>
        ) : null}
        {loginIds === null ? (
          <form onSubmit={verifyAndFindId}>
            <div className="modal-field">
              <label htmlFor="find-id-phone">휴대폰 번호</label>
              <div className="modal-field-row">
                <input
                  id="find-id-phone"
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
              <label htmlFor="find-id-code">인증번호</label>
              <div className="modal-field-row">
                <input
                  id="find-id-code"
                  type="text"
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value)}
                  placeholder="SMS로 받은 인증번호 6자리"
                  required
                  maxLength={6}
                  autoComplete="one-time-code"
                />
                {codeRequested && !phoneVerified && (
                  <button
                    type="button"
                    className="btn-check"
                    onClick={verifyCodeOnly}
                    disabled={verifyingCode || verificationCode.trim().length < 6}
                  >
                    {verifyingCode ? '확인 중…' : '확인'}
                  </button>
                )}
              </div>
            </div>
            {error && <p className="modal-error">{error}</p>}
            <div className="modal-actions">
              <button type="submit" className="btn-primary" disabled={submitting || (codeRequested && !phoneVerified)}>
                {submitting ? '확인 중…' : '아이디 찾기'}
              </button>
              <Link to="/login" className="btn-secondary">로그인으로</Link>
            </div>
          </form>
        ) : (
          <div className="find-result">
            <p className="find-result-title">가입된 아이디</p>
            <ul className="find-result-list">
              {loginIds.map((id) => (
                <li key={id}>{id}</li>
              ))}
            </ul>
            <div className="modal-actions" style={{ marginTop: 20 }}>
              <Link to="/login" className="btn-primary" style={{ textAlign: 'center', textDecoration: 'none' }}>로그인하기</Link>
            </div>
          </div>
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
          <Link to="/find-password">비밀번호 찾기</Link>
        </p>
      </div>
    </div>
  );
}
