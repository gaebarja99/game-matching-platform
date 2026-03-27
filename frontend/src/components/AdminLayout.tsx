import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import '../styles/admin-source.css';

interface AdminLayoutProps {
  title: string;
  description?: string;
  children: React.ReactNode;
}

const adminNavItems = [
  { to: '/admin', label: '\uB300\uC2DC\uBCF4\uB4DC', exact: true },
  { to: '/admin/streamers', label: '\uC2A4\uD2B8\uB9AC\uBA38 \uAD00\uB9AC' },
  { to: '/admin/broadcasts', label: '\uBC29\uC1A1 \uAD00\uB9AC' },
  { to: '/admin/community', label: '\uCEE4\uBBA4\uB2C8\uD2F0 \uAD00\uB9AC' },
  { to: '/admin/match-rooms', label: '\uB9E4\uCE6D\uBC29 \uAD00\uB9AC' },
  { to: '/admin/reports', label: '\uC2E0\uACE0 \uAD00\uB9AC' },
  { to: '/admin/members', label: '\uD68C\uC6D0 \uAD00\uB9AC' },
  { to: '/admin/settlements', label: '\uC815\uC0B0 \uAD00\uB9AC' },
  { to: '/admin/revenue', label: '\uB9E4\uCD9C \uAD00\uB9AC' },
];

export default function AdminLayout({ title, description, children }: AdminLayoutProps) {
  const { toggleTheme } = useTheme();
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const profileRef = useRef<HTMLDivElement>(null);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);

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
    if (user === null) return;
    if (user.role !== 'ADMIN') {
      navigate('/studio', { replace: true });
    }
  }, [navigate, user]);

  const profileImageUrl = useMemo(() => {
    if (profileImgError) return null;
    return resolveProfileImageUrl(user?.profileImageUrl);
  }, [profileImgError, user?.profileImageUrl]);

  const handleLogout = async () => {
    await logout();
    navigate('/streams', { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <Link to="/admin" className="admin-brand">
          <span className="admin-brand-kicker">GameMatcher</span>
          <strong>{'\uAD00\uB9AC\uC790 \uC13C\uD130'}</strong>
        </Link>
        <nav className="admin-nav" aria-label={'\uAD00\uB9AC\uC790 \uBA54\uB274'}>
          {adminNavItems.map((item) => {
            const active = item.exact ? location.pathname === item.to : location.pathname.startsWith(item.to);
            return (
              <Link key={item.to} to={item.to} className={`admin-nav-link ${active ? 'active' : ''}`}>
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="admin-sidebar-footer">
          <Link to="/studio" className="admin-sidebar-link">
            {'\uC2A4\uD29C\uB514\uC624\uB85C \uB3CC\uC544\uAC00\uAE30'}
          </Link>
          <Link to="/streams" className="admin-sidebar-link">
            {'\uC11C\uBE44\uC2A4 \uBCF4\uB7EC\uAC00\uAE30'}
          </Link>
        </div>
      </aside>

      <div className="admin-main-shell">
        <header className="admin-topbar">
          <div>
            <p className="admin-topbar-kicker">{'\uC6B4\uC601\uC790 \uC804\uC6A9'}</p>
            <h1>{title}</h1>
            {description ? <p className="admin-topbar-desc">{description}</p> : null}
          </div>
          <div className="admin-topbar-actions">
            <button type="button" className="admin-icon-btn" onClick={toggleTheme} aria-label={'\uD14C\uB9C8 \uC804\uD658'}>
              <svg className="icon-sun" viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
                <circle cx={12} cy={12} r={4} />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
              <svg className="icon-moon" viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            </button>
            <div className="admin-profile-wrap" ref={profileRef}>
              <button
                type="button"
                className="admin-profile-btn"
                onClick={() => setProfileOpen((open) => !open)}
                aria-label={'\uAD00\uB9AC\uC790 \uD504\uB85C\uD544 \uBA54\uB274'}
              >
                <span className="admin-profile-copy">
                  <strong>{user?.nickname ?? user?.username ?? user?.loginId ?? '\uAD00\uB9AC\uC790'}</strong>
                  <small>{user?.role ?? 'ADMIN'}</small>
                </span>
                <span className="admin-profile-avatar">
                  {profileImageUrl ? (
                    <img src={profileImageUrl} alt="" onError={() => setProfileImgError(true)} />
                  ) : (
                    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
                      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                    </svg>
                  )}
                </span>
              </button>
              {profileOpen ? (
                <div className="admin-profile-dropdown">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>
                    {'\uB0B4 \uD504\uB85C\uD544'}
                  </Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>
                    {'\uC2A4\uD29C\uB514\uC624'}
                  </Link>
                  <button type="button" onClick={handleLogout}>
                    {'\uB85C\uADF8\uC544\uC6C3'}
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </header>

        <main className="admin-content">{children}</main>
      </div>
    </div>
  );
}
