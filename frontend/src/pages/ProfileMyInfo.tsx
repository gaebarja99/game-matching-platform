import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';
import {
  clearRecaptchaVerifier,
  confirmVerificationCode,
  getRecaptchaVerifier,
  sendVerificationCode,
} from '../lib/phoneAuth';
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
  const [newUsername, setNewUsername] = useState('');
  const [nameMsg, setNameMsg] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [pendingUsername, setPendingUsername] = useState<string | null>(null);

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
      setPwMsg({ text: '새 비밀번호가 서로 일치하지 않습니다.', ok: false });
      return;
    }

    fetch(apiUrl('api/auth/change-password'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ currentPassword: currentPw, newPassword: newPw }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          setPwMsg({ text: '비밀번호가 변경되었습니다.', ok: true });
          setCurrentPw('');
          setNewPw('');
          setNewPw2('');
        } else {
          setPwMsg({ text: result.data?.message || '비밀번호 변경에 실패했습니다.', ok: false });
        }
      })
      .catch(() => setPwMsg({ text: '요청 처리에 실패했습니다.', ok: false }));
  };

  const submitUsernameToApi = (name: string) => {
    const formData = new FormData();
    formData.append('username', name);
    return fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: formData })
      .then((response) => {
        if (response.ok) return response.json();
        return response.json().then((data: { message?: string }) => Promise.reject(new Error(data.message)));
      })
      .then(() => {
        refreshUser();
        setNameMsg({ text: '이름이 변경되었습니다.', ok: true });
        setNewUsername('');
      })
      .catch((error) => setNameMsg({ text: error?.message || '변경에 실패했습니다.', ok: false }));
  };

  const submitPhoneToApi = (phone: string) => {
    const formData = new FormData();
    formData.append('phone', phone);
    return fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: formData })
      .then((response) => {
        if (response.ok) return response.json();
        return response.json().then((data: { message?: string }) => Promise.reject(new Error(data.message)));
      })
      .then(() => {
        refreshUser();
        setPhoneMsg({ text: '전화번호가 변경되었습니다.', ok: true });
        setNewPhone('');
      })
      .catch((error) => setPhoneMsg({ text: error?.message || '변경에 실패했습니다.', ok: false }));
  };

  const closeVerifyModal = () => {
    setVerifyModalOpen(false);
    setVerifyCode('');
    setVerifyMsg('');
    setPendingPhone(null);
    setPendingUsername(null);
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
      setVerifyMsg(
        pendingPhone
          ? '휴대전화 번호를 정확히 입력해 주세요.'
          : pendingWithdraw
            ? '계정 탈퇴를 진행하려면 등록된 휴대전화 번호가 필요합니다.'
            : '등록된 휴대전화 번호가 없습니다.',
      );
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
      } catch (firstError) {
        const firstMessage = String((firstError as { message?: string })?.message ?? firstError ?? '');
        const shouldRetry =
          firstMessage.includes('auth/invalid-app-credential') ||
          firstMessage.includes('auth/captcha-check-failed');
        if (!shouldRetry) throw firstError;
        await new Promise((resolve) => setTimeout(resolve, 300));
        await sendOnce();
      }
    } catch (error: unknown) {
      clearRecaptchaVerifier(recaptchaContainerId, recaptchaVerifierRef.current);
      recaptchaVerifierRef.current = null;
      setVerifyMsg(getFirebaseAuthErrorMessage(error));
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

        if (pendingUsername) {
          await submitUsernameToApi(pendingUsername);
          setPendingUsername(null);
        }
        if (pendingPhone) {
          await submitPhoneToApi(pendingPhone);
          setPendingPhone(null);
        }
        if (pendingWithdraw) {
          const password = pendingWithdraw.password;
          setPendingWithdraw(null);
          const result = await fetch(apiUrl('api/auth/withdraw'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ password }),
          }).then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, message: data?.message })));

          if (result.ok) {
            await logout();
            navigate('/', { replace: true });
          } else {
            setWithdrawMsg({ text: result.message || '탈퇴 처리에 실패했습니다.', ok: false });
          }
        }
      } catch (error: unknown) {
        setVerifyMsg(getFirebaseAuthErrorMessage(error));
      } finally {
        setVerifyingCode(false);
      }
      return;
    }

    setVerifyMsg('먼저 인증번호 요청을 진행해 주세요.');
  };

  const handleUsername = () => {
    setNameMsg({ text: '', ok: null });
    const name = newUsername.trim();

    if (!name) {
      setNameMsg({ text: '이름을 입력해 주세요.', ok: false });
      return;
    }

    if (!phoneVerified) {
      setPendingUsername(name);
      setVerifyModalOpen(true);
      return;
    }

    submitUsernameToApi(name);
  };

  const handlePhoneChange = () => {
    setPhoneMsg({ text: '', ok: null });
    const phone = newPhone.trim();

    if (!phone) {
      setPhoneMsg({ text: '새 휴대전화 번호를 입력해 주세요.', ok: false });
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

    const formData = new FormData();
    formData.append('email', email);
    fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: formData })
      .then((response) => {
        if (response.ok) return response.json();
        return response.json().then((data: { message?: string }) => Promise.reject(new Error(data.message)));
      })
      .then(() => {
        refreshUser();
        setEmailMsg({ text: '이메일이 변경되었습니다.', ok: true });
      })
      .catch((error) => setEmailMsg({ text: error?.message || '변경에 실패했습니다.', ok: false }));
  };

  const handleWithdraw = () => {
    if (withdrawConfirm.trim() !== '탈퇴') {
      setWithdrawMsg({ text: '확인 문구에 "탈퇴"를 정확히 입력해 주세요.', ok: false });
      return;
    }

    setWithdrawMsg({ text: '', ok: null });
    const digits = (user?.phone ?? '').replace(/\D/g, '');
    if (digits.length < 10) {
      setWithdrawMsg({
        text: '계정 탈퇴를 진행하려면 등록된 휴대전화 번호가 필요합니다. 내 정보에서 전화번호를 먼저 등록해 주세요.',
        ok: false,
      });
      return;
    }

    setPendingWithdraw({ password: withdrawPassword });
    setVerifyModalOpen(true);
  };

  return (
    <>
      <h1 className="profile-page-title">내 정보</h1>
      <p className="profile-bio" style={{ marginBottom: 24 }}>
        비밀번호, 이름, 전화번호, 이메일을 변경할 수 있습니다. 표시 이름(닉네임)은 프로필 홈의 「프로필 편집」에서 변경할 수 있습니다.
      </p>

      <section className="myinfo-section">
        <h3>비밀번호 변경</h3>
        <div className="myinfo-row">
          <label htmlFor="myinfo-current-pw">현재 비밀번호</label>
          <input
            type="password"
            id="myinfo-current-pw"
            placeholder="현재 비밀번호"
            value={currentPw}
            onChange={(event) => setCurrentPw(event.target.value)}
            autoComplete="current-password"
          />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-pw">새 비밀번호</label>
          <input
            type="password"
            id="myinfo-new-pw"
            placeholder="8자 이상"
            value={newPw}
            onChange={(event) => setNewPw(event.target.value)}
            autoComplete="new-password"
            minLength={8}
          />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-pw2">새 비밀번호 확인</label>
          <input
            type="password"
            id="myinfo-new-pw2"
            placeholder="동일하게 입력"
            value={newPw2}
            onChange={(event) => setNewPw2(event.target.value)}
            autoComplete="new-password"
            minLength={8}
          />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handlePassword}>비밀번호 변경</button>
        </div>
        <p className={`myinfo-msg ${pwMsg.ok === true ? 'ok' : pwMsg.ok === false ? 'err' : ''}`} aria-live="polite">{pwMsg.text}</p>
      </section>

      <section className="myinfo-section">
        <h3>이름 변경</h3>
        {!phoneVerified ? (
          <p className="myinfo-verify-hint">
            이름 변경 시 휴대전화 인증이 필요합니다. 새 이름을 입력한 뒤 이름 변경 버튼을 누르면 인증 창이 열립니다.
          </p>
        ) : null}
        <div className="myinfo-row">
          <label>현재 이름</label>
          <span className="current-value">{user?.username ?? '-'}</span>
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-username">새 이름</label>
          <input
            type="text"
            id="myinfo-new-username"
            placeholder="변경할 이름"
            value={newUsername}
            onChange={(event) => setNewUsername(event.target.value)}
          />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handleUsername}>이름 변경</button>
        </div>
        <p className={`myinfo-msg ${nameMsg.ok === true ? 'ok' : nameMsg.ok === false ? 'err' : ''}`} aria-live="polite">{nameMsg.text}</p>
      </section>

      <section className="myinfo-section">
        <h3>전화번호 변경</h3>
        {!phoneVerified ? (
          <p className="myinfo-verify-hint">
            전화번호 변경 시 휴대전화 인증이 필요합니다. 새 전화번호를 입력한 뒤 변경 버튼을 누르면 인증 창이 열립니다.
          </p>
        ) : null}
        <div className="myinfo-row">
          <label>현재 전화번호</label>
          <span className="current-value">{user?.phone || '등록된 번호 없음'}</span>
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-phone">새 전화번호</label>
          <input
            type="tel"
            id="myinfo-new-phone"
            placeholder="010-0000-0000"
            value={newPhone}
            onChange={(event) => setNewPhone(event.target.value)}
          />
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
          <span className="current-value">{user?.email ?? '-'}</span>
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-new-email">새 이메일</label>
          <input
            type="email"
            id="myinfo-new-email"
            placeholder="변경할 이메일"
            value={newEmail}
            onChange={(event) => setNewEmail(event.target.value)}
          />
        </div>
        <div className="myinfo-actions">
          <button type="button" className="btn-myinfo-save" onClick={handleEmail}>이메일 변경</button>
        </div>
        <p className={`myinfo-msg ${emailMsg.ok === true ? 'ok' : emailMsg.ok === false ? 'err' : ''}`} aria-live="polite">{emailMsg.text}</p>
      </section>

      <section className="myinfo-section myinfo-withdraw-section">
        <h3>계정 탈퇴</h3>
        <p className="myinfo-verify-hint" style={{ marginBottom: 12 }}>
          탈퇴 후 계정은 비활성화되며 복구할 수 없습니다. 비밀번호를 입력하고 확인 문구에 "탈퇴"를 적은 뒤 버튼을 누르면 등록된 휴대전화 인증 후 탈퇴가 완료됩니다.
        </p>
        <div className="myinfo-row">
          <label htmlFor="myinfo-withdraw-pw">비밀번호</label>
          <input
            type="password"
            id="myinfo-withdraw-pw"
            placeholder="일반 계정은 비밀번호 입력"
            value={withdrawPassword}
            onChange={(event) => setWithdrawPassword(event.target.value)}
            autoComplete="current-password"
          />
        </div>
        <div className="myinfo-row">
          <label htmlFor="myinfo-withdraw-confirm">확인 문구 입력</label>
          <input
            type="text"
            id="myinfo-withdraw-confirm"
            placeholder="탈퇴"
            value={withdrawConfirm}
            onChange={(event) => setWithdrawConfirm(event.target.value)}
            autoComplete="off"
          />
        </div>
        <div className="myinfo-actions">
          <button
            type="button"
            className="btn-myinfo-withdraw"
            onClick={handleWithdraw}
            disabled={withdrawConfirm.trim() !== '탈퇴'}
          >
            계정 탈퇴
          </button>
        </div>
        <p className={`myinfo-msg ${withdrawMsg.ok === true ? 'ok' : withdrawMsg.ok === false ? 'err' : ''}`} aria-live="polite">{withdrawMsg.text}</p>
      </section>

      {verifyModalOpen ? (
        <div className="modal-backdrop show" onClick={closeVerifyModal} role="dialog" aria-modal="true">
          <div
            id={recaptchaContainerId}
            aria-hidden="true"
            style={{ position: 'absolute', left: -9999, width: 1, height: 1, minWidth: 1, minHeight: 1, overflow: 'hidden' }}
          />
          <div className="profile-edit-modal" onClick={(event) => event.stopPropagation()}>
            <h2>휴대전화 인증</h2>
            <p className="pang-charge-desc" style={{ marginBottom: 16 }}>
              {pendingWithdraw
                ? '계정 탈퇴를 진행하려면 등록된 휴대전화 번호로 인증번호를 받아야 합니다. 아래 버튼으로 인증번호를 요청해 주세요.'
                : pendingPhone
                  ? '새 휴대전화 번호로 인증번호를 받아야 합니다. 아래 버튼으로 인증번호를 요청해 주세요.'
                  : pendingUsername
                    ? '이름 변경을 완료하려면 등록된 휴대전화 번호로 인증번호를 받아야 합니다. 아래 버튼으로 인증번호를 요청해 주세요.'
                    : '등록된 휴대전화 번호로 인증번호를 받아야 합니다. 아래 버튼으로 인증번호를 요청해 주세요.'}
            </p>
            <div className="profile-edit-field">
              <button
                type="button"
                className="btn-save-profile"
                style={{ marginBottom: 12 }}
                onClick={requestVerifyCode}
                disabled={sendingCode}
              >
                {sendingCode ? '발송 중...' : '인증번호 요청'}
              </button>
            </div>
            <div className="profile-edit-field">
              <label>인증 번호</label>
              <input
                type="text"
                placeholder="SMS 인증번호 6자리"
                value={verifyCode}
                onChange={(event) => setVerifyCode(event.target.value)}
                maxLength={6}
                autoComplete="one-time-code"
              />
            </div>
            {verifyMsg ? <p className="profile-edit-msg err">{verifyMsg}</p> : null}
            <div className="profile-edit-actions" style={{ marginTop: 20 }}>
              <button type="button" className="btn-save-profile" onClick={handleVerifySubmit} disabled={verifyingCode}>
                {verifyingCode ? '확인 중...' : '확인'}
              </button>
              <button type="button" className="btn-cancel-profile" onClick={closeVerifyModal}>취소</button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
