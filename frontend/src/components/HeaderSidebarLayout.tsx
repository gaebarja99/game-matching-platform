import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useAuth } from '../contexts/AuthContext';
import { resolveProfileImageUrl } from '../api/client';

interface NavItem {
  to: string;
  label: string;
  icon: string;
  active?: boolean;
}

interface HeaderSidebarLayoutProps {
  children: React.ReactNode;
  pageTitle: string;
  sidebarItems: NavItem[];
}

export default function HeaderSidebarLayout({ children, pageTitle, sidebarItems }: HeaderSidebarLayoutProps) {
  const { toggleTheme } = useTheme();
  const { user } = useAuth();
  const [profileImgError, setProfileImgError] = useState(false);

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  return (
    <div className="streams-page">
      <header className="header">
        <Link to="/" className="header-logo">GameMatcher</Link>
        <nav className="header-nav">
          <Link to="/streams" className={sidebarItems.some((i) => i.to === '/streams') ? 'active' : ''}>전체 방송</Link>
          <Link to="/streams">게임</Link>
        </nav>
        <input type="text" className="header-search" placeholder="채널, 라이브, 영상 검색" />
        <div className="header-right">
          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            </span>
          </button>
          <Link to="/login" className="btn-login no-auth">로그인</Link>
          {user && (
            <div className="header-profile-wrap auth-only">
              <Link to="/profile" className="header-profile-avatar" aria-label="프로필">
                {resolveProfileImageUrl(user.profileImageUrl) && !profileImgError ? <img src={resolveProfileImageUrl(user.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} /> : <span>{(user.nickname ?? user.username ?? '?')[0]}</span>}
              </Link>
            </div>
          )}
        </div>
      </header>
      <div className="layout">
        <aside className="sidebar">
          <Link to="/streams" className={`sidebar-item ${sidebarItems.some((i) => i.to === '/streams') ? 'active' : ''}`}><span className="icon">📹</span> 신규 방송</Link>
          <Link to="/streams?sort=popular" className="sidebar-item"><span className="icon">🔥</span> 인기</Link>
          <Link to="/following" className={`sidebar-item ${sidebarItems.some((i) => i.to === '/following') ? 'active' : ''}`}><span className="icon">♥</span> 팔로잉</Link>
          <Link to="/history" className={`sidebar-item ${sidebarItems.some((i) => i.to === '/history') ? 'active' : ''}`}><span className="icon">🕐</span> 히스토리</Link>
        </aside>
        <main className="content">
          <h1 className="page-title">{pageTitle}</h1>
          {children}
        </main>
      </div>
    </div>
  );
}
