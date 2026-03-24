import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';

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

  if (!user) {
    return (
      <div className="profile-page">
        <header className="header">
          <Link to="/" className="header-logo">
            GameMatcher
          </Link>
          <nav className="header-nav">
            <Link to="/streams">전체 방송</Link>
            <Link to="/streams">게임</Link>
            <Link to="/esports">e스포츠</Link>
          </nav>
          <input type="text" className="header-search" placeholder="채널, 라이브 검색" />
          <div className="header-right">
            <button type="button" className="btn-theme" onClick={toggleTheme} aria-label="테마 전환">
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
          <p>로그인하면 프로필을 확인할 수 있습니다.</p>
          <Link to="/login">로그인하기</Link>
        </div>
      </div>
    );
  }

  const profileImageUrl = !profileImgError ? resolveProfileImageUrl(user.profileImageUrl) : null;

  return (
    <div className="profile-page">
      <header className="header">
        <Link to="/" className="header-logo">
          GameMatcher
        </Link>
        <nav className="header-nav">
          <Link to="/streams">전체 방송</Link>
          <Link to="/streams">게임</Link>
          <Link to="/esports">e스포츠</Link>
        </nav>
        <input type="text" className="header-search" placeholder="채널, 라이브 검색" />
        <div className="header-right">
          <button type="button" className="btn-theme" onClick={toggleTheme} aria-label="테마 전환">
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
              {profileImageUrl ? (
                <img src={profileImageUrl} alt="" onError={() => setProfileImgError(true)} />
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
                    {profileImageUrl ? (
                      <img src={profileImageUrl} alt="" onError={() => setProfileImgError(true)} />
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
                    <div className="dropdown-profile-name">{user.nickname ?? user.username ?? user.loginId ?? '사용자'}</div>
                  </div>
                </div>
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>
                    내 프로필
                  </Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>
                    스튜디오
                  </Link>
                  {user.role === 'ADMIN' ? (
                    <Link to="/admin" onClick={() => setProfileOpen(false)}>
                      관리
                    </Link>
                  ) : null}
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
              <span className="icon">👤</span> 내 프로필
            </NavLink>
            <NavLink to="/profile/my-info" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">🧾</span> 내 정보
            </NavLink>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">GAMEMATCHER</div>
            <Link to="/studio" className="nav-item">
              <span className="icon">📺</span> 스튜디오
            </Link>
            <Link to="/channel" className="nav-item">
              <span className="icon">📋</span> 내 채널
            </Link>
            <NavLink to="/profile/pang" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">💰</span> 내 팡
            </NavLink>
            <NavLink to="/profile/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">⭐</span> 내 구독
            </NavLink>
            <NavLink to="/profile/adfree" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">🚫</span> 광고제거
            </NavLink>
            <NavLink to="/profile/mileage-shop" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">🛒</span> 마일리지 상점
            </NavLink>
            <button type="button" className="nav-item nav-item-disabled" onClick={() => alert('준비 중인 기능입니다.')}>
              <span className="icon">🕒</span> 최근 시청 영상
            </button>
            <Link to="/following" className="nav-item">
              <span className="icon">🖤</span> 팔로잉 채널
            </Link>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">게임 커뮤니티</div>
            <Link to="/community" className="nav-item">
              <span className="icon">✔</span> 커뮤니티
            </Link>
            <NavLink to="/profile/my-posts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">📄</span> 작성한 글
            </NavLink>
            <NavLink to="/profile/saved-posts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">📌</span> 저장한 글
            </NavLink>
          </div>

          <div className="nav-section">
            <div className="nav-section-title">e스포츠</div>
            <NavLink
              to="/profile/esports-predictions"
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="icon">📊</span> 승부 예측
            </NavLink>
            <NavLink to="/profile/esports-rewards" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="icon">🏆</span> 당첨 보관함
            </NavLink>
          </div>

          <div className="nav-section">
            <Link to="/streams" className="nav-item">
              <span className="icon">⚙</span> 설정
            </Link>
            <button type="button" className="nav-item" onClick={handleLogout}>
              <span className="icon">🚪</span> 로그아웃
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
