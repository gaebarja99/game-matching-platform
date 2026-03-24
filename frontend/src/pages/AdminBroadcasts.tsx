import { useEffect, useState } from 'react';
import type { AdminBroadcastRow } from '../api/admin';
import { fetchAdminBroadcasts, updateAdminBroadcast } from '../api/admin';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';

function formatDate(value?: string | null) {
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

function tone(status: AdminBroadcastRow['status']) {
  switch (status) {
    case 'LIVE':
      return 'tone-success';
    case 'ENDED':
      return 'tone-muted';
    default:
      return 'tone-warning';
  }
}

export default function AdminBroadcasts() {
  const { user, loading: authLoading } = useAuth();
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('all');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<{
    content: AdminBroadcastRow[];
    totalPages: number;
    first: boolean;
    last: boolean;
  } | null>(null);

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminBroadcasts({ query, status, page, size: 12 });
    setResult(
      response.ok && response.data
        ? response.data
        : { content: [], totalPages: 0, first: true, last: true }
    );
    setLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [user, query, status, page]);

  const handleAction = async (
    streamId: number,
    action: 'warn' | 'force_end' | 'hide_recent' | 'restore_recent'
  ) => {
    const message =
      action === 'warn' || action === 'force_end'
        ? window.prompt(
            action === 'warn'
              ? '경고 문구를 입력해 주세요.'
              : '강제 종료 안내 문구를 입력해 주세요.',
            ''
          ) ?? ''
        : '';

    if ((action === 'warn' || action === 'force_end') && !message.trim()) {
      const proceed = window.confirm('문구 없이 기본 안내로 처리할까요?');
      if (!proceed) return;
    }

    const response = await updateAdminBroadcast(streamId, action, message.trim() || undefined);
    if (!response.ok) {
      window.alert(response.error ?? '처리에 실패했습니다.');
      return;
    }

    await load();
  };

  if (authLoading) {
    return (
      <AdminLayout title="방송 관리" description="방송 운영 화면을 불러오는 중입니다.">
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
      title="방송 관리"
      description="방송 경고, 강제 종료, 최근 방송 내역 숨김과 복구를 한 화면에서 처리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="방송 제목, 아이디, 닉네임, 게임 검색"
          />
          <select
            className="admin-filter-select"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="all">전체 상태</option>
            <option value="live">방송 중</option>
            <option value="created">준비 중</option>
            <option value="ended">종료됨</option>
            <option value="hidden_recent">최근 숨김</option>
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
              setStatus('all');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-broadcasts-table">
          <colgroup>
            <col style={{ width: '22%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '10%' }} />
            <col style={{ width: '11%' }} />
            <col style={{ width: '16%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '15%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>방송</th>
              <th>스트리머</th>
              <th>게임</th>
              <th>상태</th>
              <th>방송 시간</th>
              <th>운영 상태</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="empty-msg">
                  불러오는 중입니다.
                </td>
              </tr>
            ) : !result || result.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-msg">
                  조건에 맞는 방송이 없습니다.
                </td>
              </tr>
            ) : (
              result.content.map((stream) => (
                <tr key={stream.id}>
                  <td>
                    <div>{stream.title || '-'}</div>
                    <div className="admin-subtext">방송번호 #{stream.id}</div>
                  </td>
                  <td>
                    <div>{stream.displayName || '-'}</div>
                    <div className="admin-subtext">{shortenLoginId(stream.loginId || '-')}</div>
                  </td>
                  <td>{stream.game || '-'}</td>
                  <td>
                    <span className={`admin-status-badge ${tone(stream.status)}`}>
                      {stream.statusLabel}
                    </span>
                  </td>
                  <td>
                    <div>시작 {formatDate(stream.startedAt || stream.createdAt)}</div>
                    <div className="admin-subtext">종료 {formatDate(stream.endedAt)}</div>
                  </td>
                  <td>
                    <div>최근 방송 {stream.visibleInRecent ? '노출' : '숨김'}</div>
                    <div className="admin-subtext">
                      경고 {stream.warningCount}회
                      {stream.lastWarningAt ? ` · ${formatDate(stream.lastWarningAt)}` : ''}
                    </div>
                    {stream.lastWarningMessage ? (
                      <div className="admin-subtext admin-warning-preview">
                        {stream.lastWarningMessage}
                      </div>
                    ) : null}
                  </td>
                  <td>
                    <div className="admin-inline-actions">
                      <button
                        type="button"
                        className="admin-action-btn"
                        onClick={() => void handleAction(stream.id, 'warn')}
                      >
                        경고
                      </button>
                      {stream.status !== 'ENDED' ? (
                        <button
                          type="button"
                          className="admin-action-btn danger"
                          onClick={() => void handleAction(stream.id, 'force_end')}
                        >
                          강제 종료
                        </button>
                      ) : null}
                      <button
                        type="button"
                        className={`admin-action-btn ${stream.visibleInRecent ? '' : 'primary'}`}
                        onClick={() =>
                          void handleAction(
                            stream.id,
                            stream.visibleInRecent ? 'hide_recent' : 'restore_recent'
                          )
                        }
                      >
                        {stream.visibleInRecent ? '최근 숨김' : '최근 복구'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        <div className="admin-pagination">
          <button
            type="button"
            className="admin-action-btn"
            disabled={result?.first ?? true}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            이전
          </button>
          <span>
            페이지 {page + 1}
            {result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}
          </span>
          <button
            type="button"
            className="admin-action-btn"
            disabled={result?.last ?? true}
            onClick={() => setPage((current) => current + 1)}
          >
            다음
          </button>
        </div>
      </section>
    </AdminLayout>
  );
}
