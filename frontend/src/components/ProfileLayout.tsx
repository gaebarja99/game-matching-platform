import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { resolveProfileImageUrl } from '../api/client';

function getDisplayName(user: ReturnType<typeof useAuth>['user']): string {
  return user?.nickname?.trim() || user?.username?.trim() || user?.loginId?.trim() || '사용자';
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
          <Link to="/" className="header-logo">GameMatcher</Link>
          <nav className="header-nav">
            <Link to="/streams">전체 방송</Link>
            <Link to="/records">전적 검색</Link>
            <Link to="/community">커뮤니티</Link>
          </nav>
          <div className="header-right">
            <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
              <span className="theme-icon">
                <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" /></svg>
                <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" /></svg>
              </span>
            </button>
            <Link to="/login" className="btn-login no-auth">로그인</Link>
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
        <Link to="/" className="header-logo">GameMatcher</Link>
        <nav className="header-nav">
          <Link to="/streams">전체 방송</Link>
          <Link to="/records">전적 검색</Link>
          <Link to="/community">커뮤니티</Link>
        </nav>
        <div className="header-right">
          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" /></svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" /></svg>
            </span>
          </button>
          <div className="header-profile-wrap auth-only" ref={profileRef}>
            <button type="button" className="header-profile-avatar" onClick={() => setProfileOpen((open) => !open)} aria-label="프로필 메뉴">
              {profileImage && !profileImgError ? (
                <img src={profileImage} alt="" onError={() => setProfileImgError(true)} />
              ) : (
                <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" /></svg>
              )}
            </button>
            {profileOpen ? (
              <div className="header-profile-dropdown show">
                <div className="dropdown-profile-info">
                  <div className="dropdown-avatar">
                    {profileImage && !profileImgError ? (
                      <img src={profileImage} alt="" onError={() => setProfileImgError(true)} />
                    ) : (
                      <svg className="avatar-placeholder" style={{ width: 32, height: 32, margin: 8 }} viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" /></svg>
                    )}
                  </div>
                  <div>
                    <div className="dropdown-profile-label">GameMatcher 프로필</div>
                    <div className="dropdown-profile-name">{displayName}</div>
                  </div>
                </div>
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>프로필</Link>
                  <Link to="/profile/account-links" onClick={() => setProfileOpen(false)}>외부 계정 연동</Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>스튜디오</Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}>내 채널</Link>
                  <Link to="/following" onClick={() => setProfileOpen(false)}>팔로잉</Link>
                  <button type="button" onClick={handleLogout}>로그아웃</button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <div className="layout">
        <aside className="profile-sidebar">
          <div className="nav-section">
            <div className="nav-section-title">프로필</div>
            <NavLink to="/profile" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">P</span> 프로필 홈
            </NavLink>
            <NavLink to="/profile/my-info" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">M</span> 내 정보
            </NavLink>
            <NavLink to="/profile/account-links" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">L</span> 외부 계정 연동
            </NavLink>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">GameMatcher</div>
            <Link to="/studio" className="nav-item"><span className="icon">S</span> 스튜디오</Link>
            <Link to="/channel" className="nav-item"><span className="icon">C</span> 내 채널</Link>
            <NavLink to="/profile/pang" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">P</span> 팡</NavLink>
            <NavLink to="/profile/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">U</span> 구독</NavLink>
            <NavLink to="/profile/adfree" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">A</span> 광고 제거</NavLink>
            <NavLink to="/profile/mileage-shop" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">M</span> 마일리지 상점</NavLink>
            <Link to="/following" className="nav-item"><span className="icon">F</span> 팔로잉</Link>
          </div>

          <div className="nav-section">
            <button type="button" className="nav-item" onClick={handleLogout}>
              <span className="icon">O</span> 로그아웃
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
