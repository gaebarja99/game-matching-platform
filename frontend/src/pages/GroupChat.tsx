import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

interface RoomItem {
  id: number;
  name: string;
  createdByUserId: number;
  memberCount: number;
}

interface InvitationItem {
  id: number;
  roomId: number;
  roomName: string;
  fromUserId: number;
  fromNickname: string;
  fromLoginId?: string;
  createdAt: string;
}

export default function GroupChat() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [rooms, setRooms] = useState<RoomItem[]>([]);
  const [invitations, setInvitations] = useState<InvitationItem[]>([]);
  const [roomsLoading, setRoomsLoading] = useState(true);
  const [invitationsLoading, setInvitationsLoading] = useState(true);
  const [createName, setCreateName] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [actionId, setActionId] = useState<number | null>(null);

  const fetchRooms = useCallback(() => {
    if (!user) return;
    setRoomsLoading(true);
    fetch(apiUrl('api/group-chat/rooms'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: RoomItem[] }) => setRooms(Array.isArray(d?.list) ? d.list : []))
      .catch(() => setRooms([]))
      .finally(() => setRoomsLoading(false));
  }, [user]);

  const fetchInvitations = useCallback(() => {
    if (!user) return;
    setInvitationsLoading(true);
    fetch(apiUrl('api/group-chat/invitations/received'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { list: [] }))
      .then((d: { list?: InvitationItem[] }) => {
        setInvitations(Array.isArray(d?.list) ? d.list : []);
        window.dispatchEvent(new Event('group-chat-invitation-updated'));
      })
      .catch(() => setInvitations([]))
      .finally(() => setInvitationsLoading(false));
  }, [user]);

  useEffect(() => {
    if (!user) {
      setRooms([]);
      setInvitations([]);
      return;
    }
    fetchRooms();
    fetchInvitations();
  }, [user, fetchRooms, fetchInvitations]);

  const handleCreateRoom = (e: React.FormEvent) => {
    e.preventDefault();
    const name = createName.trim();
    if (!name) {
      setCreateError('방 이름을 입력해 주세요.');
      return;
    }
    setCreating(true);
    setCreateError('');
    fetch(apiUrl('api/group-chat/rooms'), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name }),
    })
      .then((r) => r.json().then((d: { id?: number; message?: string }) => ({ ok: r.ok, ...d })))
      .then(({ ok, id, message }) => {
        if (ok && id) {
          setCreateName('');
          fetchRooms();
          navigate(`/group-chat/room/${id}`);
        } else {
          setCreateError(message || '방 만들기에 실패했습니다.');
        }
      })
      .catch(() => setCreateError('방 만들기에 실패했습니다.'))
      .finally(() => setCreating(false));
  };

  const handleAccept = (invitationId: number) => {
    setActionId(invitationId);
    fetch(apiUrl(`api/group-chat/invitations/${invitationId}/accept`), {
      method: 'POST',
      credentials: 'include',
    })
      .then((r) => {
        if (r.ok) {
          const inv = invitations.find((i) => i.id === invitationId);
          setInvitations((prev) => prev.filter((i) => i.id !== invitationId));
          window.dispatchEvent(new Event('group-chat-invitation-updated'));
          if (inv) navigate(`/group-chat/room/${inv.roomId}`);
        }
      })
      .finally(() => setActionId(null));
  };

  const handleReject = (invitationId: number) => {
    setActionId(invitationId);
    fetch(apiUrl(`api/group-chat/invitations/${invitationId}/reject`), {
      method: 'POST',
      credentials: 'include',
    })
      .then((r) => {
        if (r.ok) {
          setInvitations((prev) => prev.filter((i) => i.id !== invitationId));
          window.dispatchEvent(new Event('group-chat-invitation-updated'));
        }
      })
      .finally(() => setActionId(null));
  };

  if (!user) {
    return (
      <Layout>
        <div className="group-chat-page">
          <h1 className="page-title">단체 채팅</h1>
          <p className="group-chat-msg">로그인하면 채팅방을 만들고 친구를 초대할 수 있습니다.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="group-chat-page">
        <h1 className="page-title">단체 채팅</h1>

        <section className="group-chat-section">
          <div className="group-chat-section-head">
            <h2>내 채팅방</h2>
            <form className="group-chat-create" onSubmit={handleCreateRoom}>
              <input
                type="text"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
                placeholder="방 이름"
                maxLength={100}
                className="group-chat-input"
              />
              <button type="submit" disabled={creating} className="btn-primary">
                {creating ? '만드는 중…' : '방 만들기'}
              </button>
            </form>
            {createError && <p className="group-chat-error">{createError}</p>}
          </div>
          {roomsLoading ? (
            <p className="group-chat-loading">로딩 중...</p>
          ) : rooms.length === 0 ? (
            <p className="group-chat-empty">참여 중인 채팅방이 없습니다. 방을 만들거나 초대를 수락해 보세요.</p>
          ) : (
            <ul className="group-chat-room-list">
              {rooms.map((r) => (
                <li key={r.id}>
                  <Link to={`/group-chat/room/${r.id}`} className="group-chat-room-item">
                    <span className="group-chat-room-name">{r.name}</span>
                    <span className="group-chat-room-meta">{r.memberCount}명</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="group-chat-section">
          <h2>초대받은 목록</h2>
          {invitationsLoading ? (
            <p className="group-chat-loading">로딩 중...</p>
          ) : invitations.length === 0 ? (
            <p className="group-chat-empty">받은 초대가 없습니다.</p>
          ) : (
            <ul className="group-chat-invitation-list">
              {invitations.map((inv) => (
                <li key={inv.id} className="group-chat-invitation-item">
                  <div className="group-chat-invitation-info">
                    <strong>{inv.roomName}</strong>
                    <span>{inv.fromNickname || inv.fromLoginId || '?'}님이 초대했습니다.</span>
                  </div>
                  <div className="group-chat-invitation-actions">
                    <button
                      type="button"
                      className="btn-primary"
                      disabled={actionId === inv.id}
                      onClick={() => handleAccept(inv.id)}
                    >
                      수락
                    </button>
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={actionId === inv.id}
                      onClick={() => handleReject(inv.id)}
                    >
                      거절
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </Layout>
  );
}
