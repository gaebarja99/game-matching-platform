import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { filterConversationsExcludingSelf, filterFriendsExcludingSelf, isDmWithSelf } from '../utils/dmSelf';
import { useAutoResizeTextarea } from '../hooks/useAutoResizeTextarea';
import './DirectMessages.css';

type FriendRow = {
  id: number;
  loginId: string;
  nickname: string;
  profileImageUrl?: string | null;
};

type ConvRow = {
  otherUserId: number;
  nickname: string;
  loginId: string;
  profileImageUrl?: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount?: number;
};

type DmMsg = {
  id?: number;
  fromUserId: number;
  toUserId: number;
  text: string;
  createdAt?: string;
};

type LeftTab = 'conversations' | 'friends';

export default function DirectMessages() {
  const { user, loading: authLoading } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const lastMessagesSigRef = useRef<string>('');

  const [leftTab, setLeftTab] = useState<LeftTab>('conversations');
  const [conversations, setConversations] = useState<ConvRow[]>([]);
  const [convLoading, setConvLoading] = useState(false);
  const [friends, setFriends] = useState<FriendRow[]>([]);
  const [friendsLoading, setFriendsLoading] = useState(false);

  const [selected, setSelected] = useState<FriendRow | null>(null);
  const selectedRef = useRef<FriendRow | null>(null);
  const [messages, setMessages] = useState<DmMsg[]>([]);
  const [msgLoading, setMsgLoading] = useState(false);
  const [input, setInput] = useState('');
  const inputResize = useAutoResizeTextarea(input);
  const [sending, setSending] = useState(false);
  const [friendProfileModal, setFriendProfileModal] = useState<FriendRow | null>(null);
  const [friendProfileBio, setFriendProfileBio] = useState<{ loading: boolean; text: string; error?: string }>({
    loading: false,
    text: '',
  });
  const [blockBusy, setBlockBusy] = useState(false);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  const loadConversations = useCallback(() => {
    if (!user) return;
    setConvLoading(true);
    fetch(apiUrl('api/dm/conversations?size=50'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: ConvRow[] }) => {
        const list = Array.isArray(d.list) ? d.list : [];
        setConversations(filterConversationsExcludingSelf(list, user.id));
      })
      .catch(() => setConversations([]))
      .finally(() => setConvLoading(false));
  }, [user]);

  const loadFriends = useCallback(() => {
    if (!user) return;
    setFriendsLoading(true);
    fetch(apiUrl('api/friends/list'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendRow[]) => {
        const arr = Array.isArray(list) ? list : [];
        setFriends(filterFriendsExcludingSelf(arr, user.id));
      })
      .catch(() => setFriends([]))
      .finally(() => setFriendsLoading(false));
  }, [user]);

  const loadMessages = useCallback(
    (withUserId: number, opts?: { silent?: boolean }) => {
      if (user && isDmWithSelf(user.id, withUserId)) {
        setMessages([]);
        setMsgLoading(false);
        return;
      }
      const silent = opts?.silent === true;
      if (!silent) setMsgLoading(true);
      fetch(apiUrl(`api/dm?withUserId=${withUserId}&limit=100`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : { items: [] }))
        .then((d: { items?: DmMsg[] }) => {
          const items = Array.isArray(d.items) ? d.items : [];
          setMessages(items.slice().reverse());
        })
        .catch(() => {
          if (!silent) setMessages([]);
        })
        .finally(() => {
          if (!silent) setMsgLoading(false);
        });
    },
    [user],
  );

  useEffect(() => {
    if (authLoading || !user) return;
    loadConversations();
    loadFriends();
  }, [authLoading, user, loadConversations, loadFriends]);

  useEffect(() => {
    const withParam = searchParams.get('with');
    if (!withParam || !user) return;
    const id = Number(withParam);
    if (!Number.isFinite(id)) return;
    if (id === user.id) {
      setSearchParams({});
      return;
    }
    if (friends.length === 0) return;
    const f = friends.find((x) => x.id === id);
    if (f) {
      setSelected(f);
      setLeftTab('friends');
    }
  }, [searchParams, friends, user, setSearchParams]);

  useEffect(() => {
    if (!selected) return;
    lastMessagesSigRef.current = '';
    loadMessages(selected.id);
  }, [selected, loadMessages]);

  useEffect(() => {
    if (!selected || !user) return;
    const t = window.setInterval(() => loadMessages(selected.id, { silent: true }), 5000);
    return () => window.clearInterval(t);
  }, [selected, user, loadMessages]);

  useEffect(() => {
    const last = messages[messages.length - 1];
    const sig = `${messages.length}:${last?.id ?? ''}:${last?.createdAt ?? ''}:${last?.text?.length ?? 0}`;
    if (sig === lastMessagesSigRef.current) return;
    lastMessagesSigRef.current = sig;
    if (messages.length === 0) return;
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!selected) return;
    fetch(apiUrl(`api/notifications/read-dm-from?fromUserId=${selected.id}`), { method: 'POST', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          loadConversations();
          window.dispatchEvent(new CustomEvent('gamematcher-dm-unread-changed'));
        }
      })
      .catch(() => {});
  }, [selected?.id, loadConversations]);

  useEffect(() => {
    if (!user || !selected) return;
    if (isDmWithSelf(user.id, selected.id)) {
      setSelected(null);
      setMessages([]);
      setInput('');
    }
  }, [user, selected]);

  const selectConversation = (c: ConvRow) => {
    if (!user || isDmWithSelf(user.id, c.otherUserId)) return;
    setSelected({
      id: c.otherUserId,
      loginId: c.loginId || '',
      nickname: c.nickname || c.loginId || '유저',
      profileImageUrl: c.profileImageUrl,
    });
    setSearchParams({});
  };

  const handleSend = () => {
    if (!user || !selected || sending) return;
    if (isDmWithSelf(user.id, selected.id)) return;
    const text = input.trim();
    if (!text) return;
    setSending(true);
    fetch(apiUrl('api/dm'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ toUserId: selected.id, text }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as DmMsg & { message?: string } })))
      .then((res) => {
        if (!res.ok) return;
        const sent: DmMsg = {
          id: res.data.id,
          fromUserId: user.id,
          toUserId: selected.id,
          text: res.data.text ?? text,
          createdAt: res.data.createdAt,
        };
        setMessages((prev) => [...prev, sent]);
        setInput('');
        loadConversations();
      })
      .finally(() => setSending(false));
  };

  useEffect(() => {
    if (!friendProfileModal) {
      setFriendProfileBio({ loading: false, text: '' });
      return;
    }
    setFriendProfileBio({ loading: true, text: '' });
    fetch(apiUrl(`api/friends/${friendProfileModal.id}/public-profile`), { credentials: 'include' })
      .then(async (r) => {
        const d = (await r.json().catch(() => ({}))) as { bio?: string; message?: string };
        if (!r.ok) throw new Error(d.message || '프로필을 불러오지 못했습니다.');
        setFriendProfileBio({ loading: false, text: typeof d.bio === 'string' ? d.bio : '' });
      })
      .catch((e: Error) => {
        setFriendProfileBio({ loading: false, text: '', error: e.message || '오류가 발생했습니다.' });
      });
  }, [friendProfileModal?.id]);

  const closeFriendProfileModal = useCallback(() => {
    setFriendProfileModal(null);
  }, []);

  const handleBlockFromFriendModal = useCallback(() => {
    if (!friendProfileModal || blockBusy) return;
    if (!window.confirm('이 친구를 차단할까요? 차단하면 친구 관계가 해제됩니다.')) return;
    setBlockBusy(true);
    const blockedId = friendProfileModal.id;
    fetch(apiUrl('api/friends/block'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ userId: blockedId }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          window.alert(res.data?.message || '차단에 실패했습니다.');
          return;
        }
        closeFriendProfileModal();
        loadFriends();
        loadConversations();
        const hadChatOpen = selectedRef.current?.id === blockedId;
        setSelected((prev) => (prev?.id === blockedId ? null : prev));
        if (hadChatOpen) {
          setMessages([]);
          setInput('');
        }
        window.dispatchEvent(new CustomEvent('gamematcher-dm-unread-changed'));
        window.dispatchEvent(new CustomEvent('gamematcher-friends-changed'));
      })
      .catch(() => window.alert('차단 요청 중 오류가 발생했습니다.'))
      .finally(() => setBlockBusy(false));
  }, [friendProfileModal, blockBusy, closeFriendProfileModal, loadFriends, loadConversations]);

  if (authLoading) {
    return (
      <Layout>
        <div className="dm-page-loading">불러오는 중...</div>
      </Layout>
    );
  }

  if (!user) {
    return (
      <Layout>
        <div className="dm-page-guest">
          <p>1:1 채팅은 로그인 후 친구와만 이용할 수 있습니다.</p>
          <Link to="/login" className="dm-page-login-link">
            로그인
          </Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="dm-page">
        <header className="dm-page-header">
          <h1 className="dm-page-title">1:1 채팅</h1>
          <p className="dm-page-desc">친구와만 메시지를 주고받을 수 있습니다.</p>
        </header>

        <div className="dm-page-grid">
          <aside className="dm-page-sidebar">
            <div className="dm-page-tabs">
              <button
                type="button"
                className={leftTab === 'conversations' ? 'active' : ''}
                onClick={() => setLeftTab('conversations')}
              >
                최근 대화
              </button>
              <button type="button" className={leftTab === 'friends' ? 'active' : ''} onClick={() => setLeftTab('friends')}>
                친구
              </button>
            </div>

            {leftTab === 'conversations' && (
              <ul className="dm-page-list">
                {convLoading ? (
                  <li className="dm-page-list-empty">불러오는 중...</li>
                ) : conversations.length === 0 ? (
                  <li className="dm-page-list-empty">대화 내역이 없습니다. 친구 탭에서 대화를 시작해 보세요.</li>
                ) : (
                  conversations.map((c) => (
                    <li key={c.otherUserId}>
                      <button
                        type="button"
                        className={`dm-page-list-item ${selected?.id === c.otherUserId ? 'is-active' : ''}`}
                        onClick={() => selectConversation(c)}
                      >
                        <div className="dm-page-list-avatar">
                          {resolveProfileImageUrl(c.profileImageUrl) ? (
                            <img src={resolveProfileImageUrl(c.profileImageUrl)!} alt="" />
                          ) : (
                            <span>{(c.nickname || c.loginId || '?')[0]}</span>
                          )}
                        </div>
                        <div className="dm-page-list-meta">
                          <span className="dm-page-list-name">{c.nickname || c.loginId || '유저'}</span>
                          <span className="dm-page-list-preview">
                            {c.lastMessage ? (c.lastMessage.length > 36 ? `${c.lastMessage.slice(0, 36)}…` : c.lastMessage) : '메시지 없음'}
                          </span>
                        </div>
                        {(c.unreadCount ?? 0) > 0 ? <span className="dm-page-unread">{c.unreadCount! > 99 ? '99+' : c.unreadCount}</span> : null}
                      </button>
                    </li>
                  ))
                )}
              </ul>
            )}

            {leftTab === 'friends' && (
              <ul className="dm-page-list">
                {friendsLoading ? (
                  <li className="dm-page-list-empty">불러오는 중...</li>
                ) : friends.length === 0 ? (
                  <li className="dm-page-list-empty">친구가 없습니다. 오른쪽 사이드바에서 친구를 추가해 보세요.</li>
                ) : (
                  friends.map((f) => (
                    <li key={f.id}>
                      <button
                        type="button"
                        className={`dm-page-list-item ${selected?.id === f.id ? 'is-active' : ''}`}
                        onClick={() => setFriendProfileModal(f)}
                      >
                        <div className="dm-page-list-avatar">
                          {resolveProfileImageUrl(f.profileImageUrl) ? (
                            <img src={resolveProfileImageUrl(f.profileImageUrl)!} alt="" />
                          ) : (
                            <span>{(f.nickname || f.loginId || '?')[0]}</span>
                          )}
                        </div>
                        <div className="dm-page-list-meta">
                          <span className="dm-page-list-name">{f.nickname || f.loginId}</span>
                        </div>
                      </button>
                    </li>
                  ))
                )}
              </ul>
            )}
          </aside>

          <section className="dm-page-main">
            {!selected ? (
              <div className="dm-page-placeholder">
                <p>왼쪽에서 대화 또는 친구를 선택하세요.</p>
              </div>
            ) : (
              <>
                <div className="dm-page-chat-header">
                  <div className="dm-page-chat-avatar">
                    {resolveProfileImageUrl(selected.profileImageUrl) ? (
                      <img src={resolveProfileImageUrl(selected.profileImageUrl)!} alt="" />
                    ) : (
                      <span>{(selected.nickname || selected.loginId || '?')[0]}</span>
                    )}
                  </div>
                  <div className="dm-page-chat-head-text">
                    <span className="dm-page-chat-name">{selected.nickname || selected.loginId}</span>
                  </div>
                </div>

                <ul className="dm-messages dm-page-messages">
                  {msgLoading ? (
                    <li className="friend-empty">불러오는 중...</li>
                  ) : messages.length === 0 ? (
                    <li className="friend-empty">대화를 시작해 보세요.</li>
                  ) : (
                    messages.map((m, idx) => {
                      const mine = user.id === m.fromUserId;
                      const labelMine = user.nickname || user.username || '나';
                      const labelTheirs = selected.nickname || selected.loginId || '상대';
                      return (
                        <li key={m.id ?? `${m.createdAt}-${idx}`} className={`dm-msg ${mine ? 'mine' : 'theirs'}`}>
                          <div className="dm-msg-avatar-col">
                            <span className="dm-msg-avatar-label">{mine ? labelMine : labelTheirs}</span>
                            <div className="dm-msg-avatar">
                              {mine ? (
                                resolveProfileImageUrl(user.profileImageUrl) ? (
                                  <img src={resolveProfileImageUrl(user.profileImageUrl)!} alt="" />
                                ) : (
                                  <span className="dm-msg-avatar-initial">{(user.nickname || user.username || '?')[0]}</span>
                                )
                              ) : resolveProfileImageUrl(selected.profileImageUrl) ? (
                                <img src={resolveProfileImageUrl(selected.profileImageUrl)!} alt="" />
                              ) : (
                                <span className="dm-msg-avatar-initial">{(selected.nickname || selected.loginId || '?')[0]}</span>
                              )}
                            </div>
                          </div>
                          <div className="dm-msg-body">
                            <div className="dm-msg-bubble">{m.text}</div>
                            <span className="dm-msg-time">
                              {m.createdAt ? new Date(m.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : ''}
                            </span>
                          </div>
                        </li>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </ul>

                <div className="dm-panel-input-wrap dm-page-input">
                  <textarea
                    ref={inputResize.ref}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleSend();
                      }
                    }}
                    placeholder="메시지를 입력하세요"
                    maxLength={2000}
                    rows={1}
                  />
                  <button type="button" onClick={handleSend} disabled={sending}>
                    전송
                  </button>
                </div>
              </>
            )}
          </section>
        </div>
      </div>

      {friendProfileModal ? (
        <div
          className="dm-page-friend-profile-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="dm-page-friend-profile-name"
          onClick={closeFriendProfileModal}
        >
          <div className="dm-page-friend-profile-card" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="dm-page-friend-profile-close"
              aria-label="닫기"
              onClick={closeFriendProfileModal}
            >
              ×
            </button>
            <div className="dm-page-friend-profile-avatar">
              {resolveProfileImageUrl(friendProfileModal.profileImageUrl) ? (
                <img src={resolveProfileImageUrl(friendProfileModal.profileImageUrl)!} alt="" />
              ) : (
                <span>{(friendProfileModal.nickname || friendProfileModal.loginId || '?')[0]}</span>
              )}
            </div>
            <div className="dm-page-friend-profile-name-row">
              <h3 id="dm-page-friend-profile-name" className="dm-page-friend-profile-name">
                {friendProfileModal.nickname || friendProfileModal.loginId}
              </h3>
              <button
                type="button"
                className="dm-page-friend-profile-block"
                disabled={blockBusy}
                onClick={handleBlockFromFriendModal}
              >
                {blockBusy ? '…' : '차단'}
              </button>
            </div>
            <div className="dm-page-friend-profile-desc-wrap">
              {friendProfileBio.loading ? (
                <p className="dm-page-friend-profile-desc">자기소개 불러오는 중…</p>
              ) : friendProfileBio.error ? (
                <p className="dm-page-friend-profile-desc dm-page-friend-profile-desc--error">{friendProfileBio.error}</p>
              ) : (
                <p className="dm-page-friend-profile-desc">
                  {friendProfileBio.text.trim() ? friendProfileBio.text : '작성한 자기소개가 없습니다.'}
                </p>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </Layout>
  );
}
