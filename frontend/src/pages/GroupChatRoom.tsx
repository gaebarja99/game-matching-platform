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

interface MemberProfile {
  id: number;
  loginId?: string;
  nickname?: string;
  profileImageUrl?: string;
  bio?: string;
  error?: string;
}

type ReportReason = 'SPAM' | 'HARASSMENT' | 'INAPPROPRIATE_CONTENT' | 'CHEATING' | 'IMPERSONATION' | 'HATE_SPEECH' | 'OTHER';

const REPORT_REASON_OPTIONS: Array<{ value: ReportReason; label: string }> = [
  { value: 'SPAM', label: '스팸' },
  { value: 'HARASSMENT', label: '괴롭힘' },
  { value: 'INAPPROPRIATE_CONTENT', label: '부적절한 콘텐츠' },
  { value: 'CHEATING', label: '부정행위' },
  { value: 'IMPERSONATION', label: '사칭' },
  { value: 'HATE_SPEECH', label: '혐오 발언' },
  { value: 'OTHER', label: '기타' },
];

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
  const [memberActionBusy, setMemberActionBusy] = useState<string | null>(null);
  const [selectedMember, setSelectedMember] = useState<MemberItem | null>(null);
  const [profileModal, setProfileModal] = useState<MemberProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [reportTarget, setReportTarget] = useState<MemberItem | null>(null);
  const [kickConfirmTarget, setKickConfirmTarget] = useState<MemberItem | null>(null);
  const [reportReason, setReportReason] = useState<ReportReason>('HARASSMENT');
  const [reportDescription, setReportDescription] = useState('');
  const [reportSubmitting, setReportSubmitting] = useState(false);
  const [reportError, setReportError] = useState('');
  const [failedAvatarMsgIds, setFailedAvatarMsgIds] = useState<Set<number>>(new Set());
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompRef = useRef<Client | null>(null);
  const presenceEnteredRef = useRef(false);
  const presenceLeaveSentRef = useRef(false);

  const scrollToBottom = useCallback(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), []);

  const displayName = useCallback((member?: { userId: number; nickname?: string | null; loginId?: string | null }) => {
    if (!member) return '유저';
    if (user && member.userId === user.id) {
      return user.nickname?.trim() || user.username?.trim() || member.nickname || member.loginId || '유저';
    }
    return member.nickname?.trim() || member.loginId?.trim() || '유저';
  }, [user]);

  const handleLeaveRoom = async () => {
    if (!gameRoomId || !user || leaving) return;
    setLeaveError(null);
    setLeaving(true);
    const result = await leaveGameRoom(gameRoomId);
    setLeaving(false);
    if (result.ok) {
      navigate(backTo);
      return;
    }
    setLeaveError(result.message || '방 나가기에 실패했습니다.');
  };

  const fetchRoomAndMembers = useCallback(() => {
    if (!user || !roomId) return;
    Promise.all([
      fetch(apiUrl('api/group-chat/rooms'), { credentials: 'include' }).then((response) => (response.ok ? response.json() : { list: [] })),
      fetch(apiUrl(`api/group-chat/rooms/${roomId}/members`), { credentials: 'include' }).then((response) => {
        if (response.status === 403) return { forbidden: true };
        return response.ok ? response.json() : { list: [] };
      }),
    ]).then(([roomsResponse, membersResponse]) => {
      const rooms = Array.isArray(roomsResponse?.list) ? roomsResponse.list : [];
      const room = rooms.find((item: { id: number; name?: string; createdByUserId?: number }) => item.id === roomId);
      setRoomName(room?.name || '그룹 채팅방');
      setHostUserId(typeof room?.createdByUserId === 'number' ? room.createdByUserId : null);
      if ((membersResponse as { forbidden?: boolean }).forbidden) {
        setForbidden(true);
        setLoading(false);
        return;
      }
      const nextMembers = Array.isArray((membersResponse as { list?: MemberItem[] })?.list)
        ? (membersResponse as { list: MemberItem[] }).list
        : [];
      setMembers(nextMembers);
      setLoading(false);
    });
  }, [roomId, user]);

  const fetchMessages = useCallback(() => {
    if (!user || !roomId) return;
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/messages?limit=50`), { credentials: 'include' })
      .then((response) => {
        if (response.status === 403) {
          setForbidden(true);
          return { list: [] };
        }
        return response.ok ? response.json() : { list: [] };
      })
      .then((payload: { list?: MessageItem[] }) => {
        setMessages(Array.isArray(payload?.list) ? payload.list : []);
      })
      .catch(() => setMessages([]));
  }, [roomId, user]);

  useEffect(() => {
    if (!user || !roomId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setForbidden(false);
    fetchRoomAndMembers();
    fetchMessages();
  }, [fetchMessages, fetchRoomAndMembers, roomId, user]);

  useEffect(() => {
    if (!user || !roomId || forbidden) return;
    const timer = window.setInterval(() => {
      fetchRoomAndMembers();
      fetchMessages();
    }, 3000);
    return () => window.clearInterval(timer);
  }, [fetchMessages, fetchRoomAndMembers, forbidden, roomId, user]);

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
      fetch(url, { method: 'POST', credentials: 'include', keepalive: true }).catch(() => {});
    };

    window.addEventListener('pagehide', sendLeave);
    return () => {
      window.removeEventListener('pagehide', sendLeave);
      sendLeave();
    };
  }, [forbidden, roomId, user]);

  useEffect(() => {
    if (!user || !roomId || forbidden) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(getWsUrl()) as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/group-room/${roomId}`, (message) => {
          if (!message?.body) return;
          try {
            const payload = JSON.parse(message.body) as { type?: string; userId?: number };
            if (payload.type === 'MESSAGE') {
              const nextMessage = payload as unknown as MessageItem;
              setMessages((prev) => (prev.some((item) => item.id === nextMessage.id) ? prev : [...prev, nextMessage]));
              setTimeout(scrollToBottom, 50);
            }
            if (payload.type === 'MEMBER_JOINED' || payload.type === 'MEMBER_LEFT' || payload.type === 'MEMBER_KICKED') {
              if (payload.type === 'MEMBER_KICKED' && payload.userId === user.id) {
                window.alert('방장에서 강퇴되었습니다.');
                navigate(backTo);
                return;
              }
              fetchRoomAndMembers();
            }
          } catch {
            // ignore malformed payload
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
  }, [backTo, fetchRoomAndMembers, forbidden, navigate, roomId, scrollToBottom, user]);

  useEffect(() => {
    scrollToBottom();
  }, [messages.length, scrollToBottom]);

  const sendMessage = (event: React.FormEvent) => {
    event.preventDefault();
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
      .then((response) => response.json().catch(() => ({})).then((data) => ({ ok: response.ok, data: data as { message?: string } })))
      .then(({ ok, data }) => {
        if (!ok) {
          setSendError(data.message || '채팅 전송에 실패했습니다.');
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
      .then((response) => (response.ok ? response.json() : []))
      .then((list: FriendItem[]) => setFriends(Array.isArray(list) ? list : []))
      .catch(() => setFriends([]));
  };

  const memberIds = new Set(members.map((member) => member.userId));
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
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, message: data.message })))
      .then(({ ok }) => {
        if (ok) setInviteOpen(false);
      })
      .finally(() => setInvitingId(null));
  };

  const kickMember = (targetUserId: number, nickname: string) => {
    if (!roomId || !isHost || !user || targetUserId === user.id || kickingId != null) return;
    if (!window.confirm(`${nickname || '해당 유저'}님을 강퇴할까요?`)) return;
    setKickingId(targetUserId);
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/kick`), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: targetUserId }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, message: data.message })))
      .then(({ ok, message }) => {
        if (!ok) {
          window.alert(message || '강퇴에 실패했습니다.');
          return;
        }
        fetchRoomAndMembers();
      })
      .catch(() => window.alert('강퇴 요청에 실패했습니다.'))
      .finally(() => setKickingId(null));
  };

  const openMemberProfile = async (member: MemberItem) => {
    setProfileLoading(true);
    setProfileModal({
      id: member.userId,
      nickname: displayName(member),
      loginId: member.loginId,
      profileImageUrl: member.profileImageUrl ?? undefined,
    });
    try {
      const response = await fetch(apiUrl(`api/friends/${member.userId}/public-profile`), { credentials: 'include' });
      const payload = await response.json().catch(() => ({} as { id?: number; loginId?: string; nickname?: string; profileImageUrl?: string; bio?: string; message?: string }));
      if (!response.ok) {
        setProfileModal((current) => current ? { ...current, error: payload.message ?? '프로필 정보를 불러오지 못했습니다.' } : current);
        return;
      }
      setProfileModal({
        id: payload.id ?? member.userId,
        loginId: payload.loginId ?? member.loginId,
        nickname: payload.nickname ?? displayName(member),
        profileImageUrl: payload.profileImageUrl ?? member.profileImageUrl ?? undefined,
        bio: payload.bio ?? '',
      });
    } finally {
      setProfileLoading(false);
    }
  };

  const sendFriendRequest = async (targetUserId: number) => {
    setMemberActionBusy(`friend-${targetUserId}`);
    try {
      const response = await fetch(apiUrl('api/friends/requests'), {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: targetUserId }),
      });
      const payload = await response.json().catch(() => ({} as { message?: string }));
      if (!response.ok) {
        window.alert(payload.message ?? '친구 추가에 실패했습니다.');
        return;
      }
      window.alert('친구 요청을 보냈습니다.');
    } finally {
      setMemberActionBusy(null);
    }
  };

  const blockMember = async (targetUserId: number) => {
    if (!window.confirm('이 사용자를 차단할까요?')) return;
    setMemberActionBusy(`block-${targetUserId}`);
    try {
      const response = await fetch(apiUrl('api/friends/block'), {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: targetUserId }),
      });
      const payload = await response.json().catch(() => ({} as { message?: string }));
      if (!response.ok) {
        window.alert(payload.message ?? '차단에 실패했습니다.');
        return;
      }
      window.alert('차단했습니다.');
    } finally {
      setMemberActionBusy(null);
    }
  };

  const submitReport = async () => {
    if (!user?.id || !reportTarget?.userId) return;
    const detail = reportDescription.trim();
    if (!detail) {
      setReportError('상세 내용을 입력해 주세요.');
      return;
    }
    setReportSubmitting(true);
    setReportError('');
    try {
      const response = await fetch(apiUrl(`api/users/${user.id}/reports`), {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reportedUserId: reportTarget.userId,
          reason: reportReason,
          description: detail,
        }),
      });
      const payload = await response.json().catch(() => ({} as { message?: string }));
      if (!response.ok) {
        setReportError(payload.message ?? '신고 접수에 실패했습니다.');
        return;
      }
      window.alert('신고가 접수되었습니다.');
      setReportTarget(null);
      setReportDescription('');
    } finally {
      setReportSubmitting(false);
    }
  };

  const executeKickMember = (targetUserId: number, nickname: string) => {
    if (!roomId || !isHost || !user || targetUserId === user.id || kickingId != null) return;
    setKickingId(targetUserId);
    fetch(apiUrl(`api/group-chat/rooms/${roomId}/kick`), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: targetUserId }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, message: data.message })))
      .then(({ ok, message }) => {
        if (!ok) {
          window.alert(message || '강퇴에 실패했습니다.');
          return;
        }
        setKickConfirmTarget(null);
        setSelectedMember(null);
        fetchRoomAndMembers();
      })
      .catch(() => window.alert(`${nickname || '해당 유저'}님 강퇴 요청에 실패했습니다.`))
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

  const selectedMemberId = selectedMember?.userId ?? null;

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
                <button type="button" className="group-chat-leave-btn btn-secondary" onClick={handleLeaveRoom} disabled={leaving}>
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
          <aside className="group-chat-members member-sidebar-enhanced">
            <h3>멤버 ({members.length})</h3>
            <ul>
              {members.map((member) => {
                const name = displayName(member);
                const isMe = member.userId === user.id;
                const active = selectedMemberId === member.userId;
                return (
                  <li key={member.userId} className={`group-chat-member-entry ${active ? 'is-active' : ''}`}>
                    <button
                      type="button"
                      className="group-chat-member-item group-chat-member-button"
                      onClick={() => setSelectedMember((current) => (current?.userId === member.userId ? null : member))}
                    >
                      {resolveProfileImageUrl(member.profileImageUrl) ? (
                        <img src={resolveProfileImageUrl(member.profileImageUrl)!} alt="" className="group-chat-member-avatar" />
                      ) : (
                        <span className="group-chat-member-initial">{name[0]}</span>
                      )}
                      <span>{name}{isMe ? ' (나)' : ''}</span>
                    </button>
                    {active && !isMe ? (
                      <div className="member-action-card">
                        <button type="button" className="member-action-btn" onClick={() => void openMemberProfile(member)}>
                          정보 보기
                        </button>
                        <button type="button" className="member-action-btn" onClick={() => navigate(`/dm?with=${member.userId}`)}>
                          1:1 메시지
                        </button>
                        <button type="button" className="member-action-btn" disabled={memberActionBusy === `friend-${member.userId}`} onClick={() => void sendFriendRequest(member.userId)}>
                          친구 추가
                        </button>
                        <button type="button" className="member-action-btn" disabled={memberActionBusy === `block-${member.userId}`} onClick={() => void blockMember(member.userId)}>
                          차단하기
                        </button>
                        <button
                          type="button"
                          className="member-action-btn danger"
                          onClick={() => {
                            setReportTarget(member);
                            setReportReason('HARASSMENT');
                            setReportDescription('');
                            setReportError('');
                          }}
                        >
                          신고하기
                        </button>
                        {isHost ? (
                          <button
                            type="button"
                            className="member-action-btn danger"
                            disabled={kickingId === member.userId}
                            onClick={() => setKickConfirmTarget(member)}
                          >
                            {kickingId === member.userId ? '강퇴 중...' : '강퇴하기'}
                          </button>
                        ) : null}
                      </div>
                    ) : null}
                  </li>
                );
              })}
            </ul>
          </aside>

          <div className="group-chat-messages-wrap">
            <div className="group-chat-messages">
              {messages.map((message) => {
                const isSystem = message.system === true || message.id === 0 || message.fromUserId === 0;
                return (
                  <div
                    key={message.id || `sys-${message.fromUserId}-${message.createdAt}`}
                    className={`group-chat-msg ${!isSystem && message.fromUserId === user.id ? 'mine' : ''} ${isSystem ? 'system' : ''}`}
                  >
                    {!isSystem ? (
                      <div className="group-chat-msg-avatar">
                        {resolveProfileImageUrl(message.fromProfileImageUrl) && !failedAvatarMsgIds.has(message.id) ? (
                          <img
                            src={resolveProfileImageUrl(message.fromProfileImageUrl)!}
                            alt=""
                            onError={() => setFailedAvatarMsgIds((prev) => new Set(prev).add(message.id))}
                          />
                        ) : (
                          <span className="group-chat-msg-initial">{(message.fromNickname || '유저')[0]}</span>
                        )}
                      </div>
                    ) : null}
                    <div className="group-chat-msg-body">
                      {!isSystem ? <span className="group-chat-msg-name">{message.fromNickname}</span> : null}
                      <p className="group-chat-msg-text">{message.text}</p>
                      <span className="group-chat-msg-time">
                        {message.createdAt ? new Date(message.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : ''}
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
                placeholder="메시지를 입력해 주세요."
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
                {friends.filter((friend) => !memberIds.has(friend.id)).map((friend) => (
                  <li key={friend.id} className="group-chat-invite-friend-item">
                    {resolveProfileImageUrl(friend.profileImageUrl) ? (
                      <img src={resolveProfileImageUrl(friend.profileImageUrl)!} alt="" />
                    ) : (
                      <span className="group-chat-member-initial">{(friend.nickname || friend.loginId || '유저')[0]}</span>
                    )}
                    <span>{friend.nickname || friend.loginId || '유저'}</span>
                    <button
                      type="button"
                      className="btn-primary btn-sm"
                      disabled={invitingId === friend.id}
                      onClick={() => inviteFriend(friend.id)}
                    >
                      {invitingId === friend.id ? '초대 중...' : '초대'}
                    </button>
                  </li>
                ))}
              </ul>
              {friends.filter((friend) => !memberIds.has(friend.id)).length === 0 ? (
                <p className="group-chat-empty">초대할 수 있는 친구가 없습니다.</p>
              ) : null}
            </div>
          </div>
        )}

        {profileModal && (
          <div className="main-modal-backdrop show" role="dialog" aria-modal="true" onClick={() => setProfileModal(null)}>
            <div className="main-modal-box" onClick={(event) => event.stopPropagation()}>
              <h2 className="modal-title">멤버 정보</h2>
              <div className="member-profile-modal">
                {resolveProfileImageUrl(profileModal.profileImageUrl) ? (
                  <img src={resolveProfileImageUrl(profileModal.profileImageUrl)!} alt="" className="member-profile-avatar" />
                ) : (
                  <div className="member-profile-avatar member-profile-avatar-fallback">{(profileModal.nickname || '유저')[0]}</div>
                )}
                <div className="member-profile-copy">
                  <strong>{profileModal.nickname || '유저'}</strong>
                  {profileModal.loginId ? <span>{profileModal.loginId}</span> : null}
                </div>
              </div>
              <div className="modal-desc">
                {profileLoading ? '프로필을 불러오는 중...' : profileModal.error || profileModal.bio || '공개된 소개가 없습니다.'}
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setProfileModal(null)}>
                  닫기
                </button>
              </div>
            </div>
          </div>
        )}

        {reportTarget && (
          <div className="main-modal-backdrop show" role="dialog" aria-modal="true" onClick={() => setReportTarget(null)}>
            <div className="main-modal-box" onClick={(event) => event.stopPropagation()}>
              <h2 className="modal-title">{displayName(reportTarget)} 신고하기</h2>
              <div className="modal-field">
                <label htmlFor="group-report-reason">신고 사유</label>
                <select id="group-report-reason" value={reportReason} onChange={(e) => setReportReason(e.target.value as ReportReason)}>
                  {REPORT_REASON_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="modal-field">
                <label htmlFor="group-report-description">상세 내용</label>
                <textarea
                  id="group-report-description"
                  className="watch-report-textarea"
                  value={reportDescription}
                  onChange={(e) => setReportDescription(e.target.value)}
                  rows={5}
                  maxLength={500}
                  placeholder="신고 사유를 자세히 입력해 주세요."
                />
              </div>
              {reportError ? <div className="modal-error">{reportError}</div> : null}
              <div className="modal-actions">
                <button type="button" className="btn-primary" onClick={() => void submitReport()} disabled={reportSubmitting}>
                  {reportSubmitting ? '접수 중...' : '신고 접수'}
                </button>
                <button type="button" className="btn-secondary" onClick={() => setReportTarget(null)} disabled={reportSubmitting}>
                  취소
                </button>
              </div>
            </div>
          </div>
        )}

        {kickConfirmTarget && (
          <div
            className="main-modal-backdrop show"
            role="dialog"
            aria-modal="true"
            onClick={() => kickingId == null && setKickConfirmTarget(null)}
          >
            <div className="main-modal-box" onClick={(event) => event.stopPropagation()}>
              <h2 className="modal-title">강퇴하기</h2>
              <div className="modal-desc">
                <strong>{displayName(kickConfirmTarget)}</strong>님을 이 채팅방에서 강퇴할까요?
              </div>
              <div className="modal-desc" style={{ marginTop: 8 }}>
                강퇴된 멤버는 현재 방에서 즉시 퇴장됩니다.
              </div>
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-primary"
                  disabled={kickingId === kickConfirmTarget.userId}
                  onClick={() => executeKickMember(kickConfirmTarget.userId, displayName(kickConfirmTarget))}
                >
                  {kickingId === kickConfirmTarget.userId ? '강퇴 중…' : '강퇴하기'}
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={kickingId === kickConfirmTarget.userId}
                  onClick={() => setKickConfirmTarget(null)}
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
