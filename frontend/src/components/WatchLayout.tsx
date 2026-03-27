import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { resolveNotificationTargetPath } from '../utils/notificationNavigation';

interface WatchLayoutProps {
  children: React.ReactNode;
}

type NotificationItem = {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
  streamId?: number;
  actorUserId?: number;
  actorNickname?: string;
  targetPath?: string;
};

export default function WatchLayout({ children }: WatchLayoutProps) {
  const { user, logout } = useAuth();
  const { toggleTheme } = useTheme();
  const navigate = useNavigate();
  const isAdmin = ['ADMIN', 'ROLE_ADMIN'].includes((user?.role ?? '').toUpperCase());

  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [hasUnread, setHasUnread] = useState(false);
  const [notificationList, setNotificationList] = useState<NotificationItem[]>([]);
  const [notificationListLoading, setNotificationListLoading] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);

  const profileRef = useRef<HTMLDivElement>(null);
  const notificationRef = useRef<HTMLSpanElement>(null);

  const fetchNotificationCount = useCallback(() => {
    fetch(apiUrl('api/notifications/count'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { count: 0 }))
      .then((d: { count?: number }) => setHasUnread((d.count ?? 0) > 0))
      .catch(() => setHasUnread(false));
  }, []);

  const fetchNotificationList = useCallback(() => {
    setNotificationListLoading(true);
    fetch(apiUrl('api/notifications?limit=30'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: NotificationItem[] }) => setNotificationList(Array.isArray(d.list) ? d.list : []))
      .catch(() => setNotificationList([]))
      .finally(() => setNotificationListLoading(false));
  }, []);

  const handleMarkAllNotificationsRead = useCallback(() => {
    fetch(apiUrl('api/notifications/read-all'), { method: 'POST', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          setHasUnread(false);
          fetchNotificationList();
        }
      })
      .catch(() => {});
  }, [fetchNotificationList]);

  const handleDeleteAllNotifications = useCallback(() => {
    fetch(apiUrl('api/notifications'), { method: 'DELETE', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          setNotificationList([]);
          setHasUnread(false);
        }
      })
      .catch(() => {});
  }, []);

  const handleNotificationClick = useCallback((notification: NotificationItem) => {
    setNotificationOpen(false);
    if (!notification.read) {
      fetch(apiUrl(`api/notifications/${notification.id}/read`), { method: 'PATCH', credentials: 'include' })
        .then(() => {
          setNotificationList((prev) => prev.map((item) => (item.id === notification.id ? { ...item, read: true } : item)));
          fetchNotificationCount();
        })
        .catch(() => {});
    }

    const targetPath = resolveNotificationTargetPath(notification);
    if (targetPath) {
      navigate(targetPath);
    }
  }, [fetchNotificationCount, navigate]);

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  useEffect(() => {
    if (!user) {
      setHasUnread(false);
      return;
    }
    fetchNotificationCount();
  }, [user, fetchNotificationCount]);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
      if (notificationRef.current && !notificationRef.current.contains(event.target as Node)) {
        setNotificationOpen(false);
      }
    };

    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const handleLogout = async () => {
    await logout();
    setProfileOpen(false);
    navigate('/streams');
  };

  return (
    <div className="watch-page">
      <header className="header">
        <Link to="/" className="header-logo">GameMatcher</Link>
        <nav className="header-nav">
          <Link to="/streams">전체 방송</Link>
          <Link to="/streams">게임</Link>
          <Link to="/studio" className="auth-only">스튜디오</Link>
        </nav>

        <input type="text" className="header-search" placeholder="채널, 라이브 검색" />

        <div className="header-right">
          <span className={`header-icon-wrap auth-only ${hasUnread ? 'has-unread' : ''}`} ref={notificationRef} title="알림">
            <button
              type="button"
              className="header-icon-btn"
              onClick={() => {
                setNotificationOpen((open) => {
                  const next = !open;
                  if (next) {
                    fetchNotificationList();
                  }
                  return next;
                });
              }}
            >
              <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            </button>

            {notificationOpen && (
              <div className="notification-dropdown">
                <div className="notification-dropdown-head">
                  <strong>알림</strong>
                  <div className="notification-actions">
                    <button type="button" className="notification-read-all" onClick={handleMarkAllNotificationsRead}>전체 읽음</button>
                    <button type="button" className="notification-delete-all" onClick={handleDeleteAllNotifications}>전체 삭제</button>
                  </div>
                </div>

                {notificationListLoading ? (
                  <div className="notification-empty">불러오는 중...</div>
                ) : notificationList.length === 0 ? (
                  <div className="notification-empty">알림이 없습니다.</div>
                ) : (
                  notificationList.map((notification) => (
                    <button
                      key={notification.id}
                      type="button"
                      className={`notification-item ${!notification.read ? 'unread' : ''}`}
                      onClick={() => handleNotificationClick(notification)}
                    >
                      <span>{notification.message}</span>
                      <div className="notification-time">
                        {notification.createdAt ? new Date(notification.createdAt).toLocaleString('ko-KR') : ''}
                      </div>
                    </button>
                  ))
                )}
              </div>
            )}
          </span>

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

          <Link to="/login" className="btn-login no-auth">로그인</Link>

          <div className="header-profile-wrap auth-only" ref={profileRef}>
            <button type="button" className="header-profile-avatar" onClick={() => setProfileOpen((open) => !open)} aria-label="프로필">
              {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? (
                <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} />
              ) : (
                <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                </svg>
              )}
            </button>

            {profileOpen && (
              <div className="header-profile-dropdown show">
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>내 프로필</Link>
                  {isAdmin && <Link to="/admin" onClick={() => setProfileOpen(false)}>관리자</Link>}
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>스튜디오</Link>
                  <button type="button" onClick={handleLogout}>로그아웃</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <div className="watch-layout">
        <aside className="left-sidebar">
          <Link to="/streams" className="nav-link">전체 방송</Link>
          <Link to="/following" className="nav-link">팔로잉</Link>
          <Link to="/profile" className="nav-link">내 프로필</Link>
        </aside>
        <main className="watch-main">{children}</main>
      </div>
    </div>
  );
}
