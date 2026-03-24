import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';

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
    analysis: false,
    viewers: false,
    content: false,
    revenue: false,
    channel: false,
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

export default function StudioLayout({ children }: StudioLayoutProps) {
  const { toggleTheme } = useTheme();
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const profileRef = useRef<HTMLDivElement>(null);

  const [sidebarVisible, setSidebarVisible] = useState(true);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(loadStoredOpenGroups);

  const path = location.pathname;

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

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

    if (
      path.startsWith('/studio/live') ||
      path.startsWith('/studio/settings') ||
      path.startsWith('/studio/alerts') ||
      path.startsWith('/studio/chat')
    ) {
      nextOpen.broadcast = true;
    }

    if (path.startsWith('/studio/analysis')) nextOpen.analysis = true;
    if (path.startsWith('/studio/viewers')) nextOpen.viewers = true;
    if (path.startsWith('/studio/revenue')) nextOpen.revenue = true;

    if (Object.keys(nextOpen).length > 0) {
      setOpenGroups((prev) => {
        const merged = { ...prev, ...nextOpen };
        saveOpenGroups(merged);
        return merged;
      });
    }
  }, [path]);

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

  const comingSoon = (message = '준비 중입니다.') => {
    window.alert(message);
  };

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
          <Link to="/studio/live" className="btn-create">
            + 만들기
          </Link>

          <button
            type="button"
            className="icon-btn"
            title="도움말"
            onClick={() => comingSoon('도움말 기능은 준비 중입니다.')}
          >
            <svg viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
              <circle cx={12} cy={12} r={10} />
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
              <line x1={12} y1={17} x2={12.01} y2={17} />
            </svg>
          </button>

          <button
            type="button"
            className="theme-toggle"
            onClick={toggleTheme}
            title="테마 전환"
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
                    <div className="studio-dropdown-name">
                      {user?.nickname ?? user?.username ?? user?.loginId ?? '사용자'}
                    </div>
                  </div>
                </div>

                <div className="studio-dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>
                    <svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                    내 프로필
                  </Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>
                    <svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <rect x="2" y="4" width="14" height="14" rx="2" />
                      <path d="M17 8v8l5-4z" />
                    </svg>
                    스튜디오
                  </Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}>
                    <svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <rect x="2" y="3" width="20" height="14" rx="2" />
                      <path d="M8 21h8M12 17v4" />
                    </svg>
                    내 채널
                  </Link>
                  <Link to="/following" onClick={() => setProfileOpen(false)}>
                    <svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 18.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                    팔로우 채널
                  </Link>
                  <button type="button" onClick={handleLogout}>
                    <svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    로그아웃
                  </button>
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
            <Link to="/studio" className={`nav-item ${path === '/studio' ? 'active' : ''}`}>
              <span className="icon">▣</span> 대시보드
            </Link>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">방송 관리</div>
            <div className={`nav-group ${openGroups.broadcast ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('broadcast')}>
                <span className="icon">🎥</span> 방송 관리 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/live" className={path === '/studio/live' ? 'active' : ''}>
                    <span className="icon">▶</span> 방송하기
                  </Link>
                </li>
                <li>
                  <Link to="/studio/settings" className={path === '/studio/settings' ? 'active' : ''}>
                    <span className="icon">⚙</span> 설정
                  </Link>
                </li>
                <li>
                  <Link to="/studio/alerts" className={path === '/studio/alerts' ? 'active' : ''}>
                    <span className="icon">🔔</span> 알림
                  </Link>
                </li>
                <li>
                  <Link to="/studio/chat" className={path === '/studio/chat' ? 'active' : ''}>
                    <span className="icon">💬</span> 채팅 설정
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">분석</div>
            <div className={`nav-group ${openGroups.analysis ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('analysis')}>
                <span className="icon">📊</span> 분석 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/analysis/live" className={path === '/studio/analysis/live' ? 'active' : ''}>
                    라이브 분석
                  </Link>
                </li>
                <li>
                  <Link to="/studio/analysis/video" className={path === '/studio/analysis/video' ? 'active' : ''}>
                    동영상 분석
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">시청자 관리</div>
            <div className={`nav-group ${openGroups.viewers ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('viewers')}>
                <span className="icon">👥</span> 시청자 관리 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/viewers/followers" className={path === '/studio/viewers/followers' ? 'active' : ''}>
                    팔로워
                  </Link>
                </li>
                <li>
                  <Link to="/studio/viewers/subscribers" className={path === '/studio/viewers/subscribers' ? 'active' : ''}>
                    구독자
                  </Link>
                </li>
                <li>
                  <Link to="/studio/viewers/blocklist" className={path === '/studio/viewers/blocklist' ? 'active' : ''}>
                    활동 제한
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">콘텐츠</div>
            <div className={`nav-group ${openGroups.content ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('content')}>
                <span className="icon">🗂</span> 콘텐츠 관리 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/streams">라이브 스트리밍</Link>
                </li>
                <li>
                  <a href="#" onClick={(event) => { event.preventDefault(); comingSoon(); }}>동영상</a>
                </li>
                <li>
                  <a href="#" onClick={(event) => { event.preventDefault(); comingSoon(); }}>내가 만든 클립</a>
                </li>
                <li>
                  <a href="#" onClick={(event) => { event.preventDefault(); comingSoon(); }}>내 채널의 클립</a>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">수익</div>
            <div className={`nav-group ${openGroups.revenue ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('revenue')}>
                <span className="icon">💰</span> 수익 관리 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <Link to="/studio/revenue" className={path === '/studio/revenue' ? 'active' : ''}>
                    수익 창출
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">채널</div>
            <div className={`nav-group ${openGroups.channel ? 'open' : ''}`}>
              <button type="button" className="nav-item nav-trigger" onClick={() => toggleGroup('channel')}>
                <span className="icon">📎</span> 채널/권한 관리 <span className="nav-arrow">▾</span>
              </button>
              <ul className="nav-sub">
                <li>
                  <a href="#" className="ext" onClick={(event) => { event.preventDefault(); comingSoon(); }}>
                    채널 관리
                  </a>
                </li>
                <li>
                  <a href="#" onClick={(event) => { event.preventDefault(); comingSoon(); }}>
                    권한 관리
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <div className="nav-divider" />

          <div className="nav-section">
            <Link to="/profile" className="nav-item">
              <span className="icon">👤</span> 내 정보
            </Link>
            <a
              href="#"
              className="nav-item"
              onClick={(event) => {
                event.preventDefault();
                comingSoon('공지사항은 준비 중입니다.');
              }}
            >
              <span className="icon">📢</span> 공지사항
            </a>
          </div>
        </aside>

        <main className="studio-main">{children}</main>
      </div>
    </div>
  );
}
