import { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import {
  fetchGameRoomList,
  createGameRoom,
  joinGameRoom,
  leaveGameRoom,
  closeGameRoom,
  deleteGameRoom,
  getGameRoomChatRoomId,
  type GameRoomItem,
} from '../api/gameRooms';
import { isHiddenGameRoomHost } from '../utils/gameRoomVisibility';
import { getRoomCapacityMeta, resolveRoomMaxPlayers } from '../utils/gameRoomCapacity';

function parsePlatformAndParty(s?: string | null): { platform?: string; partySize?: string } {
  if (!s || !s.trim()) return {};
  try {
    const o = JSON.parse(s) as { platform?: unknown; partySize?: unknown };
    return {
      platform: typeof o.platform === 'string' ? o.platform : undefined,
      partySize: typeof o.partySize === 'string' ? o.partySize : undefined,
    };
  } catch {
    return {};
  }
}

const GAME_OPTIONS: { key: string; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'LEAGUE_OF_LEGENDS', label: '리그오브레전드' },
  { key: 'VALORANT', label: '발로란트' },
  { key: 'OVERWATCH', label: '오버워치2' },
  { key: 'PUBG', label: 'PUBG' },
  { key: 'COUNTER_STRIKE_2', label: 'CS2' },
];

export default function GameRooms() {
  const { user } = useAuth();
  const { theme } = useTheme();
  const navigate = useNavigate();
  const [list, setList] = useState<GameRoomItem[]>([]);
  const [gameFilter, setGameFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createTitle, setCreateTitle] = useState('');
  const [createMemo, setCreateMemo] = useState('');
  const [createPassword, setCreatePassword] = useState('');
  const [createGame, setCreateGame] = useState('LEAGUE_OF_LEGENDS');
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);
  const [actionRoomId, setActionRoomId] = useState<number | null>(null);
  const [deleteModal, setDeleteModal] = useState<{ roomId: number; password: string } | null>(null);
  const [deleteError, setDeleteError] = useState('');
  const [deleting, setDeleting] = useState(false);

  const fetchList = useCallback(() => {
    setLoading(true);
    fetchGameRoomList(gameFilter === 'ALL' ? undefined : gameFilter, false)
      .then((rows) => setList(rows.filter((r) => !isHiddenGameRoomHost(r.hostNickname))))
      .finally(() => setLoading(false));
  }, [gameFilter]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      fetchList();
    }, 4000);
    return () => window.clearInterval(timer);
  }, [fetchList]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      navigate('/login');
      return;
    }
    const title = createTitle.trim();
    const password = createPassword.trim();
    if (!title) {
      setCreateError('제목을 입력해 주세요.');
      return;
    }
    if (!password) {
      setCreateError('삭제용 비밀번호를 입력해 주세요.');
      return;
    }
    setCreating(true);
    setCreateError('');
    try {
      const res = await createGameRoom({
        title,
        memo: createMemo.trim() || undefined,
        deletePassword: password,
        game: createGame,
      });
      if (res.ok) {
        setShowCreateModal(false);
        setCreateTitle('');
        setCreateMemo('');
        setCreatePassword('');
        setCreateGame('LEAGUE_OF_LEGENDS');
        fetchList();
      } else {
        setCreateError(res.message || '방 만들기에 실패했습니다.');
      }
    } catch {
      setCreateError('방 만들기에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const handleJoin = async (r: GameRoomItem) => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (getRoomCapacityMeta(r).isFull) {
      window.alert('이미 정원이 가득 찬 방입니다.');
      return;
    }
    setActionRoomId(r.id);
    const { ok, message } = await joinGameRoom(r.id);
    setActionRoomId(null);
    if (ok) {
      const chatRoomId = await getGameRoomChatRoomId(r.id);
      if (chatRoomId != null) navigate(`/group-chat/room/${chatRoomId}`, { state: { fromGameRoom: true, gameRoomId: r.id } });
    } else if (message) {
      window.alert(message);
    }
    fetchList();
  };

  const handleLeave = async (roomId: number) => {
    setActionRoomId(roomId);
    const res = await leaveGameRoom(roomId);
    setActionRoomId(null);
    if (res.ok) fetchList();
  };

  const handleClose = async (roomId: number) => {
    setActionRoomId(roomId);
    const ok = await closeGameRoom(roomId);
    setActionRoomId(null);
    if (ok) fetchList();
  };

  const handleDeleteConfirm = async () => {
    if (!deleteModal) return;
    if (!deleteModal.password.trim()) {
      setDeleteError('삭제용 비밀번호를 입력하세요.');
      return;
    }
    setDeleting(true);
    setDeleteError('');
    const { ok, message } = await deleteGameRoom(deleteModal.roomId, deleteModal.password);
    if (ok) {
      setDeleteModal(null);
      fetchList();
    } else {
      setDeleteError(message || '삭제에 실패했습니다. 비밀번호를 확인하세요.');
    }
    setDeleting(false);
  };

  const closeDeleteModal = () => {
    if (!deleting) {
      setDeleteModal(null);
      setDeleteError('');
    }
  };

  const formatDate = (s: string) => {
    try {
      return new Date(s).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return s;
    }
  };

  if (!user) {
    return (
      <Layout>
        <div className="duo-section">
          <p>로그인하면 게임방 목록을 볼 수 있습니다.</p>
          <Link to="/login" className="btn-write">로그인</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <section className="duo-section">
        <div className="duo-section-header">
          <h2 className="duo-section-title">게임방</h2>
          <div className="duo-filters">
            <div className="game-tabs" style={{ marginBottom: 0 }}>
              {GAME_OPTIONS.map((g) => (
                <button
                  key={g.key}
                  type="button"
                  className={gameFilter === g.key ? 'active' : ''}
                  onClick={() => setGameFilter(g.key)}
                >
                  {g.label}
                </button>
              ))}
            </div>
            <button type="button" className="btn-write" onClick={() => setShowCreateModal(true)}>
              방 만들기
            </button>
          </div>
        </div>
        {loading ? (
          <div className="duo-empty">로딩 중…</div>
        ) : list.length === 0 ? (
          <div className="duo-empty">열린 방이 없습니다. &quot;방 만들기&quot;로 방을 만들어 보세요.</div>
        ) : (
          <div className="game-room-list">
            {list.map((r) => {
              const capMeta = getRoomCapacityMeta(r);
              const maxCap = resolveRoomMaxPlayers(r);
              return (
              <div key={r.id} className="game-room-card">
                <div className="game-room-card-head">
                  <span className="game-room-title">{r.title}</span>
                  {r.closed && <span className="game-room-badge closed">마감</span>}
                  <span className="game-room-game">{GAME_OPTIONS.find((g) => g.key === r.game)?.label ?? r.game}</span>
                </div>
                {r.memo && <p className="game-room-memo">{r.memo}</p>}
                <div className="game-room-meta">
                  {r.game === 'PUBG' && (() => {
                    const p = parsePlatformAndParty(r.gameOptions);
                    const label = p.platform === 'KAKAO' ? '카카오' : p.platform === 'STEAM' ? '스팀' : null;
                    return label ? (
                      <span style={{ fontWeight: 800, color: '#00e676' }}>{label}</span>
                    ) : null;
                  })()}
                  <span>방장: {r.hostNickname ?? '—'}</span>
                  <span>
                    인원:{' '}
                    {maxCap != null ? `${r.memberCount}/${maxCap}` : r.memberCount}
                  </span>
                  <span>{formatDate(r.createdAt)}</span>
                </div>
                <div className="game-room-actions">
                  {r.groupChatRoomId && r.isMember && (
                    <Link to={`/group-chat/room/${r.groupChatRoomId}`} state={{ fromGameRoom: true, gameRoomId: r.id }} className="btn-room-chat">
                      방 채팅
                    </Link>
                  )}
                  {user.id === r.hostUserId ? (
                    <>
                      {!r.closed && (
                        <button
                          type="button"
                          className="btn-room-close"
                          onClick={() => handleClose(r.id)}
                          disabled={actionRoomId === r.id}
                        >
                          마감
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn-room-delete"
                        onClick={() => setDeleteModal({ roomId: r.id, password: '' })}
                      >
                        삭제
                      </button>
                    </>
                  ) : (
                    !r.closed && !r.isMember && (
                      <button
                        type="button"
                        className={`btn-room-join${capMeta.isFull ? ' btn-room-join--full' : ''}`}
                        onClick={() => handleJoin(r)}
                        disabled={actionRoomId === r.id || capMeta.isFull}
                      >
                        {actionRoomId === r.id ? '처리 중…' : capMeta.isFull ? '모집 완료' : '참가'}
                      </button>
                    )
                  )}
                  {user.id !== r.hostUserId && r.isMember && (
                    <button
                      type="button"
                      className="btn-room-leave"
                      onClick={() => handleLeave(r.id)}
                      disabled={actionRoomId === r.id}
                    >
                      나가기
                    </button>
                  )}
                </div>
              </div>
            );
            })}
          </div>
        )}
      </section>

      {showCreateModal && (
        <div className="modal-backdrop" onClick={() => !creating && setShowCreateModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h3>방 만들기</h3>
            <form onSubmit={handleCreate}>
              <label>제목 (필수)</label>
              <input
                type="text"
                value={createTitle}
                onChange={(e) => setCreateTitle(e.target.value)}
                placeholder="방 제목"
                maxLength={200}
              />
              <label>메모</label>
              <textarea
                value={createMemo}
                onChange={(e) => setCreateMemo(e.target.value)}
                placeholder="방 설명"
                rows={3}
              />
              <label>삭제용 비밀번호 (필수)</label>
              <input
                type="password"
                value={createPassword}
                onChange={(e) => setCreatePassword(e.target.value)}
                placeholder="삭제 시 입력할 비밀번호"
              />
              <label>게임</label>
              <select value={createGame} onChange={(e) => setCreateGame(e.target.value)}>
                {GAME_OPTIONS.filter((g) => g.key !== 'ALL').map((g) => (
                  <option key={g.key} value={g.key}>{g.label}</option>
                ))}
              </select>
              {createError && <p className="form-error">{createError}</p>}
              <div className="modal-actions">
                <button type="button" onClick={() => !creating && setShowCreateModal(false)}>취소</button>
                <button type="submit" disabled={creating}>{creating ? '만드는 중…' : '만들기'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteModal &&
        createPortal(
          <div
            className="confirm-dialog-backdrop show"
            role="dialog"
            aria-modal="true"
            aria-labelledby="room-delete-dialog-title"
            onClick={closeDeleteModal}
          >
            <div
              className={`confirm-dialog-box theme-${theme}`}
              onClick={(e) => e.stopPropagation()}
              style={{
                background: theme === 'dark' ? '#1e1e21' : '#fff',
                border: theme === 'dark' ? '1px solid #38383c' : '1px solid #e5e5e7',
                color: theme === 'dark' ? '#e8e8ec' : '#141418',
                width: 'min(calc(100vw - 32px), 320px)',
                maxWidth: 320,
              }}
            >
              <h3 id="room-delete-dialog-title" className="confirm-dialog-message" style={{ fontWeight: 600, marginBottom: 8 }}>
                방 삭제
              </h3>
              <p className="confirm-dialog-message" style={{ marginBottom: 12 }}>
                삭제용 비밀번호를 입력하세요.
              </p>
              <input
                type="password"
                value={deleteModal.password}
                onChange={(e) => setDeleteModal({ ...deleteModal, password: e.target.value })}
                placeholder="삭제 비밀번호"
                disabled={deleting}
                autoComplete="current-password"
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  marginBottom: 8,
                  borderRadius: 8,
                  border: theme === 'dark' ? '1px solid #45454a' : '1px solid #d8d8dc',
                  background: theme === 'dark' ? '#38383c' : '#f5f5f7',
                  color: theme === 'dark' ? '#fff' : '#141418',
                  fontSize: '0.9rem',
                  boxSizing: 'border-box',
                }}
              />
              {deleteError && (
                <p className="form-error" style={{ marginBottom: 12 }}>{deleteError}</p>
              )}
              <div className="confirm-dialog-actions">
                <button
                  type="button"
                  className="confirm-dialog-btn cancel"
                  onClick={closeDeleteModal}
                  disabled={deleting}
                >
                  취소
                </button>
                <button
                  type="button"
                  className="confirm-dialog-btn confirm"
                  onClick={handleDeleteConfirm}
                  disabled={deleting}
                >
                  {deleting ? '삭제 중…' : '삭제'}
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </Layout>
  );
}
