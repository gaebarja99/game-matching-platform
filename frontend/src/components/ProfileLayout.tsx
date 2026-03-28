import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { resolveProfileImageUrl } from '../api/client';

function getDisplayName(user: ReturnType<typeof useAuth>['user']): string {
  return user?.nickname?.trim() || user?.username?.trim() || user?.loginId?.trim() || '유저';
}

function SidebarIcon({ children }: { children: React.ReactNode }) {
  return <span className="icon profile-icon">{children}</span>;
}

export default function ProfileLayout() {
  const { toggleTheme } = useTheme();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

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

  const handleLogout = async () => {
    await logout();
    setProfileOpen(false);
    navigate('/');
  };

  const profileImage = resolveProfileImageUrl(user?.profileImageUrl);
  const displayName = getDisplayName(user);

  if (!user) {
    return (
      <div className="profile-page">
        <header className="header">
          <Link to="/" className="header-logo">
            GameMatcher
          </Link>
          <nav className="header-nav">
            <Link to="/streams">방송</Link>
            <Link to="/records">전적 검색</Link>
            <Link to="/community">커뮤니티</Link>
          </nav>
          <div className="header-right">
            <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
              <span className="theme-icon">
                <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                  <circle cx="12" cy="12" r="4" />
                  <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
                </svg>
                <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
                </svg>
              </span>
            </button>
            <Link to="/login" className="btn-login no-auth">
              로그인
            </Link>
          </div>
        </header>
        <div className="profile-login-msg">
          <p>로그인하면 프로필과 계정 연동 설정을 이용할 수 있습니다.</p>
          <Link to="/login">로그인하기</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <header className="header">
        <Link to="/" className="header-logo">
          GameMatcher
        </Link>
        <nav className="header-nav">
          <Link to="/streams">방송</Link>
          <Link to="/records">전적 검색</Link>
          <Link to="/community">커뮤니티</Link>
        </nav>
        <div className="header-right">
          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            </span>
          </button>
          <div className="header-profile-wrap auth-only" ref={profileRef}>
            <button
              type="button"
              className="header-profile-avatar"
              onClick={() => setProfileOpen((open) => !open)}
              aria-label="프로필 메뉴"
            >
              {profileImage && !profileImgError ? (
                <img src={profileImage} alt="" onError={() => setProfileImgError(true)} />
              ) : (
                <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                </svg>
              )}
            </button>
            {profileOpen ? (
              <div className="header-profile-dropdown show">
                <div className="dropdown-profile-info">
                  <div className="dropdown-avatar">
                    {profileImage && !profileImgError ? (
                      <img src={profileImage} alt="" onError={() => setProfileImgError(true)} />
                    ) : (
                      <svg
                        className="avatar-placeholder"
                        style={{ width: 32, height: 32, margin: 8 }}
                        viewBox="0 0 24 24"
                        fill="currentColor"
                      >
                        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                      </svg>
                    )}
                  </div>
                  <div>
                    <div className="dropdown-profile-label">GameMatcher 프로필</div>
                    <div className="dropdown-profile-name">{displayName}</div>
                  </div>
                </div>
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>
                    내 프로필
                  </Link>
                  <Link to="/profile/account-links" onClick={() => setProfileOpen(false)}>
                    외부 계정 연동
                  </Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>
                    스튜디오
                  </Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}>
                    내 채널
                  </Link>
                  <Link to="/following" onClick={() => setProfileOpen(false)}>
                    팔로잉 채널
                  </Link>
                  <button type="button" onClick={handleLogout}>
                    로그아웃
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <div className="layout">
        <aside className="profile-sidebar">
          <div className="nav-section">
            <div className="nav-section-title">내 프로필</div>
            <NavLink to="/profile" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5Z" />
                </svg>
              </SidebarIcon>
              <span>내 프로필</span>
            </NavLink>
            <NavLink to="/profile/my-info" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M7 3h8l4 4v14H7V3Zm7 1.5V8h3.5" />
                  <path d="M10 12h6M10 16h6" />
                </svg>
              </SidebarIcon>
              <span>내 정보</span>
            </NavLink>
            <NavLink to="/profile/account-links" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M10.59 13.41a1.996 1.996 0 0 0 2.82 0l3.59-3.59a2 2 0 1 0-2.83-2.83l-1.29 1.29" />
                  <path d="M13.41 10.59a1.996 1.996 0 0 0-2.82 0L7 14.18a2 2 0 0 0 2.83 2.83l1.29-1.29" />
                </svg>
              </SidebarIcon>
              <span>외부 계정 연동</span>
            </NavLink>
          </div>

          <div className="nav-section">
            <div className="nav-section-title profile-sidebar-label-upper">GAMEMATCHER</div>
            <Link to="/studio" className="nav-item">
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="M8 3v4M16 3v4M3 10h18" />
                </svg>
              </SidebarIcon>
              <span>스튜디오</span>
            </Link>
            <Link to="/channel" className="nav-item">
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M4 6h16v12H4z" />
                  <path d="M8 4v4M16 4v4" />
                </svg>
              </SidebarIcon>
              <span>내 채널</span>
            </Link>
            <NavLink to="/profile/pang" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M12 2 8 9h3l-1 7 6-9h-3l2-5z" />
                </svg>
              </SidebarIcon>
              <span>내 팡</span>
            </NavLink>
            <NavLink to="/profile/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="m12 17 5 3-1-6 4-4-6-.9L12 3 10 9.1 4 10l4 4-1 6 5-3z" />
                </svg>
              </SidebarIcon>
              <span>내 구독</span>
            </NavLink>
            <NavLink to="/profile/adfree" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M5 5l14 14" />
                </svg>
              </SidebarIcon>
              <span>광고 제거</span>
            </NavLink>
            <NavLink to="/profile/mileage-shop" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M4 7h16v10H4z" />
                  <path d="M8 11h8M8 15h5" />
                </svg>
              </SidebarIcon>
              <span>마일리지 상점</span>
            </NavLink>
            <NavLink to="/history" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v6l4 2" />
                </svg>
              </SidebarIcon>
              <span>히스토리</span>
            </NavLink>
            <NavLink to="/following" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M12.1 20.3 4.8 13a4.5 4.5 0 0 1 6.4-6.4l.8.8.8-.8a4.5 4.5 0 1 1 6.4 6.4l-7.1 7.3z" />
                </svg>
              </SidebarIcon>
              <span>팔로잉 채널</span>
            </NavLink>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">게임 커뮤니티</div>
            <NavLink to="/community" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M5 13l4 4L19 7" />
                </svg>
              </SidebarIcon>
              <span>커뮤니티</span>
            </NavLink>
            <NavLink to="/profile/my-posts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M7 3h8l4 4v14H7V3Zm7 1.5V8h3.5" />
                  <path d="M10 12h6M10 16h6" />
                </svg>
              </SidebarIcon>
              <span>작성한 글</span>
            </NavLink>
            <NavLink to="/profile/saved-posts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M6 3h12v18l-6-4-6 4V3z" />
                </svg>
              </SidebarIcon>
              <span>저장한 글</span>
            </NavLink>
          </div>

          <div className="nav-section">
            <button type="button" className="nav-item" onClick={handleLogout}>
              <SidebarIcon>
                <svg viewBox="0 0 24 24">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <path d="M16 17l5-5-5-5" />
                  <path d="M21 12H9" />
                </svg>
              </SidebarIcon>
              <span>로그아웃</span>
            </button>
          </div>
        </aside>

        <main className="profile-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
