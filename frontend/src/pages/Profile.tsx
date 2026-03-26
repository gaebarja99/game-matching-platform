import { useState, useRef, useMemo, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { checkNicknameAvailable, repairMojibakeText } from '../api/auth';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { fetchProfile, patchProfile, type ProfileDto, type ProfilePatchBody } from '../api/profile';
import {
  PREFERRED_GAME_OPTIONS,
  parsePreferredGamesToSelected,
  serializePreferredGames,
} from '../constants/games';
import { formatActivityPeriod } from '../lib/activityPeriod';
import { effectiveVisibility } from '../lib/profileVisibility';
import { fetchAccountConnections, type AccountConnectionStatus } from '../api/accountLinks';
import { RiotLinkedGameStats } from '../components/RiotLinkedGameStats';

const LINKED_PROVIDER_LABELS: Record<string, string> = {
  discord: 'Discord',
  steam: 'Steam',
  blizzard: 'Blizzard',
  riot: 'Riot',
};

function resolveBannerStyleUrl(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  const t = url.trim();
  if (t.startsWith('http://') || t.startsWith('https://')) return t;
  return apiUrl(t.replace(/^\//, ''));
}

function computeExtDirty(
  publicProfile: ProfileDto | null,
  banner: string,
  selectedGames: string[]
): boolean {
  const b0 = (publicProfile?.bannerImageUrl ?? '').trim();
  const b1 = banner.trim();
  const g0 = serializePreferredGames(parsePreferredGamesToSelected(publicProfile?.preferredGames)) ?? '';
  const g1 = serializePreferredGames(selectedGames) ?? '';
  return b0 !== b1 || g0 !== g1;
}

function linkVisibilityPatchKey(providerLower: string): keyof ProfilePatchBody | null {
  switch (providerLower) {
    case 'discord':
      return 'discordLinkVisible';
    case 'steam':
      return 'steamLinkVisible';
    case 'blizzard':
      return 'blizzardLinkVisible';
    case 'riot':
      return 'riotLinkVisible';
    default:
      return null;
  }
}

/** Riot 연동: 닉네임 표시 / LoL 랭크 / 발로 랭크 공개 */
type RiotVisibilityDraft = {
  link: boolean;
  lolRank: boolean;
  valorantRank: boolean;
};

function riotDraftFromConnection(c: AccountConnectionStatus | undefined): RiotVisibilityDraft | null {
  if (!c || (c.provider ?? '').toLowerCase() !== 'riot') return null;
  return {
    link: c.publicProfileVisible !== false,
    lolRank: c.publicLolRankVisible !== false,
    valorantRank: c.publicValorantRankVisible !== false,
  };
}

function computeRiotVisibilityDirty(a: RiotVisibilityDraft | null, b: RiotVisibilityDraft | null): boolean {
  if (!a && !b) return false;
  if (!a || !b) return true;
  return a.link !== b.link || a.lolRank !== b.lolRank || a.valorantRank !== b.valorantRank;
}

function computeLinkVisibilityDirty(
  linkedSnapshot: AccountConnectionStatus[],
  linkDraft: Record<string, boolean>,
  riotDraft: RiotVisibilityDraft | null,
  riotSnap: RiotVisibilityDraft | null
): boolean {
  if (computeRiotVisibilityDirty(riotSnap, riotDraft)) return true;
  for (const c of linkedSnapshot) {
    const key = (c.provider ?? '').toLowerCase();
    if (key === 'riot') continue;
    const cur = c.publicProfileVisible !== false;
    const next = linkDraft[key] ?? cur;
    if (cur !== next) return true;
  }
  return false;
}

function computeVisibilityDirty(
  publicProfile: ProfileDto | null,
  preferredGames: boolean,
  linkedSnapshot: AccountConnectionStatus[],
  linkDraft: Record<string, boolean>,
  riotDraft: RiotVisibilityDraft | null,
  riotSnap: RiotVisibilityDraft | null
): boolean {
  if (effectiveVisibility(publicProfile?.visibility).preferredGames !== preferredGames) return true;
  return computeLinkVisibilityDirty(linkedSnapshot, linkDraft, riotDraft, riotSnap);
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
  const [selectedGames, setSelectedGames] = useState<string[]>([]);
  const [gamesModalOpen, setGamesModalOpen] = useState(false);
  const [modalGameDraft, setModalGameDraft] = useState<string[]>([]);
  const [linkedAccounts, setLinkedAccounts] = useState<AccountConnectionStatus[]>([]);
  /** 공개 프로필에서 선호 게임 표시 여부(체크 = 공개) */
  const [vPublicPreferredGames, setVPublicPreferredGames] = useState(true);
  const [visibilityOpen, setVisibilityOpen] = useState(false);
  const [visibilitySaveLoading, setVisibilitySaveLoading] = useState(false);
  const [visibilityEditMsg, setVisibilityEditMsg] = useState('');
  /** 공개 모달을 열 당시 연동 목록(변경 감지·체크박스용) */
  const [visibilityLinkedSnapshot, setVisibilityLinkedSnapshot] = useState<AccountConnectionStatus[]>([]);
  const [linkPublicDraft, setLinkPublicDraft] = useState<Record<string, boolean>>({});
  const [riotVisibilityDraft, setRiotVisibilityDraft] = useState<RiotVisibilityDraft | null>(null);

  const loadLinkedAccounts = useCallback(async () => {
    if (!user?.id) return;
    try {
      const { ok, data } = await fetchAccountConnections();
      if (ok && data?.connections) {
        setLinkedAccounts(data.connections.filter((c) => c.connected));
      } else {
        setLinkedAccounts([]);
      }
    } catch {
      setLinkedAccounts([]);
    }
  }, [user?.id]);

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
    void loadLinkedAccounts();
  }, [loadLinkedAccounts]);

  useEffect(() => {
    if (!editOpen) setGamesModalOpen(false);
  }, [editOpen]);

  useEffect(() => {
    if (!visibilityOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setVisibilityOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [visibilityOpen]);

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
    const extDirty = computeExtDirty(publicProfile, fBannerUrl, selectedGames);
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
    selectedGames,
  ]);

  const pangBalance = user?.pangBalance ?? 0;
  const cardVisibility = effectiveVisibility(publicProfile?.visibility);
  const displayName =
    repairMojibakeText(user?.nickname) ??
    repairMojibakeText(user?.username) ??
    repairMojibakeText(user?.loginId) ??
    '사용자';
  const displayBio = repairMojibakeText(user?.bio)?.trim() || '간단한 자기소개를 작성해보세요.';
  const bannerDisplaySrc = publicProfile ? resolveBannerStyleUrl(publicProfile.bannerImageUrl) : null;
  const gameTagsDisplay = parsePreferredGamesToSelected(publicProfile?.preferredGames);
  const gamesPreview = (
    <>
      <div className="phe-section-label">선호 게임</div>
      <div className="phe-games-row">
        {!cardVisibility.preferredGames ? (
          <span className="phe-games-empty">다른 사용자에게는 표시하지 않도록 설정했습니다.</span>
        ) : gameTagsDisplay.length === 0 ? (
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
      setSelectedGames(parsePreferredGamesToSelected(p.preferredGames));
    } else {
      setFBannerUrl('');
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
    const extDirty = computeExtDirty(publicProfile, fBannerUrl, selectedGames);

    setSaveLoading(true);
    try {
      let uploadedPublicImagePath: string | undefined;
      if (mainDirty) {
        const fd = new FormData();
        fd.append('nickname', nickname);
        fd.append('bio', editBio.trim());
        if (profileImageFile) fd.append('profileImage', profileImageFile);
        const r = await fetch(apiUrl('api/profile'), { method: 'PUT', credentials: 'include', body: fd });
        const putData = (await r.json().catch(() => ({}))) as { message?: string; profileImageUrl?: string | null };
        if (!r.ok) {
          throw new Error(putData.message ?? '저장에 실패했습니다.');
        }
        if (profileImageFile && putData.profileImageUrl) {
          uploadedPublicImagePath = putData.profileImageUrl;
        }
      }
      const patchBody: ProfilePatchBody = {};
      /** PUT만 하고 PATCH를 건너뛰면 user_profiles(배너·선호 게임)가 비는 경우가 있어, 메인/확장/이미지 경로 중 하나라도 저장할 때 동기화 */
      const needsUserProfilePatch = extDirty || mainDirty || uploadedPublicImagePath !== undefined;
      if (needsUserProfilePatch) {
        patchBody.bannerImageUrl = fBannerUrl.trim() === '' ? null : fBannerUrl.trim();
        patchBody.preferredGames = serializePreferredGames(selectedGames);
      }
      if (uploadedPublicImagePath !== undefined) {
        patchBody.profileImageUrl = uploadedPublicImagePath;
      }
      if (user?.id && Object.keys(patchBody).length > 0) {
        const next = await patchProfile(user.id, patchBody);
        setPublicProfile(next);
      }
      await refreshUser();
      await loadPublicProfile();
      await loadLinkedAccounts();
      setEditOpen(false);
      navigate('/profile', { replace: true });
    } catch (err) {
      setEditMsg(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleOpenVisibilitySettings = async () => {
    if (!user?.id) return;
    setVisibilityEditMsg('');
    try {
      const p = await fetchProfile(user.id);
      setPublicProfile(p);
      const v = effectiveVisibility(p.visibility);
      setVPublicPreferredGames(v.preferredGames);
      const { ok, data } = await fetchAccountConnections();
      const connected =
        ok && data?.connections ? data.connections.filter((c) => c.connected) : [];
      setVisibilityLinkedSnapshot(connected);
      setLinkPublicDraft(
        Object.fromEntries(
          connected
            .filter((c) => (c.provider ?? '').toLowerCase() !== 'riot')
            .map((c) => [((c.provider ?? '') as string).toLowerCase(), c.publicProfileVisible !== false])
        )
      );
      setRiotVisibilityDraft(riotDraftFromConnection(connected.find((c) => (c.provider ?? '').toLowerCase() === 'riot')));
    } catch {
      setVisibilityEditMsg('공개 설정을 불러오지 못했습니다. 표시되는 값을 확인한 뒤 저장해 보세요.');
      const v = effectiveVisibility(publicProfile?.visibility);
      setVPublicPreferredGames(v.preferredGames);
      const connected = linkedAccounts.filter((c) => c.connected);
      setVisibilityLinkedSnapshot(connected);
      setLinkPublicDraft(
        Object.fromEntries(
          connected
            .filter((c) => (c.provider ?? '').toLowerCase() !== 'riot')
            .map((c) => [((c.provider ?? '') as string).toLowerCase(), c.publicProfileVisible !== false])
        )
      );
      setRiotVisibilityDraft(riotDraftFromConnection(connected.find((c) => (c.provider ?? '').toLowerCase() === 'riot')));
    }
    setVisibilityOpen(true);
  };

  const handleSaveVisibilityOnly = async () => {
    if (!user?.id || visibilitySaveLoading) return;
    setVisibilityEditMsg('');
    const riotSnap = riotDraftFromConnection(
      visibilityLinkedSnapshot.find((c) => (c.provider ?? '').toLowerCase() === 'riot')
    );
    const visDirty = computeVisibilityDirty(
      publicProfile,
      vPublicPreferredGames,
      visibilityLinkedSnapshot,
      linkPublicDraft,
      riotVisibilityDraft,
      riotSnap
    );
    if (!visDirty) {
      setVisibilityOpen(false);
      return;
    }
    setVisibilitySaveLoading(true);
    try {
      const patchBody: ProfilePatchBody = {
        preferredGamesVisible: vPublicPreferredGames,
      };
      for (const c of visibilityLinkedSnapshot) {
        const key = (c.provider ?? '').toLowerCase();
        if (key === 'riot') continue;
        const pk = linkVisibilityPatchKey(key);
        if (!pk) continue;
        const cur = c.publicProfileVisible !== false;
        const next = linkPublicDraft[key] ?? cur;
        if (cur !== next) {
          (patchBody as Record<string, boolean | undefined>)[pk] = next;
        }
      }
      if (riotSnap && riotVisibilityDraft) {
        if (riotSnap.link !== riotVisibilityDraft.link) {
          patchBody.riotLinkVisible = riotVisibilityDraft.link;
        }
        if (riotSnap.lolRank !== riotVisibilityDraft.lolRank) {
          patchBody.riotLolRankVisible = riotVisibilityDraft.lolRank;
        }
        if (riotSnap.valorantRank !== riotVisibilityDraft.valorantRank) {
          patchBody.riotValorantRankVisible = riotVisibilityDraft.valorantRank;
        }
      }
      const next = await patchProfile(user.id, patchBody);
      setPublicProfile(next);
      await loadPublicProfile();
      await loadLinkedAccounts();
      setVisibilityOpen(false);
    } catch (err) {
      setVisibilityEditMsg(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setVisibilitySaveLoading(false);
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
          <div className="value">{formatActivityPeriod(user?.createdAt ?? null)}</div>
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
        <div className="phe-title-row">
          <h2 className="phe-title">공개 프로필</h2>
          <button type="button" className="phe-visibility-settings-btn" onClick={() => void handleOpenVisibilitySettings()}>
            공개 표시 설정
          </button>
        </div>
        <p className="phe-hint">
          다른 사용자에게 보이는 카드입니다. 내용은 「프로필 편집」에서 수정하고, 선호 게임·연동 계정 표시 여부는 「공개 표시 설정」에서 바꿀 수 있습니다.
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

      {linkedAccounts.length > 0 ? (
        <section className="profile-linked-section" aria-label="연동된 외부 계정">
          <h2 className="phe-title">연동된 계정</h2>
          <p className="phe-hint profile-linked-hint">
            추가·해제와 각 연동의 공개 여부는{' '}
            <Link to="/profile/account-links" className="profile-linked-inline-link">
              외부 계정 연동
            </Link>
            에서 설정합니다.
          </p>
          <ul className="profile-linked-list">
            {linkedAccounts.map((c) => {
              const key = (c.provider ?? '').toLowerCase();
              const label = LINKED_PROVIDER_LABELS[key] ?? c.provider;
              const avatarSrc =
                c.avatarUrl && (c.avatarUrl.startsWith('http') || c.avatarUrl.startsWith('/'))
                  ? resolveProfileImageUrl(c.avatarUrl) ?? c.avatarUrl
                  : null;
              const showSub = Boolean(c.secondaryValue) && key !== 'riot';
              return (
                <li key={key} className={`profile-linked-row${key === 'riot' ? ' profile-linked-row--riot' : ''}`}>
                  <div className="profile-linked-row-main">
                    {avatarSrc ? (
                      <img
                        className="profile-linked-avatar"
                        src={avatarSrc}
                        alt=""
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    ) : (
                      <span className="profile-linked-avatar-fallback" aria-hidden />
                    )}
                    <div className="profile-linked-text">
                      <span className="profile-linked-provider">
                        {label}
                        {key === 'riot' ? (
                          <>
                            {c.publicProfileVisible === false ? (
                              <span className="profile-linked-visibility is-off"> · Riot 연동 숨김</span>
                            ) : (
                              <span className="profile-linked-visibility is-on"> · Riot 닉네임 공개</span>
                            )}
                          </>
                        ) : c.publicProfileVisible === false ? (
                          <span className="profile-linked-visibility is-off"> · 타인에게 숨김</span>
                        ) : (
                          <span className="profile-linked-visibility is-on"> · 공개</span>
                        )}
                      </span>
                      <span className="profile-linked-display">{c.displayName?.trim() || '—'}</span>
                      {showSub ? <span className="profile-linked-secondary">{c.secondaryValue}</span> : null}
                      {key === 'riot' && c.publicProfileVisible !== false ? (
                        <RiotLinkedGameStats
                          riotDisplayName={c.displayName}
                          lolRankSummary={c.lolRankSummary}
                          valorantRankSummary={c.valorantRankSummary}
                          showVisibilityBadges
                          lolPublic={c.publicLolRankVisible !== false}
                          valorPublic={c.publicValorantRankVisible !== false}
                        />
                      ) : null}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      ) : null}

      {/* 프로필 편집 모달 */}
      <div className={`modal-backdrop ${editOpen ? 'show' : ''}`} onClick={() => setEditOpen(false)} role="dialog" aria-modal="true">
        <div className="profile-edit-modal" onClick={(e) => e.stopPropagation()}>
          <h2>프로필 편집</h2>
          <form onSubmit={handleSaveProfile}>
            <div className="profile-edit-field profile-edit-avatar-row">
              <label>프로필 사진</label>
              <p className="profile-edit-msg" style={{ margin: '0 0 8px', fontSize: '0.8rem', opacity: 0.85 }}>
                공개 프로필(전적·커뮤니티 등)에도 같은 사진이 쓰입니다.
              </p>
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

      <div
        className={`modal-backdrop ${visibilityOpen ? 'show' : ''}`}
        onClick={() => setVisibilityOpen(false)}
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-visibility-title"
      >
        <div className="profile-edit-modal" onClick={(e) => e.stopPropagation()}>
          <h2 id="profile-visibility-title">공개 프로필에 표시</h2>
          <p className="profile-edit-msg" style={{ margin: '0 0 14px', fontSize: '0.85rem', opacity: 0.85 }}>
            체크 해제한 항목은 전적·커뮤니티 등 공개 프로필 조회에서 숨겨집니다. 자기소개·배너·프로필 사진은 항상 공개됩니다.
          </p>
          <label className="profile-visibility-row">
            <input type="checkbox" checked={vPublicPreferredGames} onChange={(e) => setVPublicPreferredGames(e.target.checked)} />
            <span>선호 게임 공개</span>
          </label>
          {visibilityLinkedSnapshot.length > 0 ? (
            <>
              <h3 className="profile-visibility-modal-subtitle">연동된 계정</h3>
              <p className="profile-edit-msg" style={{ margin: '0 0 10px', fontSize: '0.82rem', opacity: 0.82 }}>
                표시 이름·게임 랭크 등 연동 정보입니다. Riot은 LoL·발로란트를 각각 설정할 수 있습니다. 계정 연결·해제는{' '}
                <Link to="/profile/account-links" className="profile-linked-inline-link" onClick={(e) => e.stopPropagation()}>
                  외부 계정 연동
                </Link>
                에서 할 수 있습니다.
              </p>
              {visibilityLinkedSnapshot
                .filter((c) => (c.provider ?? '').toLowerCase() !== 'riot')
                .map((c) => {
                  const key = (c.provider ?? '').toLowerCase();
                  const label = LINKED_PROVIDER_LABELS[key] ?? c.provider;
                  return (
                    <label key={`${key}-vis`} className="profile-visibility-row">
                      <input
                        type="checkbox"
                        checked={linkPublicDraft[key] ?? true}
                        onChange={(e) =>
                          setLinkPublicDraft((prev) => ({ ...prev, [key]: e.target.checked }))
                        }
                      />
                      <span>
                        {label} 연동 정보 공개 <span style={{ opacity: 0.75, fontWeight: 400 }}>(랭크 등)</span>
                      </span>
                    </label>
                  );
                })}
              {riotVisibilityDraft ? (
                <div className="profile-visibility-riot-block">
                  <h4 className="profile-visibility-riot-subheading">Riot (LoL · 발로란트)</h4>
                  <label className="profile-visibility-row profile-visibility-row--riot-parent">
                    <input
                      type="checkbox"
                      checked={riotVisibilityDraft.link}
                      onChange={(e) =>
                        setRiotVisibilityDraft((prev) =>
                          prev ? { ...prev, link: e.target.checked } : prev
                        )
                      }
                    />
                    <span>Riot 닉네임·연동 표시</span>
                  </label>
                  <div
                    className={`profile-visibility-riot-children${riotVisibilityDraft.link ? '' : ' is-disabled'}`}
                    aria-disabled={!riotVisibilityDraft.link}
                  >
                    <label className="profile-visibility-row">
                      <input
                        type="checkbox"
                        checked={riotVisibilityDraft.lolRank}
                        disabled={!riotVisibilityDraft.link}
                        onChange={(e) =>
                          setRiotVisibilityDraft((prev) =>
                            prev ? { ...prev, lolRank: e.target.checked } : prev
                          )
                        }
                      />
                      <span>리그 오브 레전드 랭크 정보 공개</span>
                    </label>
                    <label className="profile-visibility-row">
                      <input
                        type="checkbox"
                        checked={riotVisibilityDraft.valorantRank}
                        disabled={!riotVisibilityDraft.link}
                        onChange={(e) =>
                          setRiotVisibilityDraft((prev) =>
                            prev ? { ...prev, valorantRank: e.target.checked } : prev
                          )
                        }
                      />
                      <span>발로란트 경쟁 티어 정보 공개</span>
                    </label>
                  </div>
                </div>
              ) : null}
            </>
          ) : null}
          <div className="profile-edit-actions" style={{ marginTop: 18 }}>
            <button
              type="button"
              className="btn-save-profile"
              disabled={visibilitySaveLoading}
              onClick={() => void handleSaveVisibilityOnly()}
            >
              {visibilitySaveLoading ? '저장 중…' : '저장'}
            </button>
            <button type="button" className="btn-cancel-profile" onClick={() => setVisibilityOpen(false)}>
              취소
            </button>
          </div>
          {visibilityEditMsg ? (
            <p className={`profile-edit-msg ${visibilityEditMsg.includes('실패') || visibilityEditMsg.includes('못했') ? 'err' : ''}`} style={{ marginTop: 10 }}>
              {visibilityEditMsg}
            </p>
          ) : null}
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
