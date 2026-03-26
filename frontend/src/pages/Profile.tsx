import { useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { checkNicknameAvailable, repairMojibakeText } from '../api/auth';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';

function formatActivityPeriod(createdAt?: string | null): string {
  if (!createdAt) return '-';

  const joinedAt = new Date(createdAt);
  const now = new Date();
  const months = (now.getFullYear() - joinedAt.getFullYear()) * 12 + (now.getMonth() - joinedAt.getMonth());

  if (months >= 12) {
    const years = Math.floor(months / 12);
    const remainMonths = months % 12;
    return remainMonths > 0 ? `${years}년 ${remainMonths}개월` : `${years}년`;
  }

  if (months >= 1) return `${months}개월`;

  const days = Math.max(0, Math.floor((now.getTime() - joinedAt.getTime()) / (24 * 60 * 60 * 1000)));
  return `${days}일`;
}

export default function Profile() {
  const navigate = useNavigate();
  const { user, refreshUser } = useAuth();
  const [editOpen, setEditOpen] = useState(false);
  const [editNickname, setEditNickname] = useState('');
  const [editBio, setEditBio] = useState('');
  const [profileImageFile, setProfileImageFile] = useState<File | null>(null);
  const [profileImagePreview, setProfileImagePreview] = useState<string | null>(null);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [checkedNicknameSnapshot, setCheckedNicknameSnapshot] = useState<string | null>(null);
  const [nicknameMsg, setNicknameMsg] = useState('');
  const [nicknameCheckLoading, setNicknameCheckLoading] = useState(false);
  const [editMsg, setEditMsg] = useState('');
  const [saveLoading, setSaveLoading] = useState(false);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  const originalNickname = (user?.nickname ?? user?.username ?? '').trim();
  const canSave = useMemo(() => {
    const currentNick = editNickname.trim();
    const nickChanged = originalNickname !== currentNick;
    const nicknameOk =
      !nickChanged || (nicknameChecked && checkedNicknameSnapshot !== null && checkedNicknameSnapshot === currentNick);
    const bioChanged = editBio.trim() !== (user?.bio ?? '').trim();
    const hasImage = profileImageFile !== null;
    const hasChanges = nickChanged || bioChanged || hasImage;
    return nicknameOk && hasChanges;
  }, [
    editNickname,
    editBio,
    profileImageFile,
    originalNickname,
    user?.bio,
    nicknameChecked,
    checkedNicknameSnapshot,
  ]);

  const pangBalance = user?.pangBalance ?? 0;
  const displayName =
    repairMojibakeText(user?.nickname) ??
    repairMojibakeText(user?.username) ??
    repairMojibakeText(user?.loginId) ??
    '사용자';
  const displayBio = repairMojibakeText(user?.bio)?.trim() || '간단한 자기소개를 작성해보세요.';

  const handleOpenEdit = () => {
    setEditNickname(user?.nickname ?? user?.username ?? '');
    setEditBio(user?.bio ?? '');
    setProfileImageFile(null);
    setProfileImagePreview(null);
    setNicknameChecked(false);
    setCheckedNicknameSnapshot(null);
    setNicknameMsg('');
    setEditMsg('');
    setEditOpen(true);
  };

  const handleProfileImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !file.type.startsWith('image/')) return;

    setProfileImageFile(file);
    const reader = new FileReader();
    reader.onload = () => setProfileImagePreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const checkNickname = () => {
    const value = editNickname.trim();
    setNicknameMsg('');

    if (!value) {
      setNicknameMsg('닉네임을 입력한 뒤 중복 확인을 진행해 주세요.');
      setNicknameChecked(false);
      setCheckedNicknameSnapshot(null);
      return;
    }

    setNicknameCheckLoading(true);
    checkNicknameAvailable(value)
      .then(({ ok, available }) => {
        if (!ok) {
          setNicknameChecked(false);
          setCheckedNicknameSnapshot(null);
          setNicknameMsg('닉네임 확인에 실패했습니다.');
          return;
        }

        if (available) {
          setNicknameChecked(true);
          setCheckedNicknameSnapshot(value);
          setNicknameMsg('사용 가능한 닉네임입니다.');
        } else {
          setNicknameChecked(false);
          setCheckedNicknameSnapshot(null);
          setNicknameMsg('이미 사용 중인 닉네임입니다.');
        }
      })
      .catch(() => {
        setNicknameChecked(false);
        setCheckedNicknameSnapshot(null);
        setNicknameMsg('닉네임 확인에 실패했습니다.');
      })
      .finally(() => setNicknameCheckLoading(false));
  };

  const handleSaveProfile = (event: React.FormEvent) => {
    event.preventDefault();
    if (!canSave || saveLoading) return;

    setEditMsg('');
    const nickname = editNickname.trim();
    const sameNickname = originalNickname === nickname;

    if (!sameNickname && !(nicknameChecked && checkedNicknameSnapshot === nickname)) {
      setEditMsg('닉네임을 변경하려면 중복 확인이 필요합니다.');
      return;
    }

    setSaveLoading(true);
    const formData = new FormData();
    formData.append('nickname', nickname);
    formData.append('bio', editBio.trim());
    if (profileImageFile) formData.append('profileImage', profileImageFile);

    fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: formData })
      .then((response) => {
        if (response.ok) return response.json();
        return response.json().then((data: { message?: string }) => Promise.reject(new Error(data.message)));
      })
      .then(() => {
        void refreshUser();
        setEditOpen(false);
        navigate('/profile', { replace: true });
      })
      .catch((error) => setEditMsg(error?.message ?? '프로필 저장에 실패했습니다.'))
      .finally(() => setSaveLoading(false));
  };

  return (
    <>
      <h1 className="profile-page-title">GameMatcher 프로필</h1>

      <div className="profile-header">
        <div className="profile-avatar-wrap">
          {resolveProfileImageUrl(user?.profileImageUrl) ? (
            <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" />
          ) : (
            <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={64} height={64}>
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          )}
        </div>

        <div className="profile-info">
          <div className="profile-nickname">{displayName}</div>
          <p className="profile-bio">{displayBio}</p>
          <button type="button" className="profile-edit-btn" onClick={handleOpenEdit}>프로필 편집</button>
        </div>
      </div>

      <div className="profile-stats">
        <div className="profile-stat-card">
          <div className="label">레벨</div>
          <div className="value">LV {user?.level ?? 1}</div>
          <div className="level-bar">
            <div
              className="level-bar-fill"
              style={{
                width: `${Math.min(
                  100,
                  Math.max(
                    0,
                    ((user?.experienceInCurrentLevelTenths ?? 0) /
                      Math.max(1, user?.experienceRequiredForNextLevelTenths ?? 10000)) *
                      100,
                  ),
                )}%`,
              }}
            />
          </div>
          <div className="level-exp-text">
            {Math.floor((user?.experienceInCurrentLevelTenths ?? 0) / 10).toLocaleString()} /{' '}
            {Math.floor((user?.experienceRequiredForNextLevelTenths ?? 10000) / 10).toLocaleString()} 경험치
          </div>
        </div>

        <div className="profile-stat-card">
          <div className="label">활동 기간</div>
          <div className="value">{formatActivityPeriod(user?.createdAt)}</div>
        </div>

        <div className="profile-stat-card" id="profile-pang-card">
          <div className="label">보유 중인 팡</div>
          <div className="value">{Number(pangBalance).toLocaleString()}</div>
          <div className="sub">충전한 금액까지 포함된 현재 사용 가능한 잔액입니다.</div>
        </div>

        <div className="profile-stat-card">
          <div className="label">마일리지</div>
          <div className="value mileage-value">{(user?.mileage ?? 0).toLocaleString()}점</div>
          <div className="sub">결제 금액의 5%가 적립됩니다.</div>
        </div>
      </div>

      <div
        className={`modal-backdrop ${editOpen ? 'show' : ''}`}
        onClick={() => setEditOpen(false)}
        role="dialog"
        aria-modal="true"
      >
        <div className="profile-edit-modal" onClick={(event) => event.stopPropagation()}>
          <h2>프로필 편집</h2>
          <form onSubmit={handleSaveProfile}>
            <div className="profile-edit-field profile-edit-avatar-row">
              <label>프로필 사진</label>
              <div className="profile-edit-avatar-wrap">
                <div className="profile-edit-avatar-preview">
                  {profileImagePreview ? (
                    <img src={profileImagePreview} alt="" />
                  ) : resolveProfileImageUrl(user?.profileImageUrl) ? (
                    <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" />
                  ) : (
                    <svg viewBox="0 0 24 24" fill="currentColor" width={48} height={48}>
                      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                    </svg>
                  )}
                </div>
                <input
                  ref={profileImageInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  onChange={handleProfileImageChange}
                  style={{ display: 'none' }}
                />
                <button
                  type="button"
                  className="btn-select-photo"
                  onClick={() => profileImageInputRef.current?.click()}
                >
                  사진 선택
                </button>
              </div>
            </div>

            <div className="profile-edit-field">
              <label>닉네임 (중복 확인 필요)</label>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 6 }}>
                <input
                  type="text"
                  value={editNickname}
                  onChange={(event) => {
                    setEditNickname(event.target.value);
                    setNicknameChecked(false);
                    setCheckedNicknameSnapshot(null);
                  }}
                  placeholder="닉네임"
                  maxLength={50}
                  style={{ flex: 1, minWidth: 0 }}
                />
                <button
                  type="button"
                  onClick={checkNickname}
                  disabled={nicknameCheckLoading}
                  style={{
                    padding: '10px 14px',
                    borderRadius: 8,
                    border: '1px solid #00e676',
                    background: 'transparent',
                    color: '#00e676',
                    fontSize: '0.9rem',
                    fontWeight: 500,
                    cursor: nicknameCheckLoading ? 'wait' : 'pointer',
                    whiteSpace: 'nowrap',
                    opacity: nicknameCheckLoading ? 0.7 : 1,
                  }}
                >
                  {nicknameCheckLoading ? '확인 중...' : '중복 확인'}
                </button>
              </div>
              <p className={`profile-edit-msg ${nicknameMsg.includes('가능') ? 'ok' : nicknameMsg ? 'err' : ''}`}>
                {nicknameMsg}
              </p>
            </div>

            <div className="profile-edit-field">
              <label>자기소개</label>
              <textarea
                value={editBio}
                onChange={(event) => setEditBio(event.target.value)}
                placeholder="간단한 자기소개를 입력해 주세요."
                rows={3}
                maxLength={500}
                style={{
                  width: '100%',
                  resize: 'vertical',
                  minHeight: 72,
                  padding: 10,
                  borderRadius: 8,
                  border: '1px solid var(--studio-border-strong, #d8d8dc)',
                  fontSize: '0.95rem',
                  fontFamily: 'inherit',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div className="profile-edit-actions">
              <button type="submit" className="btn-save-profile" disabled={!canSave || saveLoading}>
                {saveLoading ? '저장 중...' : '저장'}
              </button>
              <button type="button" className="btn-cancel-profile" onClick={() => setEditOpen(false)}>
                취소
              </button>
            </div>

            {editMsg ? <p className="profile-edit-msg err" style={{ marginTop: 8 }}>{editMsg}</p> : null}
          </form>
        </div>
      </div>
    </>
  );
}
