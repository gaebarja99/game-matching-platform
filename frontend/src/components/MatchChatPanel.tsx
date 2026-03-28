import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, getWsUrl, resolveProfileImageUrl } from '../api/client';
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
  OVERWATCH: '오버워치 2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: 'CS2',
};

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

type MatchMemberProfile = {
  id: number;
  loginId?: string;
  nickname?: string;
  profileImageUrl?: string;
  bio?: string;
  error?: string;
};

export type MatchChatPanelProps = {
  sessionId: number;
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
  const [selectedMember, setSelectedMember] = useState<MatchSessionMember | null>(null);
  const [memberActionBusy, setMemberActionBusy] = useState<string | null>(null);
  const [profileModal, setProfileModal] = useState<MatchMemberProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [reportTarget, setReportTarget] = useState<MatchSessionMember | null>(null);
  const [reportReason, setReportReason] = useState<ReportReason>('HARASSMENT');
  const [reportDescription, setReportDescription] = useState('');
  const [reportSubmitting, setReportSubmitting] = useState(false);
  const [reportError, setReportError] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompRef = useRef<Client | null>(null);

  const sessionIdStr = String(sessionId);

  const displayNameForUser = useCallback((serverUserId: number, serverNickname?: string | null) => {
    if (user && serverUserId === user.id) {
      const nickname = user.nickname?.trim();
      if (nickname) return nickname;
      const username = user.username?.trim();
      if (username) return username;
    }
    return serverNickname?.trim() || '유저';
  }, [user]);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    if (typeof sessionStorage === 'undefined') return;
    try {
      setHasLeftSession(sessionStorage.getItem(`match-left-${sessionIdStr}`) === '1');
    } catch {
      setHasLeftSession(false);
    }
  }, [sessionIdStr]);

  const fetchSession = useCallback(async () => {
    if (!user || !sessionId) return;
    try {
      const nextSession = await getMatchSession(sessionId);
      if (nextSession) {
        setSession(nextSession);
        setForbidden(false);
        return;
      }
      setForbidden(true);
    } finally {
      setLoading(false);
    }
  }, [sessionId, user]);

  const fetchMessages = useCallback(async () => {
    if (!user || !sessionId) return;
    const nextMessages = await getMatchChatMessages(sessionId);
    setMessages(nextMessages);
  }, [sessionId, user]);

  useEffect(() => {
    if (!user || !sessionId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setForbidden(false);
    void fetchSession();
    void fetchMessages();
  }, [fetchMessages, fetchSession, sessionId, user]);

  useEffect(() => {
    if (!user || !sessionId || forbidden) return;
    const timer = window.setInterval(() => {
      void fetchSession();
      void fetchMessages();
    }, 3000);
    return () => window.clearInterval(timer);
  }, [fetchMessages, fetchSession, forbidden, sessionId, user]);

  useEffect(() => {
    if (!user?.id || !sessionId || forbidden) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(getWsUrl()) as unknown as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/match/${sessionId}`, (message) => {
          if (!message?.body) return;
          try {
            const payload = JSON.parse(message.body) as {
              type?: string;
              id?: number;
              fromUserId?: number;
              fromNickname?: string;
              fromProfileImageUrl?: string;
              text?: string;
              createdAt?: string;
            };
            if (payload.type !== 'MESSAGE' || payload.text == null) return;
            const fromUserId = payload.fromUserId;
            if (fromUserId === undefined || fromUserId === null) return;
            const text = payload.text;
            setMessages((prev) => {
              if (prev.some((item) => item.id === payload.id)) return prev;
              return [
                ...prev,
                {
                  id: payload.id ?? Date.now(),
                  sessionId,
                  fromUserId,
                  fromNickname: payload.fromNickname ?? '',
                  fromProfileImageUrl: payload.fromProfileImageUrl,
                  text,
                  createdAt: payload.createdAt ?? '',
                },
              ];
            });
            setTimeout(scrollToBottom, 50);
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
  }, [forbidden, scrollToBottom, sessionId, user?.id]);

  useEffect(() => {
    if (messages.length > 0) scrollToBottom();
  }, [messages.length, scrollToBottom]);

  const sendMessage = async (event: React.FormEvent) => {
    event.preventDefault();
    const text = input.trim();
    if (!text || !sessionId || sending || hasLeftSession) return;
    setSendError(null);
    setSending(true);
    const result = await sendMatchChatMessage(sessionId, text);
    setSending(false);
    if (result.ok) {
      setInput('');
      void fetchMessages();
      return;
    }
    setSendError(result.message || '채팅 전송에 실패했습니다.');
  };

  const handleLeaveRoom = () => {
    if (typeof sessionStorage !== 'undefined') {
      try {
        sessionStorage.setItem(`match-left-${sessionIdStr}`, '1');
      } catch {
        // ignore
      }
    }
    setHasLeftSession(true);
  };

  const goBack = () => {
    if (onBack) onBack();
    else navigate('/');
  };

  const openMemberProfile = async (member: MatchSessionMember) => {
    const targetUserId = member.userId;
    if (!targetUserId) return;
    setProfileLoading(true);
    setProfileModal({
      id: targetUserId,
      nickname: displayNameForUser(targetUserId, member.nickname),
      profileImageUrl: member.profileImageUrl,
    });
    try {
      const response = await fetch(apiUrl(`api/friends/${targetUserId}/public-profile`), { credentials: 'include' });
      const payload = await response.json().catch(() => ({} as { id?: number; loginId?: string; nickname?: string; profileImageUrl?: string; bio?: string; message?: string }));
      if (!response.ok) {
        setProfileModal((current) => current ? { ...current, error: payload.message ?? '프로필 정보를 불러오지 못했습니다.' } : current);
        return;
      }
      setProfileModal({
        id: payload.id ?? targetUserId,
        loginId: payload.loginId,
        nickname: payload.nickname || displayNameForUser(targetUserId, member.nickname),
        profileImageUrl: payload.profileImageUrl || member.profileImageUrl,
        bio: payload.bio || '',
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
        <div className="match-chat-panel-loading">로딩 중...</div>
      </div>
    );
  }

  if (forbidden || !session) {
    return (
      <div className={embedded ? 'match-chat-panel-embedded-inner' : ''}>
        <p className="match-chat-panel-error">랜덤 매칭 채팅방에 접근할 수 없습니다.</p>
        <button type="button" onClick={goBack}>
          목록으로
        </button>
      </div>
    );
  }

  const title = `랜덤 매칭 채팅 - ${GAME_LABELS[session.game] ?? session.game}`;
  const selectedMemberId = selectedMember?.userId ?? null;

  return (
    <div className={`group-chat-room-page ${embedded ? 'match-chat-panel-embedded' : ''}`}>
      <header className="group-chat-room-header">
        <button type="button" className="group-chat-back" onClick={goBack}>
          ← 목록
        </button>
        <h1 className="group-chat-room-title">{title}</h1>
      </header>

      <div className="group-chat-room-body">
        <aside className="group-chat-members member-sidebar-enhanced">
          <h3>멤버 ({session.members.length})</h3>
          <ul>
            {session.members.map((member) => {
              const label = displayNameForUser(member.userId, member.nickname);
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
                      <span className="group-chat-member-initial">{label[0]}</span>
                    )}
                    <span>{label}{isMe ? ' (나)' : ''}</span>
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
                    </div>
                  ) : null}
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
              <button type="button" className="group-chat-leave-btn btn-secondary" onClick={handleLeaveRoom}>
                방 나가기
              </button>
            )}
          </div>
        </aside>

        <div className="group-chat-messages-wrap">
          <div className="group-chat-messages">
            {messages.map((message) => {
              const senderName = displayNameForUser(message.fromUserId, message.fromNickname);
              return (
                <div key={message.id} className={`group-chat-msg ${message.fromUserId === user.id ? 'mine' : ''}`}>
                  <div className="group-chat-msg-avatar">
                    {resolveProfileImageUrl(message.fromProfileImageUrl) && !failedAvatarMsgIds.has(message.id) ? (
                      <img
                        src={resolveProfileImageUrl(message.fromProfileImageUrl)!}
                        alt=""
                        onError={() => setFailedAvatarMsgIds((prev) => new Set(prev).add(message.id))}
                      />
                    ) : (
                      <span className="group-chat-msg-initial">{senderName[0]}</span>
                    )}
                  </div>
                  <div className="group-chat-msg-body">
                    <span className="group-chat-msg-name">{senderName}</span>
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
          {hasLeftSession ? (
            <div className="group-chat-readonly-notice">방을 나간 상태입니다. 이전 대화만 볼 수 있습니다.</div>
          ) : (
            <>
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
            </>
          )}
        </div>
      </div>

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
            <h2 className="modal-title">{displayNameForUser(reportTarget.userId, reportTarget.nickname)} 신고하기</h2>
            <div className="modal-field">
              <label htmlFor="match-report-reason">신고 사유</label>
              <select id="match-report-reason" value={reportReason} onChange={(e) => setReportReason(e.target.value as ReportReason)}>
                {REPORT_REASON_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="modal-field">
              <label htmlFor="match-report-description">상세 내용</label>
              <textarea
                id="match-report-description"
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
    </div>
  );
}
