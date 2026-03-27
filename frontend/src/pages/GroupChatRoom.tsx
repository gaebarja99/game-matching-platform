import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, getWsUrl, resolveProfileImageUrl } from '../api/client';
import { leaveGameRoom } from '../api/gameRooms';

interface MemberItem {
  userId: number;
  nickname: string;
  loginId: string;
  profileImageUrl?: string | null;
}

interface MessageItem {
  id: number;
  roomId: number;
  fromUserId: number;
  fromNickname: string;
  fromProfileImageUrl?: string | null;
  text: string;
  createdAt: string;
  system?: boolean;
}

interface FriendItem {
  id: number;
  loginId: string;
  nickname: string;
  profileImageUrl?: string | null;
}

export default function GroupChatRoom() {
  const { roomId: roomIdParam } = useParams<{ roomId: string }>();
  const roomId = roomIdParam ? Number(roomIdParam) : null;
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as { fromGameRoom?: boolean; gameRoomId?: number } | null;
  const fromGameRoom = locationState?.fromGameRoom === true;
  const gameRoomId = locationState?.gameRoomId ?? null;
  const backTo = fromGameRoom ? '/' : '/group-chat';
  const [leaving, setLeaving] = useState(false);
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const [roomName, setRoomName] = useState('');
  const [hostUserId, setHostUserId] = useState<number | null>(null);
  const [members, setMembers] = useState<MemberItem[]>([]);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [forbidden, setForbidden] = useState(false);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [friends, setFriends] = useState<FriendItem[]>([]);
  const [invitingId, setInvitingId] = useState<number | null>(null);
  const [kickingId, setKickingId] = useState<number | null>(null);
  const [failedAvatarMsgIds, setFailedAvatarMsgIds] = useState<Set<number>>(new Set());
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompRef = useRef<Client | null>(null);
  const presenceEnteredRef = useRef(false);
  const presenceLeaveSentRef = useRef(false);

  const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });

  const handleLeaveRoom = async () => {
    if (!gameRoomId || !user || leaving) return;
    setLeaveError(null);
    setLeaving(true);
    const res = await leaveGameRoom(gameRoomId);
    setLeaving(false);
    if (res.ok) {
      navigate(backTo);
    } else {
      setLeaveError(res.message || '나가기 처리에 실패했습니다.');
    }
  };

  const fetchRoomAndMembers = useCallback(() => {
    if (!user || !roomId) return;
    Promise.all([
      fetch(apiUrl('api/group-chat/rooms'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
      fetch(apiUrl(`api/group-chat/rooms/${roomId}/members`), { credentials: 'include' }).then((r) => {
        if (r.status === 403) return { forbidden: true };
        return r.ok ? r.json() : { list: [] };
      }),
    ]).then(([roomsRes, membersRes]) => {
      const list = Array.isArray(roomsRes?.list) ? roomsRes.list : [];
      const room = list.find((r: { id: number; name?: string; createdByUserId?: number }) => r.id === roomId);
      setRoomName(room?.name || '채팅방');
      setHostUserId(typeof room?.createdByUserId === 'number' ? room.createdByUserId : null);
      if ((membersRes as { forbidden?: boolean }).forbidden) {
        setForbidden(true);
        setLoading(false);
        return;
      }
      const memList = Array.isArray((membersRes as { list?: MemberItem[] })?.list)
        ? (membersRes as { list: MemberItem[] }).list
        : [];
      setMembers(memList);
      setLoading(false);
    });
  }, [user, roomId]);

  const fetchMessages = useCallback(() => {
    if (!user || !roomId) return;
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/messages?limit=50`), { credentials: 'include' })
      .then((r) => {
        if (r.status === 403) {
          setForbidden(true);
          return { list: [] };
        }
        return r.ok ? r.json() : { list: [] };
      })
      .then((d: { list?: MessageItem[] }) => setMessages(Array.isArray(d?.list) ? d.list : []))
      .catch(() => setMessages([]));
  }, [user, roomId]);

  useEffect(() => {
    if (!user || !roomId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setForbidden(false);
    fetchRoomAndMembers();
    fetchMessages();
  }, [user, roomId, fetchRoomAndMembers, fetchMessages]);

  useEffect(() => {
    if (!user || !roomId || forbidden || presenceEnteredRef.current) return;

    presenceEnteredRef.current = true;
    presenceLeaveSentRef.current = false;

    fetch(apiUrl(`api/group-chat/rooms/${roomId}/presence/enter`), {
      method: 'POST',
      credentials: 'include',
    }).catch(() => {});

    const sendLeave = () => {
      if (presenceLeaveSentRef.current) return;
      presenceLeaveSentRef.current = true;

      const url = apiUrl(`api/group-chat/rooms/${roomId}/presence/leave`);
      const payload = new Blob([], { type: 'application/json' });
      if (navigator.sendBeacon) {
        navigator.sendBeacon(url, payload);
        return;
      }

      fetch(url, {
        method: 'POST',
        credentials: 'include',
        keepalive: true,
      }).catch(() => {});
    };

    window.addEventListener('pagehide', sendLeave);

    return () => {
      window.removeEventListener('pagehide', sendLeave);
      sendLeave();
    };
  }, [user, roomId, forbidden]);

  useEffect(() => {
    if (!user || !roomId || forbidden) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(getWsUrl()) as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/group-room/' + roomId, (message) => {
          if (!message?.body) return;
          try {
            const d = JSON.parse(message.body) as { type?: string; userId?: number };
            if (d.type === 'MESSAGE') {
              const msg = d as unknown as MessageItem;
              setMessages((prev) => (prev.some((m) => m.id === msg.id) ? prev : [...prev, msg]));
              setTimeout(scrollToBottom, 50);
            }
            if (d.type === 'MEMBER_JOINED' || d.type === 'MEMBER_LEFT') {
              fetchRoomAndMembers();
            }
            if (d.type === 'MEMBER_KICKED') {
              if (d.userId === user.id) {
                alert('방장에서 강퇴되었습니다.');
                navigate(backTo);
                return;
              }
              fetchRoomAndMembers();
            }
          } catch {
            // ignore malformed realtime payloads
          }
        });
      },
    });
    client.activate();
    stompRef.current = client;
    return () => {
      client.deactivate();
      stompRef.current = null;
    };
  }, [user, roomId, forbidden, fetchRoomAndMembers, navigate, backTo]);

  useEffect(() => {
    scrollToBottom();
  }, [messages.length]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || !user || !roomId || sending) return;
    setSendError(null);
    setSending(true);
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/messages`), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    })
      .then((r) => r.json().catch(() => ({})).then((data) => ({ ok: r.ok, data: data as { message?: string } })))
      .then(({ ok, data }) => {
        if (!ok) {
          setSendError(data?.message || '채팅 전송에 실패했습니다.');
          return;
        }
        setInput('');
      })
      .catch(() => setSendError('채팅 전송에 실패했습니다.'))
      .finally(() => setSending(false));
  };

  const openInvite = () => {
    setInviteOpen(true);
    fetch(apiUrl('api/friends/list'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: FriendItem[]) => setFriends(Array.isArray(list) ? list : []))
      .catch(() => setFriends([]));
  };

  const memberIds = new Set(members.map((m) => m.userId));
  const isHost = hostUserId != null && user?.id === hostUserId;

  const inviteFriend = (friendId: number) => {
    if (!roomId || memberIds.has(friendId)) return;
    setInvitingId(friendId);
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/invite`), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: friendId }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, message: d.message })))
      .then(({ ok }) => {
        if (ok) setInviteOpen(false);
      })
      .finally(() => setInvitingId(null));
  };

  const kickMember = (targetUserId: number, nickname: string) => {
    if (!roomId || !isHost || !user || targetUserId === user.id || kickingId != null) return;
    const ok = window.confirm(`${nickname || '해당 유저'}님을 강퇴할까요?`);
    if (!ok) return;
    setKickingId(targetUserId);
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/kick`), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: targetUserId }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, message: d?.message })))
      .then(({ ok, message }) => {
        if (!ok) {
          alert(message || '강퇴에 실패했습니다.');
          return;
        }
        fetchRoomAndMembers();
      })
      .catch(() => alert('강퇴 요청에 실패했습니다.'))
      .finally(() => setKickingId(null));
  };

  if (!user) {
    return (
      <Layout>
        <div className="group-chat-page">
          <p className="group-chat-msg">로그인이 필요합니다.</p>
        </div>
      </Layout>
    );
  }

  if (!roomId) {
    return (
      <Layout>
        <div className="group-chat-page">
          <p className="group-chat-msg">올바르지 않은 방입니다.</p>
          <button type="button" className="btn-primary" onClick={() => navigate(backTo)}>
            목록으로
          </button>
        </div>
      </Layout>
    );
  }

  if (forbidden) {
    return (
      <Layout>
        <div className="group-chat-page">
          <p className="group-chat-msg">이 채팅방에 참여할 수 없습니다.</p>
          <button type="button" className="btn-primary" onClick={() => navigate(backTo)}>
            목록으로
          </button>
        </div>
      </Layout>
    );
  }

  if (loading) {
    return (
      <Layout>
        <div className="group-chat-page">
          <p className="group-chat-loading">로딩 중...</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="group-chat-room-page">
        <header className="group-chat-room-header">
          <button type="button" className="group-chat-back" onClick={() => navigate(backTo)}>
            ← 목록
          </button>
          <h1 className="group-chat-room-title">{roomName}</h1>
          <div className="group-chat-room-header-actions">
            {gameRoomId != null && (
              <>
                <button
                  type="button"
                  className="group-chat-leave-btn btn-secondary"
                  onClick={handleLeaveRoom}
                  disabled={leaving}
                  title="게임방 나가기"
                >
                  {leaving ? '나가는 중...' : '나가기'}
                </button>
                {leaveError && <span className="group-chat-leave-error">{leaveError}</span>}
              </>
            )}
            <button type="button" className="btn-secondary" onClick={openInvite}>
              멤버 초대
            </button>
          </div>
        </header>

        <div className="group-chat-room-body">
          <aside className="group-chat-members">
            <h3>멤버 ({members.length})</h3>
            <ul>
              {members.map((m) => (
                <li key={m.userId} className="group-chat-member-item">
                  {resolveProfileImageUrl(m.profileImageUrl) ? (
                    <img src={resolveProfileImageUrl(m.profileImageUrl)!} alt="" className="group-chat-member-avatar" />
                  ) : (
                    <span className="group-chat-member-initial">{(m.nickname || m.loginId || '?')[0]}</span>
                  )}
                  <span>{m.nickname || m.loginId || '?'}</span>
                  {isHost && m.userId !== user.id && (
                    <button
                      type="button"
                      className="group-chat-member-kick"
                      disabled={kickingId === m.userId}
                      onClick={() => kickMember(m.userId, m.nickname || m.loginId || '')}
                    >
                      {kickingId === m.userId ? '강퇴 중...' : '강퇴'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </aside>

          <div className="group-chat-messages-wrap">
            <div className="group-chat-messages">
              {messages.map((msg) => {
                const isSystem = msg.system === true || msg.id === 0 || msg.fromUserId === 0;
                return (
                  <div
                    key={msg.id || `sys-${msg.fromUserId}-${msg.createdAt}`}
                    className={`group-chat-msg ${!isSystem && msg.fromUserId === user.id ? 'mine' : ''} ${isSystem ? 'system' : ''}`}
                  >
                    {!isSystem && (
                      <div className="group-chat-msg-avatar">
                        {resolveProfileImageUrl(msg.fromProfileImageUrl) && !failedAvatarMsgIds.has(msg.id) ? (
                          <img
                            src={resolveProfileImageUrl(msg.fromProfileImageUrl)!}
                            alt=""
                            onError={() => setFailedAvatarMsgIds((prev) => new Set(prev).add(msg.id))}
                          />
                        ) : (
                          <span className="group-chat-msg-initial">{(msg.fromNickname || '?')[0]}</span>
                        )}
                      </div>
                    )}
                    <div className="group-chat-msg-body">
                      {!isSystem && <span className="group-chat-msg-name">{msg.fromNickname}</span>}
                      <p className="group-chat-msg-text">{msg.text}</p>
                      <span className="group-chat-msg-time">
                        {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>
            <form className="group-chat-send-form" onSubmit={sendMessage}>
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="메시지를 입력하세요."
                maxLength={2000}
                className="group-chat-send-input"
              />
              <button type="submit" disabled={sending || !input.trim()} className="btn-primary">
                전송
              </button>
            </form>
            {sendError && <p className="group-chat-empty" style={{ color: '#dc2626', marginTop: 8 }}>{sendError}</p>}
          </div>
        </div>

        {inviteOpen && (
          <div className="group-chat-invite-modal" role="dialog">
            <div className="group-chat-invite-modal-inner">
              <div className="group-chat-invite-modal-head">
                <h3>친구 초대</h3>
                <button type="button" onClick={() => setInviteOpen(false)}>닫기</button>
              </div>
              <ul className="group-chat-invite-friend-list">
                {friends.filter((f) => !memberIds.has(f.id)).map((f) => (
                  <li key={f.id} className="group-chat-invite-friend-item">
                    {resolveProfileImageUrl(f.profileImageUrl) ? (
                      <img src={resolveProfileImageUrl(f.profileImageUrl)!} alt="" />
                    ) : (
                      <span className="group-chat-member-initial">{(f.nickname || f.loginId || '?')[0]}</span>
                    )}
                    <span>{f.nickname || f.loginId || '?'}</span>
                    <button
                      type="button"
                      className="btn-primary btn-sm"
                      disabled={invitingId === f.id}
                      onClick={() => inviteFriend(f.id)}
                    >
                      {invitingId === f.id ? '초대 중...' : '초대'}
                    </button>
                  </li>
                ))}
              </ul>
              {friends.filter((f) => !memberIds.has(f.id)).length === 0 && (
                <p className="group-chat-empty">초대할 수 있는 친구가 없습니다.</p>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
