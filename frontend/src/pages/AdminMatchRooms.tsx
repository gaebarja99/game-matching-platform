import { useEffect, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { deleteAdminMatchRoom, fetchAdminMatchRooms } from '../api/admin';
import type { AdminMatchRoomRow } from '../api/admin';

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function shortenLoginId(loginId: string) {
  if (!loginId) return '-';
  const socialPrefixes = ['google_', 'naver_', 'kakao_'];
  const matchedPrefix = socialPrefixes.find((prefix) => loginId.startsWith(prefix));
  if (!matchedPrefix) return loginId;
  const rest = loginId.slice(matchedPrefix.length);
  if (rest.length <= 5) return loginId;
  return `${matchedPrefix}${rest.slice(0, 5)}...`;
}

export default function AdminMatchRooms() {
  const { user, loading: authLoading } = useAuth();
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [game, setGame] = useState('all');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<{
    content: AdminMatchRoomRow[];
    totalPages: number;
    first: boolean;
    last: boolean;
  } | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminMatchRooms({ query, game, page, size: 12 });
    setResult(response.ok && response.data ? response.data : { content: [], totalPages: 0, first: true, last: true });
    setLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [user, query, game, page]);

  const handleDelete = async (roomId: number) => {
    const ok = window.confirm('이 매칭방을 삭제할까요?');
    if (!ok) return;
    const response = await deleteAdminMatchRoom(roomId);
    if (response.ok) {
      await load();
    } else {
      window.alert(response.error ?? '삭제에 실패했습니다.');
    }
  };

  if (authLoading) {
    return (
      <AdminLayout title="매칭방 관리" description="관리자 권한과 매칭방 데이터를 확인하는 중입니다.">
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중입니다.</p>
        </section>
      </AdminLayout>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  return (
    <AdminLayout
      title="매칭방 관리"
      description="모집글 검색, 게임 필터, 작성자 확인과 운영 삭제를 한 화면에서 처리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="소환사명, 작성자 아이디, 닉네임, 메모 검색"
          />
          <select className="admin-filter-select" value={game} onChange={(event) => setGame(event.target.value)}>
            <option value="all">전체 게임</option>
            <option value="league_of_legends">리그 오브 레전드</option>
            <option value="tft">TFT</option>
            <option value="valorant">발로란트</option>
            <option value="pubg">배틀그라운드</option>
            <option value="overwatch">오버워치</option>
            <option value="steam">스팀</option>
            <option value="others">기타</option>
          </select>
          <button
            type="button"
            className="admin-action-btn primary"
            onClick={() => {
              setQuery(queryInput.trim());
              setPage(0);
            }}
          >
            검색
          </button>
          <button
            type="button"
            className="admin-action-btn"
            onClick={() => {
              setQueryInput('');
              setQuery('');
              setGame('all');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-match-rooms-table">
          <colgroup>
            <col style={{ width: '14%' }} />
            <col style={{ width: '18%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '16%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '10%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>소환사명</th>
              <th>작성자</th>
              <th>게임/티어</th>
              <th>포지션</th>
              <th>모드/메모</th>
              <th>등록일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="empty-msg">불러오는 중입니다.</td></tr>
            ) : !result || result.content.length === 0 ? (
              <tr><td colSpan={7} className="empty-msg">조건에 맞는 매칭방이 없습니다.</td></tr>
            ) : (
              result.content.map((room) => (
                <tr key={room.id}>
                  <td>{room.summonerName || '-'}</td>
                  <td>
                    <div>{room.ownerName || '-'}</div>
                    <div className="admin-subtext">{shortenLoginId(room.ownerLoginId || '-')}</div>
                  </td>
                  <td>
                    <div>{room.game || '-'}</div>
                    <div className="admin-subtext">{room.tier || '-'}</div>
                  </td>
                  <td>
                    <div>주포지션 {room.mainPosition || '-'}</div>
                    <div className="admin-subtext">찾는 포지션 {room.findPosition || '-'}</div>
                  </td>
                  <td>
                    <div>{room.mode || '-'}</div>
                    <div className="admin-subtext">{room.memo || '-'}</div>
                  </td>
                  <td>{formatDate(room.createdAt)}</td>
                  <td>
                    <button type="button" className="admin-action-btn danger" onClick={() => void handleDelete(room.id)}>
                      삭제
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        <div className="admin-pagination">
          <button type="button" className="admin-action-btn" disabled={result?.first ?? true} onClick={() => setPage((current) => Math.max(0, current - 1))}>이전</button>
          <span>페이지 {page + 1}{result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}</span>
          <button type="button" className="admin-action-btn" disabled={result?.last ?? true} onClick={() => setPage((current) => current + 1)}>다음</button>
        </div>
      </section>
    </AdminLayout>
  );
}
