import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../contexts/AuthContext';
import { getWsUrl, resolveProfileImageUrl } from '../api/client';
import {
  getMatchSession,
  getMatchChatMessages,
  sendMatchChatMessage,
  type MatchSessionMember,
  type MatchSessionInfo,
  type MatchChatMessage as MatchChatMessageType,
} from '../api/match';

const GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: 'CS2',
};

export type MatchChatPanelProps = {
  sessionId: number;
  /** 위젯 등 인라인 표시 시 레이아웃·뒤로가기 동작 분기 */
  embedded?: boolean;
  onBack?: () => void;
};

export default function MatchChatPanel({ sessionId, embedded, onBack }: MatchChatPanelProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [session, setSession] = useState<MatchSessionInfo | null>(null);
  const [messages, setMessages] = useState<MatchChatMessageType[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [forbidden, setForbidden] = useState(false);
  const [failedAvatarMsgIds, setFailedAvatarMsgIds] = useState<Set<number>>(new Set());
  const [hasLeftSession, setHasLeftSession] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompRef = useRef<Client | null>(null);

  const sessionIdStr = String(sessionId);

  /** 프로필에서 닉네임을 바꾼 직후에도 멤버/내 말풍선에 즉시 반영 (세션 스냅샷은 옛 이름일 수 있음) */
  const displayNameForUser = (serverUserId: number, serverNickname?: string | null) => {
    if (user && serverUserId === user.id) {
      const n = user.nickname?.trim();
      if (n) return n;
      const un = user.username?.trim();
      if (un) return un;
    }
    return serverNickname?.trim() || '—';
  };

  useEffect(() => {
    if (typeof sessionStorage === 'undefined') return;
    try {
      setHasLeftSession(sessionStorage.getItem('match-left-' + sessionIdStr) === '1');
    } catch {}
  }, [sessionIdStr]);

  const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });

  const fetchSession = useCallback(() => {
    if (!user || !sessionId) return;
    const delay = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));
    const maxAttempts = 6;
    (async () => {
      try {
        for (let attempt = 0; attempt < maxAttempts; attempt++) {
          const s = await getMatchSession(sessionId);
          if (s) {
            setSession(s);
            setForbidden(false);
            return;
          }
          if (attempt < maxAttempts - 1) {
            await delay(100 * (attempt + 1));
          }
        }
        setForbidden(true);
      } catch {
        setForbidden(true);
      } finally {
        setLoading(false);
      }
    })();
  }, [user, sessionId]);

  const fetchMessages = useCallback(() => {
    if (!user || !sessionId) return;
    getMatchChatMessages(sessionId).then(setMessages);
  }, [user, sessionId]);

  useEffect(() => {
    if (!user || !sessionId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setForbidden(false);
    fetchSession();
    fetchMessages();
  }, [user, sessionId, fetchSession, fetchMessages]);

  useEffect(() => {
    messages.length && scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (!user?.id || !sessionId || forbidden) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(getWsUrl()) as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/match/' + sessionId, (message) => {
          if (!message?.body) return;
          try {
            const d = JSON.parse(message.body) as {
              type?: string;
              id?: number;
              fromUserId?: number;
              fromNickname?: string;
              fromProfileImageUrl?: string;
              text?: string;
              createdAt?: string;
            };
            if (d.type === 'MESSAGE' && d.text != null) {
              const text = d.text;
              setMessages((prev) => {
                if (prev.some((m) => m.id === d.id)) return prev;
                const msg: MatchChatMessageType = {
                  id: d.id!,
                  sessionId,
                  fromUserId: d.fromUserId!,
                  fromNickname: d.fromNickname ?? '',
                  fromProfileImageUrl: d.fromProfileImageUrl,
                  text,
                  createdAt: d.createdAt ?? '',
                };
                return [...prev, msg];
              });
              setTimeout(scrollToBottom, 50);
            }
          } catch {}
        });
      },
    });
    client.activate();
    stompRef.current = client;
    return () => {
      client.deactivate();
      stompRef.current = null;
    };
  }, [user?.id, sessionId, forbidden]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || !sessionId || sending || hasLeftSession) return;
    setSendError(null);
    setSending(true);
    sendMatchChatMessage(sessionId, text).then((res) => {
      setSending(false);
      if (res.ok) {
        setInput('');
        fetchMessages();
        return;
      }
      setSendError(res.message || '채팅 전송에 실패했습니다.');
    });
  };

  const handleLeaveRoom = () => {
    if (typeof sessionStorage !== 'undefined') {
      try {
        sessionStorage.setItem('match-left-' + sessionIdStr, '1');
      } catch {}
    }
    setHasLeftSession(true);
  };

  const goBack = () => {
    if (onBack) onBack();
    else navigate('/');
  };

  if (!user) {
    return (
      <div className={embedded ? 'match-chat-panel-embedded-inner' : ''}>
        <p className="match-chat-panel-login-msg">로그인이 필요합니다.</p>
        <button type="button" className="match-chat-panel-login-btn" onClick={() => navigate('/login')}>
          로그인
        </button>
      </div>
    );
  }

  if (loading) {
    return (
      <div className={embedded ? 'match-chat-panel-embedded-inner' : ''}>
        <div className="match-chat-panel-loading">로딩 중…</div>
      </div>
    );
  }

  if (forbidden || !session) {
    return (
      <div className={embedded ? 'match-chat-panel-embedded-inner' : ''}>
        <p className="match-chat-panel-error">매칭 채팅방에 접근할 수 없습니다.</p>
        <button type="button" onClick={goBack}>
          돌아가기
        </button>
      </div>
    );
  }

  const title = `랜덤 매칭 채팅 — ${GAME_LABELS[session.game] ?? session.game}`;

  return (
    <div className={`group-chat-room-page ${embedded ? 'match-chat-panel-embedded' : ''}`}>
      <header className="group-chat-room-header">
        <button type="button" className="group-chat-back" onClick={goBack}>
          ← 목록
        </button>
        <h1 className="group-chat-room-title">{title}</h1>
      </header>

      <div className="group-chat-room-body">
        <aside className="group-chat-members">
          <h3>멤버 ({session.members.length})</h3>
          <ul>
            {session.members.map((m: MatchSessionMember) => {
              const label = displayNameForUser(m.userId, m.nickname);
              return (
                <li key={m.userId} className="group-chat-member-item">
                  {resolveProfileImageUrl(m.profileImageUrl) ? (
                    <img src={resolveProfileImageUrl(m.profileImageUrl)!} alt="" className="group-chat-member-avatar" />
                  ) : (
                    <span className="group-chat-member-initial">{(label === '—' ? '?' : label)[0]}</span>
                  )}
                  <span>{label}</span>
                </li>
              );
            })}
          </ul>
          <div className="group-chat-members-footer">
            {hasLeftSession ? (
              <button type="button" className="group-chat-leave-btn group-chat-back" onClick={goBack}>
                목록으로
              </button>
            ) : (
              <button type="button" className="group-chat-leave-btn btn-secondary" onClick={handleLeaveRoom} title="채팅방 나가기">
                방 나가기
              </button>
            )}
          </div>
        </aside>

        <div className="group-chat-messages-wrap">
          <div className="group-chat-messages">
            {messages.map((m) => {
              const senderName = displayNameForUser(m.fromUserId, m.fromNickname);
              return (
              <div key={m.id} className={`group-chat-msg ${m.fromUserId === user.id ? 'mine' : ''}`}>
                <div className="group-chat-msg-avatar">
                  {resolveProfileImageUrl(m.fromProfileImageUrl) && !failedAvatarMsgIds.has(m.id) ? (
                    <img
                      src={resolveProfileImageUrl(m.fromProfileImageUrl)!}
                      alt=""
                      onError={() => setFailedAvatarMsgIds((prev) => new Set(prev).add(m.id))}
                    />
                  ) : (
                    <span className="group-chat-msg-initial">{(senderName === '—' ? '?' : senderName)[0]}</span>
                  )}
                </div>
                <div className="group-chat-msg-body">
                  <span className="group-chat-msg-name">{senderName}</span>
                  <p className="group-chat-msg-text">{m.text}</p>
                  <span className="group-chat-msg-time">
                    {m.createdAt ? new Date(m.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : ''}
                  </span>
                </div>
              </div>
            );
            })}
            <div ref={messagesEndRef} />
          </div>
          {hasLeftSession ? (
            <div className="group-chat-readonly-notice">나간 방입니다. 내용만 볼 수 있습니다.</div>
          ) : (
            <>
            <form className="group-chat-send-form" onSubmit={sendMessage}>
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="메시지 입력..."
                maxLength={2000}
                className="group-chat-send-input"
              />
              <button type="submit" disabled={sending || !input.trim()} className="btn-primary">
                전송
              </button>
            </form>
            {sendError && <p className="group-chat-empty" style={{ color: '#dc2626', marginTop: 8 }}>{sendError}</p>}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
