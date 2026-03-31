import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';

interface ChannelLayoutProps {
  children: React.ReactNode;
}

export default function ChannelLayout({ children }: ChannelLayoutProps) {
  const { toggleTheme } = useTheme();
  const { user } = useAuth();
  const [profileImgError, setProfileImgError] = useState(false);

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  return (
    <div className="channel-page">
      <header className="header">
        <Link to="/" className="header-logo">GameMatcher</Link>
        <nav className="header-nav">
          <Link to="/streams">방송</Link>
          <Link to="/streams">게임</Link>
          <Link to="/channel" className="active">내 채널</Link>
        </nav>
        <div className="header-right">
          {user && (
            <Link
              to="/studio"
              className="auth-only"
              style={{ padding: '8px 14px', borderRadius: 8, fontSize: '0.9rem', fontWeight: 500, border: '1px solid #00e676', color: '#00e676', textDecoration: 'none' }}
            >
              스튜디오
            </Link>
          )}
          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" /></svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" /></svg>
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
      <main className="channel-main">{children}</main>
    </div>
  );
}
