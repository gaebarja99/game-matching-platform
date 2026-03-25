import { useState, useRef, useMemo, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { checkNicknameAvailable } from '../api/auth';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { fetchProfile, patchProfile, type ProfileDto } from '../api/profile';
import {
  PREFERRED_GAME_OPTIONS,
  parsePreferredGamesToSelected,
  serializePreferredGames,
} from '../constants/games';
import { effectiveCustomProfileUrl } from '../constants/profile';

function resolveBannerStyleUrl(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  const t = url.trim();
  if (t.startsWith('http://') || t.startsWith('https://')) return t;
  return apiUrl(t.replace(/^\//, ''));
}

function computeExtDirty(
  publicProfile: ProfileDto | null,
  banner: string,
  profileUrl: string,
  selectedGames: string[]
): boolean {
  const b0 = (publicProfile?.bannerImageUrl ?? '').trim();
  const b1 = banner.trim();
  const p0 = effectiveCustomProfileUrl(publicProfile?.profileImageUrl ?? null).trim();
  const p1 = profileUrl.trim();
  const g0 = serializePreferredGames(parsePreferredGamesToSelected(publicProfile?.preferredGames)) ?? '';
  const g1 = serializePreferredGames(selectedGames) ?? '';
  return b0 !== b1 || p0 !== p1 || g0 !== g1;
}

function formatActivityPeriod(createdAt?: string | null): string {
  if (!createdAt) return '—';
  const d = new Date(createdAt);
  const now = new Date();
  const months = (now.getFullYear() - d.getFullYear()) * 12 + (now.getMonth() - d.getMonth());
  if (months >= 12) {
    const years = Math.floor(months / 12);
    return `${years}년 ${months % 12}개월 +`;
  }
  if (months >= 1) return `${months}개월 +`;
  const days = Math.max(0, Math.floor((now.getTime() - d.getTime()) / (24 * 60 * 60 * 1000)));
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
  /** 중복확인 통과 시점의 닉네임(입력이 바뀌면 무효) */
  const [checkedNicknameSnapshot, setCheckedNicknameSnapshot] = useState<string | null>(null);
  const [nicknameMsg, setNicknameMsg] = useState('');
  const [nicknameCheckLoading, setNicknameCheckLoading] = useState(false);
  const [editMsg, setEditMsg] = useState('');
  const [saveLoading, setSaveLoading] = useState(false);
  const profileImageInputRef = useRef<HTMLInputElement>(null);

  const [publicProfile, setPublicProfile] = useState<ProfileDto | null>(null);
  const [publicLoading, setPublicLoading] = useState(false);
  const [publicErr, setPublicErr] = useState('');
  const [fBannerUrl, setFBannerUrl] = useState('');
  const [fProfileUrl, setFProfileUrl] = useState('');
  const [selectedGames, setSelectedGames] = useState<string[]>([]);
  const [gamesModalOpen, setGamesModalOpen] = useState(false);
  const [modalGameDraft, setModalGameDraft] = useState<string[]>([]);

  const loadPublicProfile = useCallback(async () => {
    if (!user?.id) return;
    setPublicLoading(true);
    setPublicErr('');
    try {
      const p = await fetchProfile(user.id);
      setPublicProfile(p);
    } catch {
      setPublicErr('공개 프로필 정보를 불러오지 못했습니다.');
      setPublicProfile(null);
    } finally {
      setPublicLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    void loadPublicProfile();
  }, [loadPublicProfile]);

  useEffect(() => {
    if (!editOpen) setGamesModalOpen(false);
  }, [editOpen]);

  useEffect(() => {
    if (!gamesModalOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setGamesModalOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [gamesModalOpen]);

  const originalNickname = (user?.nickname ?? user?.username ?? '').trim();
  const canSave = useMemo(() => {
    const currentNick = editNickname.trim();
    const nickChanged = originalNickname !== currentNick;
    const nicknameOk =
      !nickChanged || (nicknameChecked && checkedNicknameSnapshot !== null && checkedNicknameSnapshot === currentNick);
    const bioChanged = editBio.trim() !== (user?.bio ?? '').trim();
    const hasImage = profileImageFile !== null;
    const mainDirty = nickChanged || bioChanged || hasImage;
    const extDirty = computeExtDirty(publicProfile, fBannerUrl, fProfileUrl, selectedGames);
    const hasChanges = mainDirty || extDirty;
    return nicknameOk && hasChanges;
  }, [
    editNickname,
    editBio,
    profileImageFile,
    originalNickname,
    user?.bio,
    nicknameChecked,
    checkedNicknameSnapshot,
    publicProfile,
    fBannerUrl,
    fProfileUrl,
    selectedGames,
  ]);

  const pangBalance = user?.pangBalance ?? 0;
  const bannerDisplaySrc = publicProfile ? resolveBannerStyleUrl(publicProfile.bannerImageUrl) : null;
  const gameTagsDisplay = parsePreferredGamesToSelected(publicProfile?.preferredGames);
  const gamesPreview = (
    <>
      <div className="phe-section-label">선호 게임</div>
      <div className="phe-games-row">
        {gameTagsDisplay.length === 0 ? (
          <span className="phe-games-empty">선택된 게임이 없습니다. 프로필 편집에서 추가해 보세요.</span>
        ) : (
          gameTagsDisplay.map((g) => (
            <span key={g} className="phe-game-pill">
              {g}
            </span>
          ))
        )}
      </div>
    </>
  );

  const handleOpenEdit = async () => {
    setEditNickname(user?.nickname ?? user?.username ?? '');
    setEditBio(user?.bio ?? '');
    setProfileImageFile(null);
    setProfileImagePreview(null);
    setNicknameChecked(false);
    setCheckedNicknameSnapshot(null);
    setNicknameMsg('');
    setEditMsg('');
    let p: ProfileDto | null = publicProfile;
    if (user?.id) {
      try {
        p = await fetchProfile(user.id);
        setPublicProfile(p);
      } catch {
        p = publicProfile;
      }
    }
    if (p) {
      setFBannerUrl(p.bannerImageUrl ?? '');
      setFProfileUrl(effectiveCustomProfileUrl(p.profileImageUrl));
      setSelectedGames(parsePreferredGamesToSelected(p.preferredGames));
    } else {
      setFBannerUrl('');
      setFProfileUrl('');
      setSelectedGames([]);
    }
    setEditOpen(true);
  };

  const handleProfileImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
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
      setNicknameMsg('닉네임을 입력한 뒤 중복확인해 주세요.');
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
          setNicknameMsg('확인에 실패했습니다.');
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
        setNicknameMsg('확인에 실패했습니다.');
      })
      .finally(() => setNicknameCheckLoading(false));
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSave || saveLoading) return;
    setEditMsg('');
    const nickname = editNickname.trim();
    const sameNickname = originalNickname === nickname;
    if (!sameNickname && !(nicknameChecked && checkedNicknameSnapshot === nickname)) {
      setEditMsg('닉네임 변경 시 중복확인을 해주세요.');
      return;
    }
    const nickChanged = originalNickname !== nickname;
    const bioChanged = editBio.trim() !== (user?.bio ?? '').trim();
    const hasImage = profileImageFile !== null;
    const mainDirty = nickChanged || bioChanged || hasImage;
    const extDirty = computeExtDirty(publicProfile, fBannerUrl, fProfileUrl, selectedGames);

    setSaveLoading(true);
    try {
      if (mainDirty) {
        const fd = new FormData();
        fd.append('nickname', nickname);
        fd.append('bio', editBio.trim());
        if (profileImageFile) fd.append('profileImage', profileImageFile);
        const r = await fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: fd });
        if (!r.ok) {
          const d = (await r.json().catch(() => ({}))) as { message?: string };
          throw new Error(d.message ?? '저장에 실패했습니다.');
        }
      }
      if (extDirty && user?.id) {
        const next = await patchProfile(user.id, {
          profileImageUrl: fProfileUrl.trim() === '' ? null : fProfileUrl.trim(),
          bannerImageUrl: fBannerUrl.trim() === '' ? null : fBannerUrl.trim(),
          preferredGames: serializePreferredGames(selectedGames),
        });
        setPublicProfile(next);
      }
      await refreshUser();
      await loadPublicProfile();
      setEditOpen(false);
      navigate('/profile', { replace: true });
    } catch (err) {
      setEditMsg(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setSaveLoading(false);
    }
  };

  return (
    <>
      <h1 className="profile-page-title">GameMatcher 프로필</h1>
      <div className="profile-header">
        <div className="profile-avatar-wrap">
          {resolveProfileImageUrl(user?.profileImageUrl) ? (
            <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" />
          ) : (
            <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={64} height={64}><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          )}
        </div>
        <div className="profile-info">
          <div className="profile-nickname">{user?.nickname ?? user?.username ?? user?.loginId ?? '—'}</div>
          <p className="profile-bio">{user?.bio?.trim() || '간단 자기소개를 작성해보세요.'}</p>
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
                width: `${Math.min(100, Math.max(0, ((user?.experienceInCurrentLevelTenths ?? 0) / Math.max(1, user?.experienceRequiredForNextLevelTenths ?? 10000)) * 100))}%`,
              }}
            />
          </div>
          <div className="level-exp-text">
            {Math.floor((user?.experienceInCurrentLevelTenths ?? 0) / 10).toLocaleString()} / {(Math.floor((user?.experienceRequiredForNextLevelTenths ?? 10000) / 10)).toLocaleString()} 경험치
          </div>
        </div>
        <div className="profile-stat-card">
          <div className="label">활동 기간</div>
          <div className="value">{formatActivityPeriod(user?.createdAt)}</div>
        </div>
        <div className="profile-stat-card" id="profile-pang-card">
          <div className="label">보유중인 팡</div>
          <div className="value">{Number(pangBalance).toLocaleString()}</div>
          <div className="sub">후원받은 팡까지 포함한 현재 사용 가능 팡입니다.</div>
        </div>
        <div className="profile-stat-card">
          <div className="label">마일리지</div>
          <div className="value mileage-value">{(user?.mileage ?? 0).toLocaleString()}원</div>
          <div className="sub">팡 결제 금액의 5% 적립 (1팡=1.2원 기준)</div>
        </div>
      </div>

      <div className="phe-wrap">
        <h2 className="phe-title">공개 프로필</h2>
        <p className="phe-hint">
          다른 사용자에게 보이는 카드입니다. 닉네임·자기소개·사진 업로드·배너·공개용 이미지 URL·선호 게임은 모두 「프로필 편집」에서 설정할 수 있습니다.
        </p>
        {publicLoading ? <div className="phe-loading">불러오는 중…</div> : null}
        {!publicLoading && publicErr ? (
          <div className="phe-err" role="alert">
            {publicErr}{' '}
            <button type="button" className="phe-btn phe-btn-secondary" onClick={() => void loadPublicProfile()}>
              다시 시도
            </button>
          </div>
        ) : null}
        {!publicLoading ? (
          <>
            {bannerDisplaySrc ? (
              <>
                <div
                  className="phe-banner has-image"
                  style={{ backgroundImage: `url(${JSON.stringify(bannerDisplaySrc)})` }}
                />
                <div className="phe-card">{gamesPreview}</div>
              </>
            ) : (
              <div className="phe-card phe-card-standalone">{gamesPreview}</div>
            )}
          </>
        ) : null}
      </div>

      {/* 프로필 편집 모달 */}
      <div className={`modal-backdrop ${editOpen ? 'show' : ''}`} onClick={() => setEditOpen(false)} role="dialog" aria-modal="true">
        <div className="profile-edit-modal" onClick={(e) => e.stopPropagation()}>
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
                    <svg viewBox="0 0 24 24" fill="currentColor" width={48} height={48}><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                  )}
                </div>
                <input ref={profileImageInputRef} type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={handleProfileImageChange} style={{ display: 'none' }} />
                <button type="button" className="btn-select-photo" onClick={() => profileImageInputRef.current?.click()}>사진 선택</button>
              </div>
            </div>
            <div className="profile-edit-field">
              <label>닉네임 (중복확인 필수)</label>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 6 }}>
                <input
                  type="text"
                  value={editNickname}
                  onChange={(e) => {
                    setEditNickname(e.target.value);
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
                  style={{ padding: '10px 14px', borderRadius: 8, border: '1px solid #00e676', background: 'transparent', color: '#00e676', fontSize: '0.9rem', fontWeight: 500, cursor: nicknameCheckLoading ? 'wait' : 'pointer', whiteSpace: 'nowrap', opacity: nicknameCheckLoading ? 0.7 : 1 }}
                >
                  {nicknameCheckLoading ? '확인 중…' : '중복확인'}
                </button>
              </div>
              <p className={`profile-edit-msg ${nicknameMsg.includes('가능') ? 'ok' : nicknameMsg ? 'err' : ''}`}>{nicknameMsg}</p>
            </div>
            <div className="profile-edit-field">
              <label>자기소개</label>
              <textarea value={editBio} onChange={(e) => setEditBio(e.target.value)} placeholder="간단한 자기소개를 입력하세요." rows={3} maxLength={500} style={{ width: '100%', resize: 'vertical', minHeight: 72, padding: 10, borderRadius: 8, border: '1px solid var(--studio-border-strong, #d8d8dc)', fontSize: '0.95rem', fontFamily: 'inherit', boxSizing: 'border-box' }} />
            </div>
            <div className="profile-edit-field">
              <label htmlFor="edit-banner-url">배너 이미지 URL (공개 카드)</label>
              <input
                id="edit-banner-url"
                type="url"
                placeholder="https://... (비우면 그라데이션 배너)"
                value={fBannerUrl}
                onChange={(e) => setFBannerUrl(e.target.value)}
                autoComplete="off"
              />
            </div>
            <div className="profile-edit-field">
              <label htmlFor="edit-public-profile-url">프로필 이미지 URL (공개 카드용)</label>
              <input
                id="edit-public-profile-url"
                type="url"
                placeholder="https://... (비우면 기본 아바타)"
                value={fProfileUrl}
                onChange={(e) => setFProfileUrl(e.target.value)}
                autoComplete="off"
              />
              <p className="profile-edit-msg" style={{ marginTop: 6, fontSize: '0.8rem', opacity: 0.85 }}>
                위에서 파일로 올린 사진과 별도입니다. 전적·커뮤니티 등 공개 프로필 API에 반영됩니다.
              </p>
            </div>
            <div className="profile-edit-field">
              <span style={{ display: 'block', fontSize: '0.9rem', fontWeight: 500, marginBottom: 8 }}>선호 게임</span>
              <div className="phe-games-row" style={{ marginBottom: 8 }}>
                {selectedGames.length === 0 ? (
                  <span className="phe-games-empty">선택 없음</span>
                ) : (
                  selectedGames.map((g) => (
                    <span key={g} className="phe-game-pill">
                      <span>{g}</span>
                      <button
                        type="button"
                        aria-label={`${g} 제거`}
                        onClick={() => setSelectedGames((prev) => prev.filter((x) => x !== g))}
                        style={{
                          marginLeft: 6,
                          border: 'none',
                          background: 'transparent',
                          color: 'inherit',
                          cursor: 'pointer',
                          padding: 0,
                          fontSize: '1rem',
                          lineHeight: 1,
                        }}
                      >
                        ×
                      </button>
                    </span>
                  ))
                )}
              </div>
              <button
                type="button"
                className="phe-open-games"
                onClick={() => {
                  setModalGameDraft([...selectedGames]);
                  setGamesModalOpen(true);
                }}
              >
                선호 게임 선택
              </button>
            </div>
            <div className="profile-edit-actions">
              <button type="submit" className="btn-save-profile" disabled={!canSave || saveLoading}>
                {saveLoading ? '저장 중…' : '저장'}
              </button>
              <button type="button" className="btn-cancel-profile" onClick={() => setEditOpen(false)}>취소</button>
            </div>
            {editMsg && <p className="profile-edit-msg err" style={{ marginTop: 8 }}>{editMsg}</p>}
          </form>
        </div>
      </div>

      {gamesModalOpen ? (
        <div
          className="phe-modal-backdrop phe-games-modal-backdrop"
          onClick={() => setGamesModalOpen(false)}
          role="presentation"
        >
          <div className="phe-modal" role="dialog" aria-modal="true" aria-labelledby="phe-games-title" onClick={(e) => e.stopPropagation()}>
            <h2 id="phe-games-title">선호 게임</h2>
            <p className="phe-modal-desc">플레이하는 게임을 눌러 선택하세요. (여러 개 가능)</p>
            <div className="phe-game-blocks">
              {PREFERRED_GAME_OPTIONS.map((opt) => {
                const on = modalGameDraft.includes(opt.value);
                return (
                  <button
                    key={opt.value}
                    type="button"
                    className={`phe-game-block${on ? ' selected' : ''}`}
                    onClick={() =>
                      setModalGameDraft((prev) =>
                        prev.includes(opt.value) ? prev.filter((v) => v !== opt.value) : [...prev, opt.value]
                      )
                    }
                    aria-pressed={on}
                  >
                    {opt.value}
                  </button>
                );
              })}
            </div>
            <div className="phe-modal-actions">
              <button type="button" className="phe-btn phe-btn-secondary" onClick={() => setGamesModalOpen(false)}>
                취소
              </button>
              <button
                type="button"
                className="phe-btn phe-btn-primary"
                onClick={() => {
                  setSelectedGames(modalGameDraft);
                  setGamesModalOpen(false);
                }}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      ) : null}

    </>
  );
}
