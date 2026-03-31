import { useEffect, useState } from 'react';
import type { AdminStreamerRow } from '../api/admin';
import { fetchAdminStreamers, updateAdminStreamerTier } from '../api/admin';
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

export default function AdminStreamers() {
  const { user, loading: authLoading } = useAuth();
  const isAdmin = ['ADMIN', 'ROLE_ADMIN'].includes((user?.role ?? '').toUpperCase());
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [tier, setTier] = useState('all');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<{
    content: AdminStreamerRow[];
    totalPages: number;
    first: boolean;
    last: boolean;
  } | null>(null);

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminStreamers({ query, tier, page, size: 12 });
    setResult(
      response.ok && response.data
        ? response.data
        : { content: [], totalPages: 0, first: true, last: true }
    );
    setLoading(false);
  };

  useEffect(() => {
    if (!isAdmin) return;
    void load();
  }, [isAdmin, query, tier, page]);

  const handleTierChange = async (streamerId: number, nextTier: 'GENERAL' | 'PARTNER') => {
    const response = await updateAdminStreamerTier(streamerId, nextTier);
    if (!response.ok) {
      window.alert(response.message ?? '등급 변경에 실패했습니다.');
      return;
    }
    await load();
  };

  if (authLoading) {
    return (
      <AdminLayout title="스트리머 관리" description="스트리머 정보를 불러오는 중입니다.">
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중입니다.</p>
        </section>
      </AdminLayout>
    );
  }

  if (!isAdmin) {
    return null;
  }

  return (
    <AdminLayout
      title="스트리머 관리"
      description="스트리머 아이디, 방송 활동, 받은 팡, 권한 등급을 운영 전용 화면에서 관리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="아이디 또는 이름 검색"
          />
          <select
            className="admin-filter-select"
            value={tier}
            onChange={(event) => setTier(event.target.value)}
          >
            <option value="all">전체 등급</option>
            <option value="general">일반 스트리머</option>
            <option value="partner">파트너 스트리머</option>
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
              setTier('all');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>아이디</th>
              <th>이름</th>
              <th>구분</th>
              <th>가입일</th>
              <th>받은 팡</th>
              <th>변경</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="empty-msg">불러오는 중입니다.</td>
              </tr>
            ) : !result || result.content.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty-msg">조건에 맞는 스트리머가 없습니다.</td>
              </tr>
            ) : (
              result.content.map((streamer) => {
                const currentTier = streamer.streamerTier ?? 'GENERAL';
                const tierLabel = currentTier === 'PARTNER' ? '파트너 스트리머 (20%)' : '일반 스트리머 (30%)';
                return (
                  <tr key={streamer.id}>
                    <td>{shortenLoginId(streamer.loginId || '-')}</td>
                    <td>{streamer.displayName || '-'}</td>
                    <td>
                      <span className={`admin-status-badge ${currentTier === 'PARTNER' ? 'tone-info' : 'tone-muted'}`}>
                        {tierLabel}
                      </span>
                    </td>
                    <td>{formatDate(streamer.createdAt)}</td>
                    <td>{(streamer.totalReceivedPang ?? 0).toLocaleString()}</td>
                    <td>
                      <div className="admin-inline-actions">
                        <button
                          type="button"
                          className={`admin-action-btn ${currentTier === 'GENERAL' ? 'primary' : ''}`}
                          onClick={() => void handleTierChange(streamer.id, 'GENERAL')}
                        >
                          일반
                        </button>
                        <button
                          type="button"
                          className={`admin-action-btn ${currentTier === 'PARTNER' ? 'primary' : ''}`}
                          onClick={() => void handleTierChange(streamer.id, 'PARTNER')}
                        >
                          파트너
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
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
