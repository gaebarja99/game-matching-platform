import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

interface StreamsLayoutProps {
  children: React.ReactNode;
  sidebarVariant?: 'full' | 'simple';
}

type NotificationItem = {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
};

type RankItem = {
  rank?: number;
  displayName?: string;
  count?: number;
  totalPang?: number;
  viewerCount?: number;
};

export default function StreamsLayout({ children, sidebarVariant = 'full' }: StreamsLayoutProps) {
  const { user, logout, loading: authLoading } = useAuth();
  const { toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  const [categoryOpen, setCategoryOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [hasUnread, setHasUnread] = useState(false);
  const [notificationList, setNotificationList] = useState<NotificationItem[]>([]);
  const [notificationListLoading, setNotificationListLoading] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);

  const [followRank, setFollowRank] = useState<RankItem[]>([]);
  const [pangRank, setPangRank] = useState<RankItem[]>([]);
  const [viewerRank, setViewerRank] = useState<RankItem[]>([]);

  const profileRef = useRef<HTMLDivElement>(null);
  const notificationRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    if (authLoading) return;
    Promise.all([
      fetch(apiUrl('api/rank/follow'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
      fetch(apiUrl('api/rank/pang'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
      fetch(apiUrl('api/rank/viewers'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
    ])
      .then(([follow, pang, viewers]) => {
        setFollowRank(Array.isArray(follow) ? follow : []);
        setPangRank(Array.isArray(pang) ? pang : []);
        setViewerRank(Array.isArray(viewers) ? viewers : []);
      })
      .catch(() => {});
  }, [authLoading]);

  const fetchNotificationCount = useCallback(() => {
    fetch(apiUrl('api/notifications/count'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { count: 0 }))
      .then((d: { count?: number }) => {
        const c = d.count != null ? Number(d.count) : 0;
        setHasUnread(c > 0);
      })
      .catch(() => setHasUnread(false));
  }, []);

  const fetchNotificationList = useCallback(() => {
    setNotificationListLoading(true);
    fetch(apiUrl('api/notifications?limit=30'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: NotificationItem[] }) => setNotificationList(Array.isArray(d?.list) ? d.list : []))
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

  useEffect(() => {
    if (!user) {
      setHasUnread(false);
      return;
    }
    fetchNotificationCount();
  }, [user, fetchNotificationCount]);

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) setProfileOpen(false);
      if (notificationRef.current && !notificationRef.current.contains(e.target as Node)) setNotificationOpen(false);
    };
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const handleLogout = async () => {
    await logout();
    setProfileOpen(false);
    navigate('/streams');
  };

  const pangIconUrl = apiUrl('images/pang-sparkle.svg');

  const rankRow = (name: string | undefined, value: string | number | undefined, idx: number) => (
    <li className="sidebar-rank-item" key={`${name ?? 'user'}-${idx}`}>
      <span className="rank-num">{idx + 1}</span>
      <span className="rank-name">{name ?? '유저'}</span>
      <span className="rank-value">{value ?? 0}</span>
    </li>
  );

  return (
    <div className="streams-page">
      <header className="header">
        <Link to="/" className="header-logo">GameMatcher</Link>

        <nav className="header-nav">
          <Link to="/streams" className={location.pathname === '/streams' ? 'active' : ''}>전체 방송</Link>
          <Link to="/streams">게임</Link>
          <Link to="/esports" className={location.pathname === '/esports' ? 'active' : ''}>e스포츠</Link>
        </nav>

        <input type="text" className="header-search" placeholder="채널, 라이브 검색" />

        <div className="header-right">
          <Link to="/studio" className="header-icon-btn auth-only" title="스튜디오" aria-label="스튜디오">
            <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
              <rect x="2" y="4" width="14" height="14" rx="2" />
              <path d="M17 8v8l5-4z" />
            </svg>
          </Link>

          <Link to="/profile/pang" className="header-icon-btn auth-only header-icon-pang" title="내 팡" aria-label="내 팡">
            <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
              <path d="M12 3l1.7 4.3L18 9l-4.3 1.7L12 15l-1.7-4.3L6 9l4.3-1.7L12 3z" />
              <path d="M19 14l.9 2.1L22 17l-2.1.9L19 20l-.9-2.1L16 17l2.1-.9L19 14z" />
            </svg>
          </Link>

          <span className={`header-icon-wrap auth-only ${hasUnread ? 'has-unread' : ''}`} ref={notificationRef} title="알림">
            <button
              type="button"
              className="header-icon-btn"
              onClick={() => {
                setNotificationOpen((o) => {
                  const next = !o;
                  if (next) fetchNotificationList();
                  return next;
                });
              }}
              aria-label="알림"
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
                  notificationList.map((n) => (
                    <button
                      key={n.id}
                      type="button"
                      className={`notification-item ${!n.read ? 'unread' : ''}`}
                      onClick={() => {
                        setNotificationOpen(false);
                        if (!n.read) {
                          fetch(apiUrl(`api/notifications/${n.id}/read`), { method: 'PATCH', credentials: 'include' })
                            .then(() => {
                              setNotificationList((prev) => prev.map((item) => (item.id === n.id ? { ...item, read: true } : item)));
                              fetchNotificationCount();
                            })
                            .catch(() => {});
                        }
                      }}
                    >
                      <span>{n.message}</span>
                      <div className="notification-time">{n.createdAt ? new Date(n.createdAt).toLocaleString('ko-KR') : ''}</div>
                    </button>
                  ))
                )}
              </div>
            )}
          </span>

          <Link to="/profile/adfree" className="header-icon-btn auth-only" title="광고 제거" aria-label="광고 제거">
            <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
              <circle cx="12" cy="12" r="9" />
              <path d="M5 5l14 14" />
            </svg>
          </Link>

          <span className="header-divider auth-only" />

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
            <button type="button" className="header-profile-avatar" onClick={() => setProfileOpen((o) => !o)} aria-label="프로필 메뉴">
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
                <div className="dropdown-profile-info">
                  <div className="dropdown-avatar">
                    {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? (
                      <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} />
                    ) : (
                      <svg className="avatar-placeholder" style={{ width: 32, height: 32, margin: 8 }} viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                      </svg>
                    )}
                  </div>
                  <div>
                    <div className="dropdown-profile-label">GameMatcher 프로필</div>
                    <div className="dropdown-profile-name">{user?.nickname ?? user?.username ?? user?.loginId ?? '유저'}</div>
                  </div>
                </div>
                <div className="dropdown-menu">
                  <Link to="/profile" onClick={() => setProfileOpen(false)}>내 프로필</Link>
                  <Link to="/studio" onClick={() => setProfileOpen(false)}>스튜디오</Link>
                  <Link to="/channel" onClick={() => setProfileOpen(false)}>내 채널</Link>
                  <button type="button" onClick={handleLogout}>로그아웃</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <div className="layout">
        <aside className="sidebar">
          <NavLink to="/streams" end className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}>
            <span className="icon">📺</span>
            <span>전체 방송</span>
          </NavLink>
          <Link to="/streams?sort=popular" className="sidebar-item"><span className="icon">🔥</span><span>인기 채널</span></Link>

          {sidebarVariant === 'full' && (
            <>
              <div className={`sidebar-category-wrap ${categoryOpen ? 'open' : ''}`}>
                <button type="button" className="sidebar-category-toggle" onClick={() => setCategoryOpen((o) => !o)}>
                  <span className="icon">🎮</span>
                  <span>게임 카테고리</span>
                  <span className="arrow">▶</span>
                </button>
                <ul className="sidebar-sub">
                  <li><Link to="/streams?game=LEAGUE_OF_LEGENDS" className="sidebar-item sidebar-item-sub">리그 오브 레전드</Link></li>
                  <li><Link to="/streams?game=VALORANT" className="sidebar-item sidebar-item-sub">발로란트</Link></li>
                  <li><Link to="/streams?game=OVERWATCH" className="sidebar-item sidebar-item-sub">오버워치2</Link></li>
                  <li><Link to="/streams?game=PUBG" className="sidebar-item sidebar-item-sub">PUBG</Link></li>
                </ul>
              </div>

              <NavLink to="/following" className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}><span className="icon">♥</span><span>팔로잉</span></NavLink>
              <NavLink to="/history" className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}><span className="icon">🕒</span><span>시청 기록</span></NavLink>

              <div className="sidebar-divider" />
              <div className="sidebar-title">RANKING</div>

              <div className="sidebar-rank-block">
                <div className="sidebar-rank-title"><span className="icon">👥</span> 팔로워 순위</div>
                {followRank.length > 0 ? <ul className="sidebar-rank-list">{followRank.slice(0, 5).map((r, i) => rankRow(r.displayName, r.count, i))}</ul> : <div className="sidebar-rank-empty">데이터 없음</div>}
              </div>

              <div className="sidebar-rank-block">
                <div className="sidebar-rank-title"><img src={pangIconUrl} alt="" className="pang-icon" /> 팡 후원 순위</div>
                {pangRank.length > 0 ? <ul className="sidebar-rank-list">{pangRank.slice(0, 5).map((r, i) => rankRow(r.displayName, r.totalPang, i))}</ul> : <div className="sidebar-rank-empty">데이터 없음</div>}
              </div>

              <div className="sidebar-rank-block">
                <div className="sidebar-rank-title"><span className="icon">👁</span> 시청자 순위</div>
                {viewerRank.length > 0 ? <ul className="sidebar-rank-list">{viewerRank.slice(0, 5).map((r, i) => rankRow(r.displayName, r.viewerCount, i))}</ul> : <div className="sidebar-rank-empty">데이터 없음</div>}
              </div>
            </>
          )}
        </aside>

        <main className="content">{children}</main>
      </div>
    </div>
  );
}
