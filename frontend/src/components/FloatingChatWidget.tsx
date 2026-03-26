import { useCallback, useEffect, useRef, useState, type ComponentType } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { sendChatMessage } from '../api/chat';
import ChatbotMessageContent from './ChatbotMessageContent';
import { CHATBOT_STARTER_PROMPTS } from '../constants/chatbotStarterPrompts';
import { useDirectMessages, type FriendRow } from '../hooks/useDirectMessages';
import { useAutoResizeTextarea } from '../hooks/useAutoResizeTextarea';
import { deriveChatbotActions, type ChatbotAction } from '../utils/chatbotActions';
import ChatPopup from './ChatPopup';
import RandomMatchChatBody from './RandomMatchChatBody';
import './FloatingChatWidget.css';

/** 우측 하단 플로팅 위젯에서 열 수 있는 채팅 모드 (null = 닫힘) */
export type ActiveChatMode = 'ONE_ON_ONE' | 'RANDOM' | 'GROUP' | 'CHATBOT';

const MODE_TITLES: Record<ActiveChatMode, string> = {
  ONE_ON_ONE: '1:1 채팅',
  RANDOM: '랜덤채팅',
  GROUP: '단체채팅',
  CHATBOT: '챗봇',
};

const MENU: { key: ActiveChatMode; label: string; Icon: ComponentType }[] = [
  { key: 'ONE_ON_ONE', label: '1:1채팅', Icon: IconOneOnOne },
  { key: 'RANDOM', label: '랜덤채팅', Icon: IconRandomMatch },
  { key: 'GROUP', label: '단체채팅', Icon: IconGroupChat },
  { key: 'CHATBOT', label: '챗봇', Icon: IconChatbot },
];

function IconOneOnOne() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden>
      <path
        fill="none"
        stroke="#ec407a"
        strokeWidth="1.65"
        strokeLinejoin="round"
        d="M5 8a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H9l-2.5 2v-2H7a2 2 0 0 1-2-2V8z"
      />
      <path
        fill="none"
        stroke="#f48fb1"
        strokeWidth="1.5"
        strokeLinejoin="round"
        d="M11 14.5a2 2 0 0 1 1.7-1h4.3a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2h-1.2l-1.8 1.5V20h-0.3a2 2 0 0 1-2-2v-3.5z"
      />
    </svg>
  );
}

function IconRandomMatch() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden>
      <path
        fill="none"
        stroke="#7c4dff"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M4 6h10v6H4V6zm10 0h6v4h-6V6zM4 14h6v6H4v-6zm8 2h8v4h-8v-4z"
      />
      <circle cx="18" cy="18" r="2.2" fill="#7c4dff" opacity="0.35" />
    </svg>
  );
}

function IconGroupChat() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden>
      <circle cx="9" cy="8" r="3.5" fill="none" stroke="#7c4dff" strokeWidth="1.8" />
      <path
        fill="none"
        stroke="#7c4dff"
        strokeWidth="1.8"
        strokeLinecap="round"
        d="M4 20v-1a5 5 0 0 1 5-5h0a5 5 0 0 1 5 5v1"
      />
      <circle cx="17" cy="9" r="2.8" fill="none" stroke="#9575cd" strokeWidth="1.6" />
      <path fill="none" stroke="#9575cd" strokeWidth="1.6" strokeLinecap="round" d="M21 20v-0.8a3.5 3.5 0 0 0-2.5-3.3" />
    </svg>
  );
}

function IconChatbot() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden>
      <path
        fill="none"
        stroke="#00acc1"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm-6 8v-1a6 6 0 0 1 12 0v1M9 15h6M8 21h8"
      />
      <circle cx="9" cy="11" r="0.8" fill="#00acc1" />
      <circle cx="15" cy="11" r="0.8" fill="#00acc1" />
    </svg>
  );
}

type GroupRoom = { id: number; name: string; memberCount: number };

function FloatingGroupPanel({ onRoomNavigate }: { onRoomNavigate?: () => void }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [rooms, setRooms] = useState<GroupRoom[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(() => {
    if (!user) return;
    setLoading(true);
    fetch(apiUrl('api/group-chat/rooms'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: GroupRoom[] }) => setRooms(Array.isArray(d?.list) ? d.list : []))
      .catch(() => setRooms([]))
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => {
    if (!user) return;
    load();
  }, [user, load]);

  if (!user) {
    return (
      <p className="floating-chat-widget-login">
        <button type="button" className="floating-chat-widget-inline-link" onClick={() => navigate('/login')}>
          로그인
        </button>
        후 단체 채팅방을 이용할 수 있습니다.
      </p>
    );
  }

  return (
    <div className="floating-chat-widget-group">
      <p className="floating-chat-widget-hint">참여 중인 방 목록입니다.</p>
      {loading ? (
        <div className="floating-chat-widget-empty">불러오는 중...</div>
      ) : rooms.length === 0 ? (
        <div className="floating-chat-widget-empty">참여 중인 단체 채팅방이 없습니다.</div>
      ) : (
        <ul className="floating-chat-widget-list">
          {rooms.map((r) => (
            <li key={r.id}>
              <button
                type="button"
                className="floating-chat-widget-list-btn"
                onClick={() => {
                  navigate(`/group-chat/room/${r.id}`);
                  onRoomNavigate?.();
                }}
              >
                <span className="floating-chat-widget-list-title">{r.name}</span>
                <span className="floating-chat-widget-list-sub">{r.memberCount}명</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

type BotMsg = { role: 'assistant' | 'user'; text: string; time: string; actions?: ChatbotAction[] };

function formatTime() {
  return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

function FloatingChatbotPanel() {
  const navigate = useNavigate();
  const endRef = useRef<HTMLDivElement | null>(null);
  const [messages, setMessages] = useState<BotMsg[]>([
    {
      role: 'assistant',
      text: '필요한 기능만 보내 주세요.\n\n📌 바로 도와드릴 수 있는 내용\n- 전적 검색\n- 계정 연동\n- 채팅과 매칭\n- 후원과 스튜디오',
      time: formatTime(),
      actions: [
        { label: '전적 검색', to: '/records' },
        { label: '계정 연동', to: '/profile/account-links' },
      ],
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, loading]);

  const submit = async (text: string) => {
    const m = text.trim();
    if (!m || loading) return;
    setMessages((prev) => [...prev, { role: 'user', text: m, time: formatTime() }]);
    setInput('');
    setError('');
    setLoading(true);
    try {
      const res = await sendChatMessage(m);
      const reply = res.reply || '답변을 생성하지 못했습니다.';
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: reply,
          time: formatTime(),
          actions: deriveChatbotActions(`${m}\n${reply}`),
        },
      ]);
    } catch (e) {
      const msg = e instanceof Error ? e.message : '응답을 불러오지 못했습니다.';
      setError(msg);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: msg, time: formatTime(), actions: deriveChatbotActions(m) },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="floating-chat-widget-chatbot">
      <div className="floating-chat-widget-chatbot-messages chat-popup-scroll">
        {messages.map((msg, i) => (
          <div key={`${msg.role}-${i}-${msg.time}`} className={`floating-chat-widget-bubble ${msg.role === 'user' ? 'is-user' : 'is-bot'}`}>
            <span className="floating-chat-widget-bubble-meta">{msg.role === 'user' ? '사용자' : 'GM MATE'} · {msg.time}</span>
            <ChatbotMessageContent text={msg.text} className="floating-chat-widget-message-content" />
            {msg.role === 'assistant' && i === 0 ? (
              <div className="floating-chat-widget-starter-grid">
                {CHATBOT_STARTER_PROMPTS.map((item) => (
                  <button
                    key={item.label}
                    type="button"
                    className="floating-chat-widget-starter-card"
                    onClick={() => void submit(item.prompt)}
                    disabled={loading}
                  >
                    <strong>{item.label}</strong>
                    <span>바로 질문</span>
                  </button>
                ))}
              </div>
            ) : null}
            {msg.role === 'assistant' && msg.actions?.length ? (
              <div className="floating-chat-widget-bubble-actions">
                {msg.actions.map((action) => (
                  <button
                    key={`${action.to}-${action.label}`}
                    type="button"
                    className="floating-chat-widget-bubble-action"
                    onClick={() => navigate(action.to)}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        ))}
        {loading ? (
          <div className="floating-chat-widget-bubble is-bot">
            <span className="floating-chat-widget-bubble-meta">GM MATE</span>
            <ChatbotMessageContent text="응답 생성 중..." className="floating-chat-widget-message-content" />
          </div>
        ) : null}
        <div ref={endRef} />
      </div>
      <form
        className="floating-chat-widget-chatbot-form"
        onSubmit={(e) => {
          e.preventDefault();
          void submit(input);
        }}
      >
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              void submit(input);
            }
          }}
          placeholder="질문을 입력해 주세요"
          rows={3}
          disabled={loading}
        />
        <button type="submit" disabled={loading || !input.trim()}>
          {loading ? '전송 중...' : '보내기'}
        </button>
      </form>
      {error ? <p className="floating-chat-widget-error">{error}</p> : null}
    </div>
  );
}

function FloatingOneOnOnePanel() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const dm = useDirectMessages({ enabled: !!user });
  const dmInputResize = useAutoResizeTextarea(dm.input);
  const [friendProfileModal, setFriendProfileModal] = useState<FriendRow | null>(null);
  const [friendProfileBio, setFriendProfileBio] = useState<{ loading: boolean; text: string; error?: string }>({
    loading: false,
    text: '',
  });
  const [blockBusy, setBlockBusy] = useState(false);

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
    fetch(apiUrl('api/friends/block'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ userId: friendProfileModal.id }),
    })
      .then((r) => r.json().then((d) => ({ ok: r.ok, data: d as { message?: string } })))
      .then((res) => {
        if (!res.ok) {
          window.alert(res.data?.message || '차단에 실패했습니다.');
          return;
        }
        closeFriendProfileModal();
        void dm.loadFriends();
        void dm.loadConversations();
        window.dispatchEvent(new CustomEvent('gamematcher-dm-unread-changed'));
        window.dispatchEvent(new CustomEvent('gamematcher-friends-changed'));
      })
      .catch(() => window.alert('차단 요청 중 오류가 발생했습니다.'))
      .finally(() => setBlockBusy(false));
  }, [friendProfileModal, blockBusy, closeFriendProfileModal, dm]);

  if (!user) {
    return (
      <p className="floating-chat-widget-login">
        <button type="button" className="floating-chat-widget-inline-link" onClick={() => navigate('/login')}>
          로그인
        </button>
        후 친구와 1:1 채팅을 이용할 수 있습니다.
      </p>
    );
  }

  if (dm.selected) {
    const sel = dm.selected;
    return (
      <div className="floating-chat-widget-dm floating-chat-widget-dm-thread">
        <div className="floating-chat-widget-dm-thread-head">
          <button type="button" className="floating-chat-widget-dm-back" onClick={() => dm.resetSelection()} aria-label="목록으로">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden>
              <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none" />
            </svg>
          </button>
          <div className="floating-chat-widget-dm-thread-peer">
            <div className="floating-chat-widget-dm-thread-avatar">
              {resolveProfileImageUrl(sel.profileImageUrl) ? (
                <img src={resolveProfileImageUrl(sel.profileImageUrl)!} alt="" />
              ) : (
                <span>{(sel.nickname || sel.loginId || '?')[0]}</span>
              )}
            </div>
            <div className="floating-chat-widget-dm-thread-names">
              <span className="floating-chat-widget-dm-thread-name">{sel.nickname || sel.loginId}</span>
            </div>
          </div>
        </div>
        <ul className="dm-messages floating-chat-widget-dm-messages chat-popup-scroll">
          {dm.msgLoading ? (
            <li className="friend-empty">불러오는 중...</li>
          ) : dm.messages.length === 0 ? (
            <li className="friend-empty">대화를 시작해 보세요.</li>
          ) : (
            dm.messages.map((m, idx) => {
              const mine = user.id === m.fromUserId;
              const labelMine = user.nickname || user.username || '나';
              const labelTheirs = sel.nickname || sel.loginId || '상대';
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
                      ) : resolveProfileImageUrl(sel.profileImageUrl) ? (
                        <img src={resolveProfileImageUrl(sel.profileImageUrl)!} alt="" />
                      ) : (
                        <span className="dm-msg-avatar-initial">{(sel.nickname || sel.loginId || '?')[0]}</span>
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
          <div ref={dm.messagesEndRef} />
        </ul>
        <div className="dm-panel-input-wrap floating-chat-widget-dm-input">
          <textarea
            ref={dmInputResize.ref}
            value={dm.input}
            onChange={(e) => dm.setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                dm.handleSend();
              }
            }}
            placeholder="메시지를 입력하세요"
            maxLength={2000}
            rows={1}
          />
          <button type="button" onClick={dm.handleSend} disabled={dm.sending}>
            전송
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="floating-chat-widget-dm floating-chat-widget-dm-with-modal">
      <div className="floating-chat-widget-dm-tabs">
        <button
          type="button"
          className={dm.leftTab === 'conversations' ? 'active' : ''}
          onClick={() => dm.setLeftTab('conversations')}
        >
          최근 대화
        </button>
        <button type="button" className={dm.leftTab === 'friends' ? 'active' : ''} onClick={() => dm.setLeftTab('friends')}>
          친구
        </button>
      </div>
      <div className="floating-chat-widget-dm-scroll chat-popup-scroll">
        {dm.leftTab === 'conversations' && (
          <>
            {dm.convLoading ? (
              <div className="floating-chat-widget-empty">불러오는 중...</div>
            ) : dm.conversations.length === 0 ? (
              <div className="floating-chat-widget-empty">대화 내역이 없습니다.</div>
            ) : (
              <ul className="floating-chat-widget-list">
                {dm.conversations.map((c) => (
                  <li key={c.otherUserId}>
                    <button
                      type="button"
                      className="floating-chat-widget-list-btn floating-chat-widget-list-btn--row"
                      onClick={() => {
                        dm.selectConversation(c);
                      }}
                    >
                      <span className="floating-chat-widget-list-col">
                        <span className="floating-chat-widget-list-title">{c.nickname || c.loginId || '유저'}</span>
                        <span className="floating-chat-widget-list-sub">
                          {c.lastMessage ? (c.lastMessage.length > 40 ? `${c.lastMessage.slice(0, 40)}…` : c.lastMessage) : '—'}
                        </span>
                      </span>
                      {(c.unreadCount ?? 0) > 0 ? (
                        <span className="floating-chat-widget-conv-unread">{c.unreadCount! > 99 ? '99+' : c.unreadCount}</span>
                      ) : null}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
        {dm.leftTab === 'friends' && (
          <>
            {dm.friendsLoading ? (
              <div className="floating-chat-widget-empty">불러오는 중...</div>
            ) : dm.friends.length === 0 ? (
              <div className="floating-chat-widget-empty">친구가 없습니다.</div>
            ) : (
              <ul className="floating-chat-widget-list">
                {dm.friends.map((f) => (
                  <li key={f.id}>
                    <button
                      type="button"
                      className="floating-chat-widget-list-btn"
                      onClick={() => setFriendProfileModal(f)}
                    >
                      <span className="floating-chat-widget-list-title">{f.nickname || f.loginId}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>

      {friendProfileModal ? (
        <div
          className="floating-chat-widget-friend-profile-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="floating-friend-profile-name"
          onClick={closeFriendProfileModal}
        >
            <div
              className="floating-chat-widget-friend-profile-card"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                type="button"
                className="floating-chat-widget-friend-profile-close"
                aria-label="닫기"
                onClick={closeFriendProfileModal}
              >
                ×
              </button>
              <div className="floating-chat-widget-friend-profile-avatar">
                {resolveProfileImageUrl(friendProfileModal.profileImageUrl) ? (
                  <img src={resolveProfileImageUrl(friendProfileModal.profileImageUrl)!} alt="" />
                ) : (
                  <span>{(friendProfileModal.nickname || friendProfileModal.loginId || '?')[0]}</span>
                )}
              </div>
              <div className="floating-chat-widget-friend-profile-name-row">
                <h3 id="floating-friend-profile-name" className="floating-chat-widget-friend-profile-name">
                  {friendProfileModal.nickname || friendProfileModal.loginId}
                </h3>
                <button
                  type="button"
                  className="floating-chat-widget-friend-profile-block"
                  disabled={blockBusy}
                  onClick={handleBlockFromFriendModal}
                >
                  {blockBusy ? '…' : '차단'}
                </button>
              </div>
              <div className="floating-chat-widget-friend-profile-desc-wrap">
                {friendProfileBio.loading ? (
                  <p className="floating-chat-widget-friend-profile-desc">자기소개 불러오는 중…</p>
                ) : friendProfileBio.error ? (
                  <p className="floating-chat-widget-friend-profile-desc floating-chat-widget-friend-profile-desc--error">
                    {friendProfileBio.error}
                  </p>
                ) : (
                  <p className="floating-chat-widget-friend-profile-desc">
                    {friendProfileBio.text.trim()
                      ? friendProfileBio.text
                      : '작성한 자기소개가 없습니다.'}
                  </p>
                )}
              </div>
            </div>
          </div>
      ) : null}
    </div>
  );
}

export default function FloatingChatWidget() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [activeMode, setActiveMode] = useState<ActiveChatMode | null>(null);
  const [dmUnreadCount, setDmUnreadCount] = useState(0);

  const fetchDmUnreadCount = useCallback(() => {
    fetch(apiUrl('api/notifications/dm-count'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { count: 0 }))
      .then((d: { count?: number }) => setDmUnreadCount(Number(d.count ?? 0)))
      .catch(() => setDmUnreadCount(0));
  }, []);

  useEffect(() => {
    if (!user) {
      setDmUnreadCount(0);
      return;
    }
    fetchDmUnreadCount();
    const t = window.setInterval(fetchDmUnreadCount, 20000);
    const onDmUnread = () => fetchDmUnreadCount();
    window.addEventListener('gamematcher-dm-unread-changed', onDmUnread);
    return () => {
      window.clearInterval(t);
      window.removeEventListener('gamematcher-dm-unread-changed', onDmUnread);
    };
  }, [user, fetchDmUnreadCount]);

  const openMode = useCallback((mode: ActiveChatMode) => {
    setActiveMode(mode);
    if (mode === 'ONE_ON_ONE') fetchDmUnreadCount();
  }, [fetchDmUnreadCount]);

  const closePanel = useCallback(() => {
    setActiveMode(null);
    fetchDmUnreadCount();
  }, [fetchDmUnreadCount]);

  const toggleMenu = useCallback(() => {
    setIsOpen((wasOpen) => {
      if (wasOpen) {
        setActiveMode(null);
        fetchDmUnreadCount();
        return false;
      }
      return true;
    });
  }, [fetchDmUnreadCount]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (activeMode !== null) closePanel();
      else setIsOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [activeMode, closePanel]);

  const panelOpen = activeMode !== null;

  return (
    <div className="floating-chat-widget-root">
      {panelOpen && activeMode && (
        <ChatPopup title={MODE_TITLES[activeMode]} onClose={closePanel} titleId="floating-chat-popup-title">
          {activeMode === 'ONE_ON_ONE' && <FloatingOneOnOnePanel />}
          {activeMode === 'RANDOM' && (
            <div className="chat-popup-random-wrap">
              <RandomMatchChatBody enabled embedMatchChat />
            </div>
          )}
          {activeMode === 'GROUP' && <FloatingGroupPanel onRoomNavigate={closePanel} />}
          {activeMode === 'CHATBOT' && <FloatingChatbotPanel />}
        </ChatPopup>
      )}

      <div className="floating-chat-widget-buttons">
        <div className={`floating-chat-widget-menu ${isOpen ? 'is-open' : ''}`} aria-hidden={!isOpen}>
          {MENU.map(({ key, label, Icon }) => (
            <div className="floating-chat-widget-row" key={key}>
              <span className="floating-chat-widget-label">{label}</span>
              <button
                type="button"
                className={`floating-chat-widget-fab ${activeMode === key ? 'is-active' : ''}`}
                aria-label={label}
                aria-pressed={activeMode === key}
                onClick={() => openMode(key)}
              >
                <Icon />
              </button>
            </div>
          ))}
        </div>
        <button
          type="button"
          className={`floating-chat-widget-toggle ${user && dmUnreadCount > 0 ? 'has-dm-unread' : ''}`}
          aria-label={isOpen ? '메뉴 닫기' : '메뉴 열기'}
          aria-expanded={isOpen}
          onClick={toggleMenu}
        >
          {user && dmUnreadCount > 0 ? (
            <span className="floating-chat-widget-n-badge" aria-hidden>
              N
            </span>
          ) : null}
          {isOpen ? (
            <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden className="floating-chat-widget-toggle-icon">
              <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden className="floating-chat-widget-toggle-icon">
              <path
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinejoin="round"
                d="M4 5.5h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-4.5l-3.5 3v-3H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z"
              />
            </svg>
          )}
        </button>
      </div>
    </div>
  );
}
