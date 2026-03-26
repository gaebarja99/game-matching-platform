import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';
import { clearRecaptchaVerifier, confirmVerificationCode, getRecaptchaVerifier, sendVerificationCode } from '../lib/phoneAuth';
import { getFirebaseAuthErrorMessage } from '../lib/firebaseAuthErrorMessages';

const PROFILE_VERIFY_RECAPTCHA_ID = 'profile-verify-recaptcha';

export default function ProfileMyInfo() {
  const navigate = useNavigate();
  const { user, refreshUser, logout } = useAuth();
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [newPw2, setNewPw2] = useState('');
  const [pwMsg, setPwMsg] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [newEmail, setNewEmail] = useState('');
  const [emailMsg, setEmailMsg] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [newPhone, setNewPhone] = useState('');
  const [phoneMsg, setPhoneMsg] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [verifyModalOpen, setVerifyModalOpen] = useState(false);
  const [verifyCode, setVerifyCode] = useState('');
  const [verifyMsg, setVerifyMsg] = useState('');
  const [pendingPhone, setPendingPhone] = useState<string | null>(null);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const confirmationResultRef = useRef<import('firebase/auth').ConfirmationResult | null>(null);
  const recaptchaVerifierRef = useRef<import('firebase/auth').RecaptchaVerifier | null>(null);
  const recaptchaSeqRef = useRef(0);
  const [recaptchaContainerId, setRecaptchaContainerId] = useState(`${PROFILE_VERIFY_RECAPTCHA_ID}-0`);
  const [withdrawPassword, setWithdrawPassword] = useState('');
  const [withdrawConfirm, setWithdrawConfirm] = useState('');
  const [withdrawMsg, setWithdrawMsg] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [pendingWithdraw, setPendingWithdraw] = useState<{ password: string } | null>(null);

  const handlePassword = () => {
    setPwMsg({ text: '', ok: null });
    if (!currentPw.trim()) {
      setPwMsg({ text: '현재 비밀번호를 입력해 주세요.', ok: false });
      return;
    }
    if (newPw.length < 8) {
      setPwMsg({ text: '새 비밀번호는 8자 이상이어야 합니다.', ok: false });
      return;
    }
    if (newPw !== newPw2) {
      setPwMsg({ text: '새 비밀번호가 일치하지 않습니다.', ok: false });
      return;
    }
    fetch(apiUrl('api/auth/change-password'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ currentPassword: currentPw, newPassword: newPw }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) {
          setPwMsg({ text: '비밀번호가 변경되었습니다.', ok: true });
          setCurrentPw('');
          setNewPw('');
          setNewPw2('');
        } else {
          setPwMsg({ text: res.data?.message || '비밀번호 변경에 실패했습니다.', ok: false });
        }
      })
      .catch(() => setPwMsg({ text: '요청에 실패했습니다.', ok: false }));
  };

  const submitPhoneToApi = (phone: string) => {
    const fd = new FormData();
    fd.append('phone', phone);
    return fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: fd })
      .then((r) => {
        if (r.ok) return r.json();
        return r.json().then((d: { message?: string }) => Promise.reject(new Error(d.message)));
      })
      .then(() => {
        refreshUser();
        setPhoneMsg({ text: '전화번호가 변경되었습니다.', ok: true });
        setNewPhone('');
      })
      .catch((err) => setPhoneMsg({ text: err?.message || '변경에 실패했습니다.', ok: false }));
  };

  const closeVerifyModal = () => {
    setVerifyModalOpen(false);
    setVerifyCode('');
    setVerifyMsg('');
    setPendingPhone(null);
    setPendingWithdraw(null);
    confirmationResultRef.current = null;
    clearRecaptchaVerifier(recaptchaContainerId, recaptchaVerifierRef.current);
    recaptchaVerifierRef.current = null;
  };

  const resetRecaptcha = async (): Promise<string> => {
    clearRecaptchaVerifier(recaptchaContainerId, recaptchaVerifierRef.current);
    recaptchaVerifierRef.current = null;
    recaptchaSeqRef.current += 1;
    const nextId = `${PROFILE_VERIFY_RECAPTCHA_ID}-${recaptchaSeqRef.current}`;
    setRecaptchaContainerId(nextId);
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    return nextId;
  };

  const requestVerifyCode = async () => {
    const phone = pendingPhone ?? user?.phone ?? '';
    const digits = phone.replace(/\D/g, '');
    if (digits.length < 10) {
      setVerifyMsg(pendingPhone ? '새 전화번호를 정확히 입력해 주세요.' : pendingWithdraw ? '계정 탈퇴를 위해 등록된 전화번호가 필요합니다.' : '등록된 전화번호가 없습니다.');
      return;
    }
    setVerifyMsg('');
    setSendingCode(true);
    try {
      const sendOnce = async () => {
        const containerId = await resetRecaptcha();
        recaptchaVerifierRef.current = await getRecaptchaVerifier(containerId) as import('firebase/auth').RecaptchaVerifier;
        const result = await sendVerificationCode(phone.trim(), recaptchaVerifierRef.current);
        confirmationResultRef.current = result;
      };

      try {
        await sendOnce();
      } catch (firstErr) {
        const firstMsg = String((firstErr as { message?: string })?.message ?? firstErr ?? '');
        const shouldRetry = firstMsg.includes('auth/invalid-app-credential') || firstMsg.includes('auth/captcha-check-failed');
        if (!shouldRetry) throw firstErr;
        await new Promise((resolve) => setTimeout(resolve, 300));
        await sendOnce();
      }
    } catch (err: unknown) {
      clearRecaptchaVerifier(recaptchaContainerId, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      setVerifyMsg(getFirebaseAuthErrorMessage(err));
    } finally {
      setSendingCode(false);
    }
  };

  const handleVerifySubmit = async () => {
    setVerifyMsg('');
    const code = verifyCode.trim();
    if (!code) {
      setVerifyMsg('인증 번호를 입력해 주세요.');
      return;
    }
    if (confirmationResultRef.current) {
      setVerifyingCode(true);
      try {
        await confirmVerificationCode(confirmationResultRef.current, code);
        setPhoneVerified(true);
        setVerifyModalOpen(false);
        setVerifyCode('');
        setVerifyMsg('');
        confirmationResultRef.current = null;
        clearRecaptchaVerifier(recaptchaContainerId, recaptchaVerifierRef.current);
        recaptchaVerifierRef.current = null;
        if (pendingPhone) {
          await submitPhoneToApi(pendingPhone);
          setPendingPhone(null);
        }
        if (pendingWithdraw) {
          const pw = pendingWithdraw.password;
          setPendingWithdraw(null);
          const res = await fetch(apiUrl('api/auth/withdraw'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ password: pw }),
          }).then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, message: d?.message })));
          if (res.ok) {
            await logout();
            navigate('/', { replace: true });
          } else {
            setWithdrawMsg({ text: res.message || '탈퇴에 실패했습니다.', ok: false });
          }
        }
      } catch (err: unknown) {
        setVerifyMsg(getFirebaseAuthErrorMessage(err));
      } finally {
        setVerifyingCode(false);
      }
      return;
    }
    setVerifyMsg('인증번호 요청을 먼저 진행해 주세요.');
  };

  const handlePhoneChange = () => {
    setPhoneMsg({ text: '', ok: null });
    const phone = newPhone.trim();
    if (!phone) {
      setPhoneMsg({ text: '새 전화번호를 입력해 주세요.', ok: false });
      return;
    }
    if (!phoneVerified) {
      setPendingPhone(phone);
      setVerifyModalOpen(true);
      return;
    }
    submitPhoneToApi(phone);
  };

  const handleEmail = () => {
    setEmailMsg({ text: '', ok: null });
    const email = newEmail.trim();
    if (!email) {
      setEmailMsg({ text: '이메일을 입력해 주세요.', ok: false });
      return;
    }
    const fd = new FormData();
    fd.append('email', email);
    fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: fd })
      .then((r) => {
        if (r.ok) return r.json();
        return r.json().then((d: { message?: string }) => Promise.reject(new Error(d.message)));
      })
      .then(() => {
        refreshUser();
        setEmailMsg({ text: '이메일이 변경되었습니다.', ok: true });
      })
      .catch((err) => setEmailMsg({ text: err?.message || '변경에 실패했습니다.', ok: false }));
  };

  const handleWithdraw = () => {
    if (withdrawConfirm.trim() !== '탈퇴') {
      setWithdrawMsg({ text: '확인란에 "탈퇴"를 정확히 입력해 주세요.', ok: false });
      return;
    }
    setWithdrawMsg({ text: '', ok: null });
    const digits = (user?.phone ?? '').replace(/\D/g, '');
    if (digits.length < 10) {
      setWithdrawMsg({ text: '계정 탈퇴를 위해 등록된 전화번호가 필요합니다. 내 정보에 전화번호를 등록해 주세요.', ok: false });
      return;
    }
    setPendingWithdraw({ password: withdrawPassword });
    setVerifyModalOpen(true);
  };

  return (
    <>
      <h1 className="profile-page-title">내 정보</h1>
      <p className="profile-bio" style={{ marginBottom: 24 }}>
        비밀번호, 전화번호, 이메일을 변경할 수 있습니다. 표시 이름(닉네임)은 프로필 홈의 「프로필 편집」에서 변경할 수 있습니다.
      </p>

      <section className="myinfo-section">
        <h3>비밀번호 변경</h3>
        <div className="myinfo-row">
          <label htmlFor="myinfo-current-pw">현재 비밀번호</label>
          <input type="password" id="myinfo-current-pw" placeholder="현재 비밀번호" value={currentPw} onChange={(e) => setCurrentPw(e.target.value)} autoComplete="current-password" />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-pw">새 비밀번호</label>
          <input type="password" id="myinfo-new-pw" placeholder="8자 이상" value={newPw} onChange={(e) => setNewPw(e.target.value)} autoComplete="new-password" minLength={8} />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-pw2">새 비밀번호 확인</label>
          <input type="password" id="myinfo-new-pw2" placeholder="동일하게 입력" value={newPw2} onChange={(e) => setNewPw2(e.target.value)} autoComplete="new-password" minLength={8} />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handlePassword}>비밀번호 변경</button>
        </div>
        <p className={`myinfo-msg ${pwMsg.ok === true ? 'ok' : pwMsg.ok === false ? 'err' : ''}`} aria-live="polite">{pwMsg.text}</p>
      </section>

      <section className="myinfo-section">
        <h3>전화번호 변경</h3>
        {!phoneVerified && (
          <p className="myinfo-verify-hint">전화번호 변경은 휴대전화 인증이 필요합니다. 새 전화번호를 입력한 뒤 "전화번호 변경" 버튼을 누르면 인증 화면이 열립니다.</p>
        )}
        <div className="myinfo-row">
          <label>현재 전화번호</label>
          <span className="current-value">{user?.phone || '등록된 번호 없음'}</span>
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-phone">새 전화번호</label>
          <input type="tel" id="myinfo-new-phone" placeholder="010-0000-0000" value={newPhone} onChange={(e) => setNewPhone(e.target.value)} />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handlePhoneChange}>전화번호 변경</button>
        </div>
        <p className={`myinfo-msg ${phoneMsg.ok === true ? 'ok' : phoneMsg.ok === false ? 'err' : ''}`} aria-live="polite">{phoneMsg.text}</p>
      </section>

      <section className="myinfo-section">
        <h3>이메일 변경</h3>
        <div className="myinfo-row">
          <label>현재 이메일</label>
          <span className="current-value">{user?.email ?? '—'}</span>
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-email">새 이메일</label>
          <input type="email" id="myinfo-new-email" placeholder="변경할 이메일" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handleEmail}>이메일 변경</button>
        </div>
        <p className={`myinfo-msg ${emailMsg.ok === true ? 'ok' : emailMsg.ok === false ? 'err' : ''}`} aria-live="polite">{emailMsg.text}</p>
      </section>

      <section className="myinfo-section myinfo-withdraw-section">
        <h3>계정 탈퇴</h3>
        <p className="myinfo-verify-hint" style={{ marginBottom: 12 }}>
          탈퇴 시 계정이 비활성화되며 복구할 수 없습니다. 비밀번호(소셜 계정은 생략 가능)와 확인란에 "탈퇴"를 입력한 뒤 버튼을 누르면, 등록된 휴대전화로 인증 후 탈퇴가 완료됩니다.
        </p>
        <div className="myinfo-row">
          <label htmlFor="myinfo-withdraw-pw">비밀번호</label>
          <input type="password" id="myinfo-withdraw-pw" placeholder="일반 계정은 비밀번호 입력" value={withdrawPassword} onChange={(e) => setWithdrawPassword(e.target.value)} autoComplete="current-password" />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-withdraw-confirm">확인 (아래에 "탈퇴" 입력)</label>
          <input type="text" id="myinfo-withdraw-confirm" placeholder="탈퇴" value={withdrawConfirm} onChange={(e) => setWithdrawConfirm(e.target.value)} autoComplete="off" />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-withdraw" onClick={handleWithdraw} disabled={withdrawConfirm.trim() !== '탈퇴'}>
            계정 탈퇴
          </button>
        </div>
        <p className={`myinfo-msg ${withdrawMsg.ok === true ? 'ok' : withdrawMsg.ok === false ? 'err' : ''}`} aria-live="polite">{withdrawMsg.text}</p>
      </section>

      {/* 휴대전화 인증 모달 (Firebase SMS 인증) */}
      {verifyModalOpen && (
        <div className="modal-backdrop show" onClick={closeVerifyModal} role="dialog" aria-modal="true">
          <div
            id={recaptchaContainerId}
            aria-hidden="true"
            style={{ position: 'absolute', left: -9999, width: 1, height: 1, minWidth: 1, minHeight: 1, overflow: 'hidden' }}
          />
          <div className="profile-edit-modal" onClick={(e) => e.stopPropagation()}>
            <h2>휴대전화 인증</h2>
            <p className="pang-charge-desc" style={{ marginBottom: 16 }}>
              {pendingWithdraw
                ? '계정 탈퇴를 위해 등록된 전화번호로 인증 번호가 전송됩니다. 아래 "인증번호 요청"을 눌러 주세요.'
                : '새 전화번호로 인증 번호가 전송됩니다. 아래 "인증번호 요청"을 눌러 주세요.'}
            </p>
            <div className="profile-edit-field">
              <button type="button" className="btn-save-profile" style={{ marginBottom: 12 }} onClick={requestVerifyCode} disabled={sendingCode}>
                {sendingCode ? '발송 중…' : '인증번호 요청'}
              </button>
            </div>
            <div className="profile-edit-field">
              <label>인증 번호</label>
              <input type="text" placeholder="SMS 인증번호 6자리" value={verifyCode} onChange={(e) => setVerifyCode(e.target.value)} maxLength={6} autoComplete="one-time-code" />
            </div>
            {verifyMsg && <p className="profile-edit-msg err">{verifyMsg}</p>}
            <div className="profile-edit-actions" style={{ marginTop: 20 }}>
              <button type="button" className="btn-save-profile" onClick={handleVerifySubmit} disabled={verifyingCode}>
                {verifyingCode ? '확인 중…' : '확인'}
              </button>
              <button type="button" className="btn-cancel-profile" onClick={closeVerifyModal}>취소</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
