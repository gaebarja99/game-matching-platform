import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import FloatingChatWidget from './FloatingChatWidget';
import { useMatchCompleteNotification } from '../hooks/useMatchCompleteNotification';
import { useAutoResizeTextarea } from '../hooks/useAutoResizeTextarea';
import { filterFriendsExcludingSelf, isDmWithSelf } from '../utils/dmSelf';

interface LayoutProps {
  children: React.ReactNode;
  showFriendSidebar?: boolean;
  topSection?: React.ReactNode;
}

type NotificationItem = {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
};

type DmMessageItem = {
  id?: number;
  fromUserId: number;
  toUserId: number;
  text: string;
  createdAt?: string;
};

type FriendItem = {
  id: number;
  loginId: string;
  nickname: string;
  profileImageUrl?: string | null;
};

type FriendRequestItem = {
  id: number;
  nickname: string;
  loginId: string;
  profileImageUrl?: string | null;
};

type FriendTab = 'list' | 'requests' | 'find' | 'blocked';
type FriendMenuState = { friend: FriendItem; x: number; y: number } | null;

export default function Layout({ children, showFriendSidebar = true, topSection }: LayoutProps) {
  const { user, logout } = useAuth();
  useMatchCompleteNotification(user?.id);
  const { toggleTheme } = useTheme();
  const navigate = useNavigate();

  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notificationCount, setNotificationCount] = useState(0);
  const [notificationList, setNotificationList] = useState<NotificationItem[]>([]);
  const [notificationListLoading, setNotificationListLoading] = useState(false);
  const [profileImgError, setProfileImgError] = useState(false);
  const [isFriendsOpen, setIsFriendsOpen] = useState(false);

  const [activeFriendTab, setActiveFriendTab] = useState<FriendTab>('list');
  const [friends, setFriends] = useState<FriendItem[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<FriendRequestItem[]>([]);
  const [blockedList, setBlockedList] = useState<FriendItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<FriendItem[]>([]);
  const [friendMenu, setFriendMenu] = useState<FriendMenuState>(null);
  const [mainToast, setMainToast] = useState('');
  const [dmOpen, setDmOpen] = useState(false);
  const [dmTarget, setDmTarget] = useState<FriendItem | null>(null);
  const [dmMessages, setDmMessages] = useState<DmMessageItem[]>([]);
  const [dmInput, setDmInput] = useState('');
  const [dmLoading, setDmLoading] = useState(false);

  const dropdownRef = useRef<HTMLDivElement>(null);
  const notificationRef = useRef<HTMLSpanElement>(null);
  const toastTimerRef = useRef<number | null>(null);
  const dmMessagesEndRef = useRef<HTMLDivElement>(null);
  const lastDmMessagesSigRef = useRef<string>('');
  const dmInputResize = useAutoResizeTextarea(dmInput);

  const showToast = useCallback((message: string) => {
    setMainToast(message);
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
    toastTimerRef.current = window.setTimeout(() => {
      setMainToast('');
      toastTimerRef.current = null;
    }, 2200);
  }, []);

  const fetchNotificationCount = useCallback(() => {
    fetch(apiUrl('api/notifications/count'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { count: 0 }))
      .then((d: { count?: number }) => setNotificationCount(Number(d.count ?? 0)))
      .catch(() => setNotificationCount(0));
  }, []);

  const fetchNotificationList = useCallback(() => {
    setNotificationListLoading(true);
    fetch(apiUrl('api/notifications?limit=30'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: NotificationItem[] }) => setNotificationList(Array.isArray(d.list) ? d.list : []))
      .catch(() => setNotificationList([]))
      .finally(() => setNotificationListLoading(false));
  }, []);

  const fetchFriends = useCallback(() => {
    if (!user) {
      setFriends([]);
      return;
    }
    fetch(apiUrl('api/friends/list'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendItem[]) => {
        const arr = Array.isArray(list) ? list : [];
        setFriends(filterFriendsExcludingSelf(arr, user.id));
      })
      .catch(() => setFriends([]));
  }, [user]);

  const fetchReceivedRequests = useCallback(() => {
    fetch(apiUrl('api/friends/requests/received'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: FriendRequestItem[] }) =>
        setReceivedRequests(Array.isArray(d.list) ? d.list : []),
      )
      .catch(() => setReceivedRequests([]));
  }, []);

  const fetchBlocked = useCallback(() => {
    fetch(apiUrl('api/friends/blocked'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendItem[]) => setBlockedList(Array.isArray(list) ? list : []))
      .catch(() => setBlockedList([]));
  }, []);

  const handleSearchFriend = useCallback(() => {
    const q = searchQuery.trim();
    if (!q) {
      setSearchResults([]);
      return;
    }
    fetch(apiUrl(`api/friends/search?q=${encodeURIComponent(q)}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendItem[]) => {
        const arr = Array.isArray(list) ? list : [];
        setSearchResults(filterFriendsExcludingSelf(arr, user?.id));
      })
      .catch(() => setSearchResults([]));
  }, [searchQuery, user?.id]);

  const handleMarkAllNotificationsRead = useCallback(() => {
    fetch(apiUrl('api/notifications/read-all'), { method: 'POST', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          setNotificationCount(0);
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
          setNotificationCount(0);
        }
      })
      .catch(() => {});
  }, []);

  const handleNotificationClick = useCallback(
    (n: NotificationItem) => {
      setNotificationOpen(false);
      if (!n.read) {
        fetch(apiUrl(`api/notifications/${n.id}/read`), { method: 'PATCH', credentials: 'include' })
          .then(() => {
            setNotificationList((prev) => prev.map((item) => (item.id === n.id ? { ...item, read: true } : item)));
            fetchNotificationCount();
          })
          .catch(() => {});
      }

      if (n.type === 'PAYMENT_COMPLETED' || n.type === 'PAYMENT_REFUNDED') navigate('/profile/pang');
      if (n.type === 'FRIEND_REQUEST') navigate('/profile');
    },
    [fetchNotificationCount, navigate],
  );

  useEffect(() => {
    setProfileImgError(false);
  }, [user?.profileImageUrl]);

  useEffect(() => {
    const closeFriendMenu = () => setFriendMenu(null);
    document.addEventListener('click', closeFriendMenu);
    return () => document.removeEventListener('click', closeFriendMenu);
  }, []);

  useEffect(() => {
    if (!user) {
      setNotificationCount(0);
      setFriends([]);
      setReceivedRequests([]);
      setBlockedList([]);
      return;
    }
    fetchNotificationCount();
    fetchFriends();
    fetchReceivedRequests();
    fetchBlocked();

    const onFriendsChanged = () => {
      fetchFriends();
      fetchBlocked();
    };
    window.addEventListener('gamematcher-friends-changed', onFriendsChanged);
    return () => {
      window.removeEventListener('gamematcher-friends-changed', onFriendsChanged);
    };
  }, [user, fetchNotificationCount, fetchFriends, fetchReceivedRequests, fetchBlocked]);

  useEffect(() => () => {
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
      toastTimerRef.current = null;
    }
  }, []);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) setDropdownOpen(false);
      if (notificationRef.current && !notificationRef.current.contains(e.target as Node)) setNotificationOpen(false);
    };
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, []);

  const handleLogout = async () => {
    await logout();
    setDropdownOpen(false);
    navigate('/streams');
  };

  const handleAcceptRequest = useCallback((requestId: number) => {
    fetch(apiUrl(`api/friends/requests/${requestId}/accept`), { method: 'POST', credentials: 'include' })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '요청 수락에 실패했습니다.');
          return;
        }
        showToast('친구 요청을 수락했습니다.');
        fetchReceivedRequests();
        fetchFriends();
      })
      .catch(() => showToast('요청 수락 중 오류가 발생했습니다.'));
  }, [fetchFriends, fetchReceivedRequests, showToast]);

  const handleRejectRequest = useCallback((requestId: number) => {
    fetch(apiUrl(`api/friends/requests/${requestId}/reject`), { method: 'POST', credentials: 'include' })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '요청 거절에 실패했습니다.');
          return;
        }
        showToast('친구 요청을 거절했습니다.');
        fetchReceivedRequests();
      })
      .catch(() => showToast('요청 거절 중 오류가 발생했습니다.'));
  }, [fetchReceivedRequests, showToast]);

  const handleRemoveFriend = useCallback((friendUserId: number) => {
    if (!window.confirm('해당 친구를 삭제할까요?')) return;
    fetch(apiUrl(`api/friends/${friendUserId}`), { method: 'DELETE', credentials: 'include' })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '친구 삭제에 실패했습니다.');
          return;
        }
        showToast('친구를 삭제했습니다.');
        fetchFriends();
      })
      .catch(() => showToast('친구 삭제 중 오류가 발생했습니다.'));
  }, [fetchFriends, showToast]);

  const handleBlockUser = useCallback((targetUserId: number) => {
    fetch(apiUrl('api/friends/block'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ userId: targetUserId }),
    })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '차단 처리에 실패했습니다.');
          return;
        }
        showToast('사용자를 차단했습니다.');
        fetchFriends();
        fetchBlocked();
        fetchReceivedRequests();
      })
      .catch(() => showToast('차단 처리 중 오류가 발생했습니다.'));
  }, [fetchBlocked, fetchFriends, fetchReceivedRequests, showToast]);

  const handleUnblockUser = useCallback((blockedUserId: number) => {
    fetch(apiUrl(`api/friends/block/${blockedUserId}`), { method: 'DELETE', credentials: 'include' })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '차단 해제에 실패했습니다.');
          return;
        }
        showToast('차단을 해제했습니다.');
        fetchBlocked();
      })
      .catch(() => showToast('차단 해제 중 오류가 발생했습니다.'));
  }, [fetchBlocked, showToast]);

  const handleSendFriendRequest = useCallback((targetUserId: number) => {
    fetch(apiUrl('api/friends/requests'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ userId: targetUserId }),
    })
      .then((r) => r.json().catch(() => ({})).then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '친구 요청 전송에 실패했습니다.');
          return;
        }
        showToast('친구 요청을 보냈습니다.');
      })
      .catch(() => showToast('친구 요청 전송 중 오류가 발생했습니다.'));
  }, [showToast]);

  const loadDmMessages = useCallback(
    (targetUserId: number, opts?: { markRead?: boolean; silent?: boolean }) => {
      if (user && isDmWithSelf(user.id, targetUserId)) {
        setDmMessages([]);
        setDmLoading(false);
        return;
      }
      const silent = opts?.silent === true;
      const markRead = opts?.markRead === true;
      if (!silent) setDmLoading(true);
      fetch(apiUrl(`api/dm?withUserId=${targetUserId}&limit=100`), { credentials: 'include' })
        .then(async (r) => {
          const d: { items?: DmMessageItem[] } = r.ok ? await r.json().catch(() => ({ items: [] })) : { items: [] };
          const items = Array.isArray(d.items) ? d.items : [];
          setDmMessages(items.slice().reverse());
          if (r.ok && markRead) {
            fetch(apiUrl(`api/notifications/read-dm-from?fromUserId=${targetUserId}`), { method: 'POST', credentials: 'include' })
              .then(() => window.dispatchEvent(new CustomEvent('gamematcher-dm-unread-changed')))
              .catch(() => {});
          }
        })
        .catch(() => {
          if (!silent) {
            setDmMessages([]);
            showToast('채팅 내역을 불러오지 못했습니다.');
          }
        })
        .finally(() => {
          if (!silent) setDmLoading(false);
        });
    },
    [showToast, user],
  );

  const openDmPanel = useCallback(
    (target: FriendItem) => {
      if (user && isDmWithSelf(user.id, target.id)) return;
      setDmTarget(target);
      setDmOpen(true);
      setDmInput('');
      lastDmMessagesSigRef.current = '';
      loadDmMessages(target.id, { markRead: true });
    },
    [loadDmMessages, user],
  );

  const handleSendDm = useCallback(() => {
    if (!dmTarget || !user) return;
    if (isDmWithSelf(user.id, dmTarget.id)) return;
    const text = dmInput.trim();
    if (!text) return;
    fetch(apiUrl('api/dm'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ toUserId: dmTarget.id, text }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as { message?: string } & DmMessageItem })))
      .then((res) => {
        if (!res.ok) {
          showToast(res.data?.message || '메시지 전송에 실패했습니다.');
          return;
        }
        const sent: DmMessageItem = {
          id: res.data.id,
          fromUserId: res.data.fromUserId ?? user.id,
          toUserId: res.data.toUserId ?? dmTarget.id,
          text: res.data.text ?? text,
          createdAt: res.data.createdAt,
        };
        setDmMessages((prev) => [...prev, sent]);
        setDmInput('');
      })
      .catch(() => showToast('메시지 전송 중 오류가 발생했습니다.'));
  }, [dmInput, dmTarget, showToast, user]);

  useEffect(() => {
    if (!dmOpen || !dmTarget) return;
    const timer = window.setInterval(() => {
      loadDmMessages(dmTarget.id, { silent: true });
    }, 5000);
    return () => window.clearInterval(timer);
  }, [dmOpen, dmTarget, loadDmMessages]);

  useEffect(() => {
    if (!user || !dmTarget || !dmOpen) return;
    if (isDmWithSelf(user.id, dmTarget.id)) {
      setDmOpen(false);
      setDmTarget(null);
      setDmMessages([]);
    }
  }, [user, dmTarget, dmOpen]);

  useEffect(() => {
    if (!dmOpen) return;
    const last = dmMessages[dmMessages.length - 1];
    const sig = `${dmMessages.length}:${last?.id ?? ''}:${last?.createdAt ?? ''}:${last?.text?.length ?? 0}`;
    if (sig === lastDmMessagesSigRef.current) return;
    lastDmMessagesSigRef.current = sig;
    if (dmMessages.length === 0) return;
    dmMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [dmMessages, dmOpen]);

  return (
    <>
      <header className="main-header">
        <Link to="/" className="logo">GameMatcher</Link>
        <nav className="main-nav">
          <Link to="/streams">전체 방송</Link>
          <Link to="/records">전적검색</Link>
          <Link to="/community">커뮤니티</Link>
        </nav>

        <div className="header-right">
          <span className={`header-icon-wrap auth-only ${notificationCount > 0 ? 'has-unread' : ''}`} ref={notificationRef} id="header-alarm-wrap" title="알림">
            <button
              type="button"
              className="header-icon-btn auth-only"
              onClick={() => {
                setNotificationOpen((o) => {
                  const next = !o;
                  if (next) fetchNotificationList();
                  return next;
                });
              }}
              aria-label="알림"
            >
              <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2" width={22} height={22}><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            </button>

            <div className={`notification-dropdown ${notificationOpen ? 'show' : ''}`}>
              <div className="notification-dropdown-head">
                <strong>알림</strong>
                <div className="notification-actions">
                  <button type="button" className="notification-read-all" onClick={handleMarkAllNotificationsRead}>전체 읽음</button>
                  <button type="button" className="notification-delete-all" onClick={handleDeleteAllNotifications}>전체 삭제</button>
                </div>
              </div>

              <div style={{ padding: '8px 0' }}>
                {notificationListLoading ? (
                  <div className="notification-empty">불러오는 중...</div>
                ) : notificationList.length === 0 ? (
                  <div className="notification-empty">알림이 없습니다.</div>
                ) : (
                  notificationList.map((n) => (
                    <button key={n.id} type="button" className={`notification-item ${!n.read ? 'unread' : ''}`} onClick={() => handleNotificationClick(n)}>
                      <span>{n.message}</span>
                      <div className="notification-time">{n.createdAt ? new Date(n.createdAt).toLocaleString('ko-KR') : ''}</div>
                    </button>
                  ))
                )}
              </div>
            </div>
          </span>

          <button type="button" className="btn-theme" onClick={toggleTheme} title="테마 전환" aria-label="테마 전환">
            <span className="theme-icon">
              <svg className="icon-sun" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
              <svg className="icon-moon" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            </span>
          </button>

          {showFriendSidebar && (
            <span className="header-icon-wrap" title="친구 목록">
              <button
                type="button"
                className={`header-icon-btn header-friends-btn ${isFriendsOpen ? 'active' : ''}`}
                onClick={() => {
                  setIsFriendsOpen((v) => {
                    const next = !v;
                    if (!next) setFriendMenu(null);
                    return next;
                  });
                }}
                aria-label="친구 목록"
                aria-pressed={isFriendsOpen}
              >
                {/* Users 아이콘 대체 SVG (lucide-react 미사용) */}
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </button>
            </span>
          )}

          <Link to="/login" className="btn-login no-auth">로그인</Link>

          <div className="header-profile-wrap auth-only" ref={dropdownRef}>
            <button type="button" className="header-profile-avatar" onClick={() => setDropdownOpen((o) => !o)} aria-label="프로필">
              {resolveProfileImageUrl(user?.profileImageUrl) && !profileImgError ? (
                <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" onError={() => setProfileImgError(true)} />
              ) : (
                <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={24} height={24}><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              )}
            </button>

            <div className={`header-profile-dropdown ${dropdownOpen ? 'show' : ''}`}>
              <div className="dropdown-menu">
                <Link to="/profile" onClick={() => setDropdownOpen(false)}>내 프로필</Link>
                <Link to="/studio" onClick={() => setDropdownOpen(false)}>스튜디오</Link>
                <button type="button" onClick={handleLogout}>로그아웃</button>
              </div>
            </div>
          </div>
        </div>
      </header>

      {topSection}

      <div className={`page-layout ${showFriendSidebar && isFriendsOpen ? 'page-layout--friends-open' : ''}`}>
        <main className="main-container">{children}</main>
        {showFriendSidebar && (
          <aside className={`friend-sidebar ${isFriendsOpen ? 'is-open' : 'is-collapsed'}`} aria-hidden={!isFriendsOpen}>
            <div className="friend-sidebar-header">친구</div>
            <div className="friend-sidebar-login-msg no-auth">
              로그인하면 친구 목록과 요청을 볼 수 있어요.
              <br />
              <Link to="/login" className="btn-login-sidebar">로그인</Link>
            </div>

            <div className="friend-sidebar-body auth-only">
              <div className="friend-sidebar-tabs">
                <button type="button" className={activeFriendTab === 'list' ? 'active' : ''} onClick={() => setActiveFriendTab('list')}>목록</button>
                <button type="button" className={activeFriendTab === 'requests' ? 'active' : ''} onClick={() => setActiveFriendTab('requests')}>
                  요청
                  <span className={`friend-request-badge ${receivedRequests.length > 0 ? 'show' : ''}`}>{receivedRequests.length}</span>
                </button>
                <button type="button" className={activeFriendTab === 'find' ? 'active' : ''} onClick={() => setActiveFriendTab('find')}>찾기</button>
                <button type="button" className={activeFriendTab === 'blocked' ? 'active' : ''} onClick={() => setActiveFriendTab('blocked')}>차단</button>
              </div>

              <div className={`friend-sidebar-panel ${activeFriendTab === 'list' ? 'active' : ''}`}>
                <div className="friend-list-wrap">
                  <ul className="friend-list">
                    {friends.length === 0 ? (
                      <li className="friend-empty">친구가 없습니다.</li>
                    ) : friends.map((f) => (
                        <li className="friend-list-item" key={f.id}>
                          <div className="friend-avatar-wrap" onClick={(e) => e.stopPropagation()}>
                            <button
                              type="button"
                              className="friend-avatar-btn"
                              onClick={(e) => {
                                const rect = e.currentTarget.getBoundingClientRect();
                                const menuWidth = 132;
                                const menuHeight = 126;
                                const x = Math.max(8, Math.min(window.innerWidth - menuWidth - 8, rect.left - menuWidth - 8));
                                const y = Math.max(8, Math.min(window.innerHeight - menuHeight - 8, rect.top));
                                setFriendMenu((prev) => (prev?.friend.id === f.id ? null : { friend: f, x, y }));
                              }}
                              title="친구 메뉴"
                            >
                              <div className="friend-avatar">
                                {resolveProfileImageUrl(f.profileImageUrl) ? <img src={resolveProfileImageUrl(f.profileImageUrl)!} alt="" /> : <span className="friend-avatar-fallback">{(f.nickname || f.loginId || '?')[0]}</span>}
                              </div>
                            </button>
                          </div>
                          <div className="friend-meta-wrap">
                            <div className="friend-meta">
                              <div className="friend-name">{f.nickname || f.loginId}</div>
                            </div>
                          </div>
                        </li>
                      ))}
                  </ul>
                </div>
              </div>

              <div className={`friend-sidebar-panel ${activeFriendTab === 'requests' ? 'active' : ''}`}>
                <div className="friend-list-wrap">
                  {receivedRequests.length === 0 ? (
                    <div className="friend-empty">받은 요청이 없습니다.</div>
                  ) : (
                    receivedRequests.map((r) => (
                      <div className="friend-request-item" key={r.id}>
                        <div className="friend-avatar-wrap">
                          <div className="friend-avatar">
                            {resolveProfileImageUrl(r.profileImageUrl) ? <img src={resolveProfileImageUrl(r.profileImageUrl)!} alt="" /> : <span className="friend-avatar-fallback">{(r.nickname || r.loginId || '?')[0]}</span>}
                          </div>
                        </div>
                        <div className="friend-meta-wrap">
                          <div className="friend-name">{r.nickname || r.loginId}</div>
                          <div className="friend-status">@{r.loginId}</div>
                        </div>
                        <div className="req-actions">
                          <button type="button" className="req-btn accept" onClick={() => handleAcceptRequest(r.id)}>수락</button>
                          <button type="button" className="req-btn reject" onClick={() => handleRejectRequest(r.id)}>거절</button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className={`friend-sidebar-panel ${activeFriendTab === 'find' ? 'active' : ''}`}>
                <div className="friend-search-wrap">
                  <input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="아이디/닉네임 검색" />
                  <button type="button" onClick={handleSearchFriend}>검색</button>
                </div>
                <div className="friend-list-wrap">
                  <ul className="friend-list">
                    {searchResults.length === 0 ? <li className="friend-empty">검색 결과가 없습니다.</li> : searchResults.map((r) => (
                      <li className="friend-list-item" key={r.id}>
                        <div className="friend-avatar-wrap">
                          <div className="friend-avatar">{resolveProfileImageUrl(r.profileImageUrl) ? <img src={resolveProfileImageUrl(r.profileImageUrl)!} alt="" /> : <span className="friend-avatar-fallback">{(r.nickname || r.loginId || '?')[0]}</span>}</div>
                        </div>
                        <div className="friend-meta-wrap">
                          <div className="friend-meta">
                            <div className="friend-name">{r.nickname || r.loginId}</div>
                            <div className="friend-status">@{r.loginId}</div>
                          </div>
                        </div>
                        <div className="friend-actions">
                          <button type="button" className="btn-friend-action" title="친구 요청" onClick={() => handleSendFriendRequest(r.id)}>＋</button>
                          <button type="button" className="btn-friend-action" title="차단" onClick={() => handleBlockUser(r.id)}>⛔</button>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              <div className={`friend-sidebar-panel ${activeFriendTab === 'blocked' ? 'active' : ''}`}>
                <div className="friend-list-wrap">
                  <ul className="friend-list">
                    {blockedList.length === 0 ? <li className="friend-empty">차단 목록이 없습니다.</li> : blockedList.map((b) => (
                      <li className="friend-list-item" key={b.id}>
                        <div className="friend-avatar-wrap">
                          <div className="friend-avatar">{resolveProfileImageUrl(b.profileImageUrl) ? <img src={resolveProfileImageUrl(b.profileImageUrl)!} alt="" /> : <span className="friend-avatar-fallback">{(b.nickname || b.loginId || '?')[0]}</span>}</div>
                        </div>
                        <div className="friend-meta-wrap">
                          <div className="friend-meta">
                            <div className="friend-name">{b.nickname || b.loginId}</div>
                            <div className="friend-status">@{b.loginId}</div>
                          </div>
                        </div>
                        <button type="button" className="req-btn unblock" onClick={() => handleUnblockUser(b.id)}>차단 해제</button>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </aside>
        )}
      </div>

      <div className={`main-toast ${mainToast ? 'show' : ''}`}>{mainToast}</div>

      {friendMenu && (
        <div
          className={`friend-avatar-menu friend-avatar-menu-portal ${document.documentElement.getAttribute('data-theme') === 'light' ? 'theme-light' : 'theme-dark'}`}
          style={{ left: `${friendMenu.x}px`, top: `${friendMenu.y}px`, width: '132px' }}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            type="button"
            onClick={() => {
              const target = friendMenu.friend;
              setFriendMenu(null);
              openDmPanel(target);
            }}
          >
            메시지
          </button>
          <button
            type="button"
            onClick={() => {
              const target = friendMenu.friend;
              setFriendMenu(null);
              handleRemoveFriend(target.id);
            }}
          >
            친구 삭제
          </button>
          <button
            type="button"
            onClick={() => {
              const target = friendMenu.friend;
              setFriendMenu(null);
              handleBlockUser(target.id);
            }}
          >
            차단
          </button>
        </div>
      )}

      <div className={`dm-panel-backdrop ${dmOpen ? 'show' : ''}`} onClick={() => setDmOpen(false)}>
        <div className="dm-panel" onClick={(e) => e.stopPropagation()}>
          <div className="dm-panel-header">
            <div className="dm-panel-header-info">
              <div className="dm-panel-header-avatar">
                {resolveProfileImageUrl(dmTarget?.profileImageUrl) ? (
                  <img src={resolveProfileImageUrl(dmTarget?.profileImageUrl)!} alt="" />
                ) : (
                  <span className="dm-panel-header-initial">{(dmTarget?.nickname || dmTarget?.loginId || '?')[0]}</span>
                )}
              </div>
              <div className="dm-panel-header-text">
                <div className="dm-panel-title">{dmTarget?.nickname || dmTarget?.loginId || '대화'}</div>
              </div>
            </div>
            <button type="button" className="dm-panel-close" onClick={() => setDmOpen(false)}>×</button>
          </div>

          <ul className="dm-messages">
            {dmLoading ? (
              <li className="friend-empty">불러오는 중...</li>
            ) : dmMessages.length === 0 ? (
              <li className="friend-empty">대화를 시작해보세요.</li>
            ) : (
              dmMessages.map((m, idx) => {
                const mine = user?.id === m.fromUserId;
                const labelMine = user?.nickname || user?.username || '나';
                const labelTheirs = dmTarget?.nickname || dmTarget?.loginId || '상대';
                return (
                  <li key={m.id ?? `${m.createdAt}-${idx}`} className={`dm-msg ${mine ? 'mine' : 'theirs'}`}>
                    <div className="dm-msg-avatar-col">
                      <span className="dm-msg-avatar-label">{mine ? labelMine : labelTheirs}</span>
                      <div className="dm-msg-avatar">
                        {mine ? (
                          resolveProfileImageUrl(user?.profileImageUrl) ? <img src={resolveProfileImageUrl(user?.profileImageUrl)!} alt="" /> : <span className="dm-msg-avatar-initial">{(user?.nickname || user?.username || '?')[0]}</span>
                        ) : (
                          resolveProfileImageUrl(dmTarget?.profileImageUrl) ? <img src={resolveProfileImageUrl(dmTarget?.profileImageUrl)!} alt="" /> : <span className="dm-msg-avatar-initial">{(dmTarget?.nickname || dmTarget?.loginId || '?')[0]}</span>
                        )}
                      </div>
                    </div>
                    <div className="dm-msg-body">
                      <div className="dm-msg-bubble">{m.text}</div>
                      <span className="dm-msg-time">{m.createdAt ? new Date(m.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                    </div>
                  </li>
                );
              })
            )}
            <div ref={dmMessagesEndRef} />
          </ul>

          <div className="dm-panel-input-wrap">
            <textarea
              ref={dmInputResize.ref}
              value={dmInput}
              onChange={(e) => setDmInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSendDm();
                }
              }}
              placeholder="메시지를 입력하세요"
              maxLength={2000}
              rows={1}
            />
            <button type="button" onClick={handleSendDm}>전송</button>
          </div>
        </div>
      </div>
      <FloatingChatWidget />
    </>
  );
}
