import { useState, useRef, useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { resolveProfileImageUrl } from '../api/client';

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
    const close = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) setProfileOpen(false);
    };
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const handleLogout = async () => {
    await logout();
    setProfileOpen(false);
    navigate('/');
  };

  if (!user) {
    return (
      <div className="profile-page">
        <header className="header">
          <Link to="/" className="header-logo">GameMatcher</Link>
          <nav className="header-nav">
            <Link to="/streams">전체 방송</Link>
            <Link to="/streams">게임</Link>
            <Link to="/streams">e스포츠</Link>
          </nav>
          <input type="text" className="header-search" placeholder="채널, 라이브 검색" />
          <div className="header-right">
            <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
              <span className="theme-icon">
                <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
                <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
              </span>
            </button>
            <Link to="/login" className="btn-login no-auth">로그인</Link>
          </div>
        </header>
        <div className="profile-login-msg">
          <p>로그인하면 내 프로필을 확인할 수 있습니다.</p>
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
          <Link to="/streams">게임</Link>
          <Link to="/streams">e스포츠</Link>
        </nav>
        <input type="text" className="header-search" placeholder="채널, 라이브 검색" />
        <div className="header-right">
          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            </span>
          </button>
          <Link to="/login" className="btn-login no-auth">로그인</Link>
          <div className="header-profile-wrap auth-only" ref={profileRef}>
            <button type="button" className="header-profile-avatar" onClick={() => setProfileOpen((o) => !o)} aria-label="프로필 메뉴">
              {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} /> : <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>}
            </button>
            {profileOpen && (
              <div className="header-profile-dropdown show">
                <div className="dropdown-profile-info">
                  <div className="dropdown-avatar">
                    {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} /> : <svg className="avatar-placeholder" style={{ width: 32, height: 32, margin: 8 }} viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>}
                  </div>
                  <div>
                    <div className="dropdown-profile-label">GameMatcher 프로필</div>
                    <div className="dropdown-profile-name">{user?.nickname ?? user?.username ?? user?.loginId ?? '—'}</div>
                  </div>
                </div>
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}><svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> 내 프로필</Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}><svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><rect x="2" y="4" width="14" height="14" rx="2"/><path d="M17 8v8l5-4z"/></svg> 스튜디오</Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}><svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg> 내 채널</Link>
                  <Link to="/following" onClick={() => setProfileOpen(false)}><svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 18.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg> 팔로잉 채널</Link>
                  <button type="button" onClick={handleLogout}><svg className="menu-icon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg> 로그아웃</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>
      <div className="layout">
        <aside className="profile-sidebar">
          <div className="nav-section">
            <div className="nav-section-title">내 프로필</div>
            <NavLink to="/profile" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">👤</span> 내 프로필</NavLink>
            <NavLink to="/profile/my-info" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">📋</span> 내 정보</NavLink>
          </div>
          <div className="nav-section">
            <div className="nav-section-title">GAMEMATCHER</div>
            <Link to="/studio" className="nav-item"><span className="icon">📹</span> 스튜디오</Link>
            <Link to="/channel" className="nav-item"><span className="icon">🖥</span> 내 채널</Link>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">✂</span> 내 클립</button>
            <NavLink to="/profile/pang" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">💰</span> 내 팡</NavLink>
            <NavLink to="/profile/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">⭐</span> 내 구독</NavLink>
            <NavLink to="/profile/adfree" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">🚫</span> 광고제거</NavLink>
            <NavLink to="/profile/mileage-shop" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><span className="icon">🛒</span> 마일리지 상점</NavLink>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">🕐</span> 최근 시청 영상</button>
            <Link to="/following" className="nav-item"><span className="icon">♥</span> 팔로잉 채널</Link>
          </div>
          <div className="nav-section">
            <div className="nav-section-title">게임 라운지</div>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">✓</span> 가입 라운지</button>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">📄</span> 작성한 글</button>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">🔖</span> 저장한 글</button>
          </div>
          <div className="nav-section">
            <div className="nav-section-title">e스포츠</div>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">🪙</span> 승부 예측</button>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}><span className="icon">🏆</span> 당첨 보관함</button>
          </div>
          <div className="nav-section">
            <Link to="/streams" className="nav-item"><span className="icon">⚙</span> 설정</Link>
            <button type="button" className="nav-item" onClick={handleLogout}><span className="icon">🚪</span> 로그아웃</button>
          </div>
        </aside>
        <main className="profile-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
