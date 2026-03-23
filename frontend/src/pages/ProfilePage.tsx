import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  fetchProfile,
  fetchProfileMatches,
  patchProfile,
  type ProfileDto,
  type ProfileMatchDto,
} from '../api/profile';
import {
  PREFERRED_GAME_OPTIONS,
  parsePreferredGamesToSelected,
  serializePreferredGames,
} from '../constants/games';
import { DEFAULT_PROFILE_IMAGE_URL, effectiveCustomProfileUrl } from '../constants/profile';
import './ProfilePage.css';

const LS_USER_ID = 'communityUserId';
const LS_USERNAME = 'communityUsername';

/** 소개 textarea: 기본 노출 높이 ~7줄, 최대 ~15줄까지 자동 확장 후 스크롤 */
const BIO_TEXTAREA_MIN_PX = 168;
const BIO_TEXTAREA_MAX_PX = 360;

function initialSearchQuery(sp: URLSearchParams): string {
  const name = sp.get('username')?.trim();
  if (name) return name;
  const id = sp.get('userId')?.trim();
  if (id) return id;
  if (typeof localStorage !== 'undefined') {
    const n = localStorage.getItem(LS_USERNAME)?.trim();
    if (n) return n;
    const uid = localStorage.getItem(LS_USER_ID)?.trim();
    if (uid) return uid;
  }
  return '';
}

function isNumericUserIdQuery(q: string): boolean {
  const t = q.trim();
  return /^\d+$/.test(t) && parseInt(t, 10) >= 1;
}

export default function ProfilePage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [searchQuery, setSearchQuery] = useState(() =>
    initialSearchQuery(searchParams)
  );
  /** 프로필 단건 조회 중이면 검색창 숨김(초기 URL이 userId일 때도 로딩 동안 숨김) */
  const [profileLoading, setProfileLoading] = useState(() => {
    const iq = initialSearchQuery(searchParams);
    return iq.trim().length > 0 && isNumericUserIdQuery(iq);
  });
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  /** null: 목록 없음(미검색 또는 프로필 선택 후). 빈 배열: 검색 결과 0건 */
  const [matches, setMatches] = useState<ProfileMatchDto[] | null>(null);
  const [phase, setPhase] = useState<'idle' | 'loading' | 'notfound' | 'error'>(() =>
    initialSearchQuery(searchParams).trim() ? 'loading' : 'idle'
  );
  const [errorMsg, setErrorMsg] = useState('');
  const [editOpen, setEditOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const [fUsername, setFUsername] = useState('');
  const [fBio, setFBio] = useState('');
  const [fProfileUrl, setFProfileUrl] = useState('');
  const [fBannerUrl, setFBannerUrl] = useState('');
  const [selectedGames, setSelectedGames] = useState<string[]>([]);
  const [gamesModalOpen, setGamesModalOpen] = useState(false);
  const [modalGameDraft, setModalGameDraft] = useState<string[]>([]);

  const bioTextareaRef = useRef<HTMLTextAreaElement>(null);

  const syncFormFromProfile = useCallback((p: ProfileDto) => {
    setFUsername(p.username);
    setFBio(p.bio ?? '');
    setFProfileUrl(effectiveCustomProfileUrl(p.profileImageUrl));
    setFBannerUrl(p.bannerImageUrl ?? '');
    setSelectedGames(parsePreferredGamesToSelected(p.preferredGames));
  }, []);

  const cancelGamesModal = useCallback(() => setGamesModalOpen(false), []);

  const openGamesModal = () => {
    setModalGameDraft([...selectedGames]);
    setGamesModalOpen(true);
  };

  const toggleModalGame = (value: string) => {
    setModalGameDraft((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value]
    );
  };

  const confirmGamesModal = () => {
    setSelectedGames(modalGameDraft);
    setGamesModalOpen(false);
  };

  const loadProfileByUserId = useCallback(
    async (userId: number) => {
      setProfileLoading(true);
      setErrorMsg('');
      setPhase('loading');
      setProfile(null);
      setMatches(null);
      try {
        const p = await fetchProfile(userId);
        setSearchQuery(p.username);
        setProfile(p);
        syncFormFromProfile(p);
        localStorage.setItem(LS_USER_ID, String(p.userId));
        localStorage.setItem(LS_USERNAME, p.username);
        setPhase('idle');
      } catch (e) {
        if (e instanceof Error && e.message === 'NOT_FOUND') {
          setPhase('notfound');
        } else {
          setErrorMsg(e instanceof Error ? e.message : '불러오기 실패');
          setPhase('error');
        }
      } finally {
        setProfileLoading(false);
      }
    },
    [syncFormFromProfile, setSearchQuery]
  );

  const searchByNickname = useCallback(async (q: string) => {
      setErrorMsg('');
      setPhase('loading');
      setProfile(null);
      setMatches(null);
      try {
        const list = await fetchProfileMatches(q);
        setMatches(list);
        if (list.length === 0) {
          setPhase('notfound');
        } else {
          setPhase('idle');
        }
      } catch (e) {
        setMatches(null);
        if (e instanceof Error && e.message === 'BAD_REQUEST') {
          setPhase('idle');
          setErrorMsg('닉네임을 입력하세요.');
        } else {
          setErrorMsg(e instanceof Error ? e.message : '검색 실패');
          setPhase('error');
        }
      }
  }, []);

  const runSearch = useCallback(() => {
    const q = searchQuery.trim();
    if (!q) {
      setSearchParams({});
      return;
    }
    if (isNumericUserIdQuery(q)) {
      setSearchParams({ userId: q });
    } else {
      setSearchParams({ username: q });
    }
  }, [searchQuery, setSearchParams]);

  const openMatchProfile = useCallback(
    (userId: number, _username: string) => {
      setEditOpen(false);
      setSearchParams({ userId: String(userId) });
    },
    [setSearchParams]
  );

  const profileSearchKey = searchParams.toString();

  useEffect(() => {
    const q = initialSearchQuery(searchParams);
    setSearchQuery(q);
    const t = q.trim();
    if (!t) {
      setProfileLoading(false);
      setErrorMsg('');
      setPhase('idle');
      setProfile(null);
      setMatches(null);
      return;
    }
    if (isNumericUserIdQuery(t)) {
      void loadProfileByUserId(parseInt(t, 10));
    } else {
      void searchByNickname(t);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- searchParams는 쿼리 문자열(profileSearchKey) 변경 시에만 읽으면 됨
  }, [profileSearchKey, loadProfileByUserId, searchByNickname]);

  useEffect(() => {
    if (!editOpen) {
      setGamesModalOpen(false);
    }
  }, [editOpen]);

  useLayoutEffect(() => {
    if (!editOpen) return;
    const el = bioTextareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    const h = Math.min(
      Math.max(el.scrollHeight, BIO_TEXTAREA_MIN_PX),
      BIO_TEXTAREA_MAX_PX
    );
    el.style.height = `${h}px`;
  }, [fBio, editOpen]);

  useEffect(() => {
    if (!gamesModalOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') cancelGamesModal();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [gamesModalOpen, cancelGamesModal]);

  const openEdit = () => {
    if (profile) syncFormFromProfile(profile);
    setEditOpen(true);
  };

  const cancelEdit = () => {
    setEditOpen(false);
    setGamesModalOpen(false);
    if (profile) syncFormFromProfile(profile);
  };

  const save = async () => {
    if (!profile) return;
    const uid = profile.userId;
    setSaving(true);
    setErrorMsg('');
    try {
      const name = fUsername.trim();
      if (!name) {
        setErrorMsg('닉네임을 입력하세요.');
        return;
      }
      const next = await patchProfile(uid, {
        username: name,
        bio: fBio.trim() === '' ? null : fBio.trim(),
        profileImageUrl: fProfileUrl.trim() === '' ? null : fProfileUrl.trim(),
        bannerImageUrl: fBannerUrl.trim() === '' ? null : fBannerUrl.trim(),
        preferredGames: serializePreferredGames(selectedGames),
      });
      setProfile(next);
      syncFormFromProfile(next);
      setSearchQuery(next.username);
      localStorage.setItem(LS_USERNAME, next.username);
      setEditOpen(false);
    } catch (e) {
      setErrorMsg(e instanceof Error ? e.message : '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="profile-wrap">
        {errorMsg ? <div className="profile-error-banner">{errorMsg}</div> : null}

        {!profile && !profileLoading && (
          <div className="profile-user-picker">
            <label htmlFor="profile-search">닉네임</label>
            <input
              id="profile-search"
              type="text"
              autoComplete="username"
              placeholder="예: 데모유저1"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') runSearch();
            }}
          />
            <button type="button" className="profile-btn profile-btn-secondary" onClick={() => runSearch()}>
              검색
            </button>
            <span style={{ color: '#52525b', fontSize: '0.8rem' }}>
              닉네임 부분 일치로 목록 → 클릭 시 프로필 · 숫자만 입력 시 ID로 바로 조회 · URL <code>?username=</code> /{' '}
              <code>?userId=</code>
            </span>
          </div>
        )}

        {matches !== null && matches.length > 0 && (
          <div className="profile-match-list" role="region" aria-label="검색 결과">
            <h2 className="profile-match-list-title">
              검색 결과 <span className="profile-match-count">{matches.length}명</span>
              {matches.length >= 50 ? (
                <span className="profile-match-cap"> (최대 50명까지 표시)</span>
              ) : null}
            </h2>
            <ul className="profile-match-items">
              {matches.map((m) => (
                <li key={m.userId}>
                  <button
                    type="button"
                    className="profile-match-row"
                    onClick={() => openMatchProfile(m.userId, m.username)}
                  >
                    <span className="profile-match-avatar">
                      <img src={m.profileImageUrl} alt="" />
                    </span>
                    <span className="profile-match-meta">
                      <span className="profile-match-name">{m.username}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {phase === 'loading' && <div className="profile-loading">불러오는 중…</div>}
        {phase === 'notfound' && <div className="profile-loading">사용자를 찾을 수 없습니다.</div>}
        {phase === 'error' && !profile && (
          <div className="profile-loading">오류가 발생했습니다.</div>
        )}

        {profile && !editOpen && (
          <>
            <div
              className={`profile-banner${profile.bannerImageUrl ? ' has-image' : ''}`}
              style={
                profile.bannerImageUrl
                  ? { backgroundImage: `url(${JSON.stringify(profile.bannerImageUrl)})` }
                  : undefined
              }
            />
            <div className="profile-card">
              <div className="profile-avatar-wrap">
                <div className="profile-avatar">
                  <img src={profile.profileImageUrl ?? DEFAULT_PROFILE_IMAGE_URL} alt="" />
                </div>
                <div>
                  <div className="profile-username">{profile.username}</div>
                  <div className="profile-userid">ID · {profile.userId}</div>
                </div>
              </div>

              <div className="profile-section">
                <h3>소개</h3>
                <div className={`profile-bio${profile.bio ? '' : ' empty'}`}>
                  {profile.bio ?? '소개가 없습니다.'}
                </div>
              </div>

              <div className="profile-section">
                <h3>선호 게임</h3>
                <div className="profile-games">{profile.preferredGames ?? '—'}</div>
              </div>

              <div className="profile-toolbar">
                <button type="button" className="profile-btn profile-btn-primary" onClick={openEdit}>
                  프로필 수정
                </button>
              </div>
            </div>
          </>
        )}

        {profile && editOpen && (
          <div className="profile-edit-screen">
            <div className="profile-edit-screen-head">
              <div>
                <h2 className="profile-edit-screen-title">프로필 수정</h2>
                <p className="profile-edit-screen-sub">ID · {profile.userId}</p>
              </div>
              <button type="button" className="profile-btn profile-btn-secondary" onClick={cancelEdit}>
                프로필 보기
              </button>
            </div>
            <div className="profile-card profile-edit-card">
              <div className="profile-field">
                <label htmlFor="fUsername">닉네임 (최대 50자)</label>
                <input
                  id="fUsername"
                  type="text"
                  maxLength={50}
                  autoComplete="nickname"
                  value={fUsername}
                  onChange={(e) => setFUsername(e.target.value)}
                />
              </div>
              <div className="profile-field">
                <label htmlFor="fBio">소개 (최대 500자)</label>
                <textarea
                  ref={bioTextareaRef}
                  id="fBio"
                  className="profile-textarea-bio"
                  maxLength={500}
                  value={fBio}
                  onChange={(e) => setFBio(e.target.value)}
                />
              </div>
              <div className="profile-field">
                <label htmlFor="fProfileUrl">프로필 이미지 URL</label>
                <input
                  id="fProfileUrl"
                  type="url"
                  placeholder="https://... (비우면 기본 이미지)"
                  value={fProfileUrl}
                  onChange={(e) => setFProfileUrl(e.target.value)}
                />
                <div className="hint">비우고 저장하면 기본 아바타로 돌아갑니다.</div>
              </div>
              <div className="profile-field">
                <label htmlFor="fBannerUrl">배너 이미지 URL</label>
                <input
                  id="fBannerUrl"
                  type="url"
                  placeholder="https://... (비우면 기본 배너)"
                  value={fBannerUrl}
                  onChange={(e) => setFBannerUrl(e.target.value)}
                />
              </div>
              <div className="profile-field">
                <span className="profile-field-heading">선호 게임</span>
                <div className="profile-games-summary" aria-live="polite">
                  {selectedGames.length === 0 ? (
                    <span className="profile-games-summary-empty">선택된 게임 없음</span>
                  ) : (
                    selectedGames.map((g) => (
                      <span key={g} className="profile-game-chip">
                        <span className="profile-game-chip-text">{g}</span>
                        <button
                          type="button"
                          className="profile-game-chip-remove"
                          aria-label={`${g} 선택 해제`}
                          onClick={() =>
                            setSelectedGames((prev) => prev.filter((x) => x !== g))
                          }
                        >
                          ×
                        </button>
                      </span>
                    ))
                  )}
                </div>
                <button
                  type="button"
                  className="profile-btn profile-btn-open-games"
                  onClick={openGamesModal}
                >
                  선호 게임 선택
                </button>
              </div>
              <div className="profile-toolbar profile-edit-actions">
                <button
                  type="button"
                  className="profile-btn profile-btn-primary"
                  disabled={saving}
                  onClick={save}
                >
                  저장
                </button>
                <button type="button" className="profile-btn profile-btn-secondary" onClick={cancelEdit}>
                  취소
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {gamesModalOpen && (
        <div
          className="profile-modal-backdrop"
          onClick={cancelGamesModal}
          role="presentation"
        >
          <div
            className="profile-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="games-modal-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="games-modal-title" className="profile-modal-title">
              선호 게임
            </h2>
            <p className="profile-modal-desc">플레이하는 게임을 눌러 선택하세요. (여러 개 가능)</p>
            <div className="profile-game-blocks">
              {PREFERRED_GAME_OPTIONS.map((opt) => {
                const on = modalGameDraft.includes(opt.value);
                return (
                  <button
                    key={opt.value}
                    type="button"
                    className={`profile-game-block${on ? ' selected' : ''}`}
                    onClick={() => toggleModalGame(opt.value)}
                    aria-pressed={on}
                  >
                    {opt.value}
                  </button>
                );
              })}
            </div>
            <div className="profile-modal-actions">
              <button
                type="button"
                className="profile-btn profile-btn-secondary"
                onClick={cancelGamesModal}
              >
                취소
              </button>
              <button
                type="button"
                className="profile-btn profile-btn-primary"
                onClick={confirmGamesModal}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
