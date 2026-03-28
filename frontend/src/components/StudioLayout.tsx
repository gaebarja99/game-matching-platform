import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { apiFetch, resolveProfileImageUrl } from '../api/client';

const STUDIO_SIDEBAR_STORAGE_KEY = 'gamematcher-studio-sidebar-open';

function loadStoredOpenGroups(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(STUDIO_SIDEBAR_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Record<string, boolean>;
      if (parsed && typeof parsed === 'object') return parsed;
    }
  } catch {
    /* ignore */
  }

  return {
    broadcast: true,
    analysis: true,
    viewers: true,
    revenue: true,
    channel: true,
  };
}

function saveOpenGroups(groups: Record<string, boolean>) {
  try {
    localStorage.setItem(STUDIO_SIDEBAR_STORAGE_KEY, JSON.stringify(groups));
  } catch {
    /* ignore */
  }
}

interface StudioLayoutProps {
  children: React.ReactNode;
}

type ManagedChannel = {
  ownerUserId: number;
  ownerNickname: string;
  ownerLoginId: string;
  roleName: string;
};

export default function StudioLayout({ children }: StudioLayoutProps) {
  const { toggleTheme } = useTheme();
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isAdmin = ['ADMIN', 'ROLE_ADMIN'].includes((user?.role ?? '').toUpperCase());
  const profileRef = useRef<HTMLDivElement>(null);
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(loadStoredOpenGroups);
  const [managedChannels, setManagedChannels] = useState<ManagedChannel[]>([]);

  const path = location.pathname;
  const ownerUserIdParam = new URLSearchParams(location.search).get('ownerUserId');
  const parsedOwnerUserId = ownerUserIdParam ? Number(ownerUserIdParam) : NaN;
  const selectedOwnerUserId = Number.isFinite(parsedOwnerUserId) ? parsedOwnerUserId : null;
  const isDashboard = path === '/studio';
  const isBroadcast = path.startsWith('/studio/live') || path.startsWith('/studio/settings') || path.startsWith('/studio/alerts');
  const isAnalysis = path.startsWith('/studio/analysis');
  const isViewers = path.startsWith('/studio/viewers');
  const isRevenue = path.startsWith('/studio/revenue');
  const isChannel = path.startsWith('/studio/channel') || path.startsWith('/studio/chat');

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  useEffect(() => {
    let cancelled = false;

    const loadManagedChannels = async () => {
      const query = selectedOwnerUserId ? `?ownerUserId=${selectedOwnerUserId}` : '';
      const response = await apiFetch<{ managedChannels?: ManagedChannel[] }>(`api/studio/channel/context${query}`);
      if (cancelled) return;
      if (!response.ok || !response.data) {
        setManagedChannels([]);
        return;
      }
      setManagedChannels(Array.isArray(response.data.managedChannels) ? response.data.managedChannels : []);
    };

    loadManagedChannels();

    return () => {
      cancelled = true;
    };
  }, [selectedOwnerUserId]);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    };

    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  useEffect(() => {
    const nextOpen: Record<string, boolean> = {};
    if (isBroadcast) nextOpen.broadcast = true;
    if (isAnalysis) nextOpen.analysis = true;
    if (isViewers) nextOpen.viewers = true;
    if (isRevenue) nextOpen.revenue = true;
    if (isChannel) nextOpen.channel = true;

    if (Object.keys(nextOpen).length > 0) {
      setOpenGroups((prev) => {
        const merged = { ...prev, ...nextOpen };
        saveOpenGroups(merged);
        return merged;
      });
    }
  }, [isAnalysis, isBroadcast, isChannel, isRevenue, isViewers]);

  const toggleGroup = (key: string) => {
    setOpenGroups((prev) => {
      const next = { ...prev, [key]: !prev[key] };
      saveOpenGroups(next);
      return next;
    });
  };

  const handleLogout = async () => {
    await logout();
    setProfileOpen(false);
    navigate('/streams');
  };

  const buildManagedChannelLink = (ownerUserId: number, section: 'manage' | 'permissions' = 'manage') =>
    `/studio/channel/${section}?ownerUserId=${ownerUserId}`;

  const isManagedChannelActive = (ownerUserId: number) =>
    isChannel && selectedOwnerUserId === ownerUserId;

  return (
    <div className="studio-page">
      <header className="studio-header">
        <button
          type="button"
          className="menu-btn"
          onClick={() => setSidebarVisible((visible) => !visible)}
          aria-label="메뉴"
        >
          <svg viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
            <line x1={3} y1={6} x2={21} y2={6} />
            <line x1={3} y1={12} x2={21} y2={12} />
            <line x1={3} y1={18} x2={21} y2={18} />
          </svg>
        </button>
        <Link to="/streams" className="logo">
          GameMatcher <span>스튜디오</span>
        </Link>
        <div className="header-right">
          <button
            type="button"
            className="theme-toggle"
            onClick={toggleTheme}
            title="다크/라이트 모드 전환"
            aria-label="테마 전환"
          >
            <svg className="icon-sun" viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
              <circle cx={12} cy={12} r={4} />
              <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
            </svg>
            <svg className="icon-moon" viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          </button>
          <div className="studio-header-profile-wrap" ref={profileRef}>
            <button
              type="button"
              className="profile-btn"
              onClick={() => setProfileOpen((open) => !open)}
              title="프로필"
              aria-label="프로필 메뉴"
            >
              {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? (
                <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} />
              ) : (
                <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                </svg>
              )}
            </button>
            {profileOpen ? (
              <div className="studio-header-profile-dropdown show">
                <div className="studio-dropdown-profile-info">
                  <div className="studio-dropdown-avatar">
                    {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? (
                      <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} />
                    ) : (
                      <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={40} height={40}>
                        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                      </svg>
                    )}
                  </div>
                  <div>
                    <div className="studio-dropdown-label">GameMatcher 프로필</div>
                    <div className="studio-dropdown-name">{user?.nickname ?? user?.username ?? user?.loginId ?? '사용자'}</div>
                  </div>
                </div>
                <div className="studio-dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>내 프로필</Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>스튜디오</Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}>내 채널</Link>
                  {managedChannels.map((channel) => (
                    <Link
                      key={channel.ownerUserId}
                      to={buildManagedChannelLink(channel.ownerUserId)}
                      onClick={() => setProfileOpen(false)}
                    >
                      {channel.ownerNickname}의 채널
                    </Link>
                  ))}
                  <Link to="/following" onClick={() => setProfileOpen(false)}>팔로잉 채널</Link>
                  {isAdmin ? <Link to="/admin" onClick={() => setProfileOpen(false)}>관리자</Link> : null}
                  <button type="button" onClick={handleLogout}>로그아웃</button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <div className="studio-body">
        <aside className="studio-sidebar" style={{ display: sidebarVisible ? undefined : 'none' }}>
          <div className="nav-section">
            <div className="nav-section-title">스튜디오</div>
            <Link to="/studio" className={`nav-item ${isDashboard ? 'active' : ''}`}>
              <span className="icon">📋</span>
              대시보드
            </Link>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">방송 관리</div>
            <div className={`nav-group ${openGroups.broadcast ? 'open' : ''}`}>
              <button type="button" className={`nav-item nav-trigger ${isBroadcast ? 'active' : ''}`} onClick={() => toggleGroup('broadcast')}>
                <span className="icon">📺</span>
                방송 관리
                <span className="nav-arrow">▲</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/live" className={path === '/studio/live' ? 'active' : ''}>
                    <span className="icon">📁</span>
                    방송하기
                  </Link>
                </li>
                <li>
                  <Link to="/studio/settings" className={path === '/studio/settings' ? 'active' : ''}>
                    <span className="icon">⚙️</span>
                    설정
                  </Link>
                </li>
                <li>
                  <Link to="/studio/alerts" className={path === '/studio/alerts' ? 'active' : ''}>
                    <span className="icon">🔔</span>
                    알림
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">분석</div>
            <div className={`nav-group ${openGroups.analysis ? 'open' : ''}`}>
              <button type="button" className={`nav-item nav-trigger ${isAnalysis ? 'active' : ''}`} onClick={() => toggleGroup('analysis')}>
                <span className="icon">📊</span>
                라이브 분석
                <span className="nav-arrow">▲</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/analysis/live" className={path === '/studio/analysis/live' ? 'active' : ''}>
                    라이브 분석
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">시청자 관리</div>
            <div className={`nav-group ${openGroups.viewers ? 'open' : ''}`}>
              <button type="button" className={`nav-item nav-trigger ${isViewers ? 'active' : ''}`} onClick={() => toggleGroup('viewers')}>
                <span className="icon">👥</span>
                시청자 관리
                <span className="nav-arrow">▲</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/viewers/followers" className={path === '/studio/viewers/followers' ? 'active' : ''}>
                    팔로워
                  </Link>
                </li>
                <li>
                  <Link to="/studio/viewers/fans" className={path === '/studio/viewers/fans' ? 'active' : ''}>
                    팬
                  </Link>
                </li>
                <li>
                  <Link to="/studio/viewers/subscribers" className={path === '/studio/viewers/subscribers' ? 'active' : ''}>
                    구독자
                  </Link>
                </li>
                <li>
                  <Link
                    to="/studio/viewers/blacklist"
                    className={path === '/studio/viewers/blacklist' || path === '/studio/viewers/blocklist' ? 'active' : ''}
                  >
                    블랙리스트
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">수익</div>
            <div className={`nav-group ${openGroups.revenue ? 'open' : ''}`}>
              <button type="button" className={`nav-item nav-trigger ${isRevenue ? 'active' : ''}`} onClick={() => toggleGroup('revenue')}>
                <span className="icon">💰</span>
                수익 관리
                <span className="nav-arrow">▲</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/revenue" className={path === '/studio/revenue' ? 'active' : ''}>
                    수익 현황
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">채널</div>
            <div className={`nav-group ${openGroups.channel ? 'open' : ''}`}>
              <button type="button" className={`nav-item nav-trigger ${isChannel ? 'active' : ''}`} onClick={() => toggleGroup('channel')}>
                <span className="icon">🔗</span>
                채널/권한 관리
                <span className="nav-arrow">▲</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/channel/manage" className={path === '/studio/channel/manage' && !selectedOwnerUserId ? 'active' : ''}>
                    채널 관리
                  </Link>
                </li>
                <li>
                  <Link to="/studio/channel/permissions" className={path === '/studio/channel/permissions' && !selectedOwnerUserId ? 'active' : ''}>
                    권한 관리
                  </Link>
                </li>
                {managedChannels.map((channel) => (
                  <li key={channel.ownerUserId}>
                    <Link
                      to={buildManagedChannelLink(channel.ownerUserId)}
                      className={isManagedChannelActive(channel.ownerUserId) ? 'active' : ''}
                    >
                      {channel.ownerNickname}의 채널
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </aside>

        <main className="studio-main">{children}</main>
      </div>
    </div>
  );
}
