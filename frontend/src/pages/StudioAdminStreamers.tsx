import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { fetchAdminStreamers, updateAdminStreamerTier } from '../api/admin';
import type { AdminStreamerRow } from '../api/admin';

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

function streamerTierMeta(tier: AdminStreamerRow['streamerTier']) {
  if (tier === 'PARTNER') {
    return { label: '파트너 스트리머 (20%)', tone: 'tone-accent' };
  }
  return { label: '일반 스트리머 (30%)', tone: 'tone-neutral' };
}

export default function StudioAdminStreamers() {
  const { user, loading: authLoading } = useAuth();
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [tierFilter, setTierFilter] = useState('');
  const [sortKey, setSortKey] = useState('pang-desc');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<{ content: AdminStreamerRow[]; totalPages: number; first: boolean; last: boolean } | null>(null);
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState<number | null>(null);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    setLoading(true);
    fetchAdminStreamers({ query, tier: tierFilter, page, size: 12 })
      .then((response) => {
        setResult(response.ok && response.data ? response.data : { content: [], totalPages: 0, first: true, last: true });
      })
      .finally(() => setLoading(false));
  }, [page, query, tierFilter, user]);

  if (authLoading) {
    return (
      <AdminLayout
        title="스트리머 관리"
        description="관리자 권한과 스트리머 정보를 확인하는 중입니다."
      >
        <section className="admin-panel">
          <p className="admin-subtext">관리자 화면을 불러오는 중...</p>
        </section>
      </AdminLayout>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  const rows = result?.content ?? [];

  const visibleRows = useMemo(() => {
    const nextRows = [...rows];
    nextRows.sort((a, b) => {
      const pangA = Number(a.totalReceivedPang ?? 0);
      const pangB = Number(b.totalReceivedPang ?? 0);
      const createdA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const createdB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      switch (sortKey) {
        case 'created-desc':
          return createdB - createdA;
        case 'created-asc':
          return createdA - createdB;
        case 'pang-desc':
        default:
          return pangB - pangA;
      }
    });
    return nextRows;
  }, [rows, sortKey]);

  const handleTierChange = async (streamerId: number, tier: 'GENERAL' | 'PARTNER') => {
    setSubmittingId(streamerId);
    const response = await updateAdminStreamerTier(streamerId, tier);
    setSubmittingId(null);
    if (!response.ok) {
      window.alert(response.message ?? '등급 변경에 실패했습니다.');
      return;
    }
    const refreshed = await fetchAdminStreamers({ query, tier: tierFilter, page, size: 12 });
    setResult(refreshed.ok && refreshed.data ? refreshed.data : { content: [], totalPages: 0, first: true, last: true });
  };

  return (
    <AdminLayout
      title="스트리머 관리"
      description="스트리머 검색, 등급 필터, 정산 기준 등급 변경을 운영 화면에서 관리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="아이디 또는 이름 검색"
          />
          <select className="admin-filter-select" value={tierFilter} onChange={(event) => { setTierFilter(event.target.value); setPage(0); }}>
            <option value="">전체 등급</option>
            <option value="GENERAL">일반 스트리머</option>
            <option value="PARTNER">파트너 스트리머</option>
          </select>
          <select className="admin-filter-select" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
            <option value="pang-desc">받은 팡 많은순</option>
            <option value="created-desc">최근 가입순</option>
            <option value="created-asc">오래된 가입순</option>
          </select>
          <button type="button" className="admin-action-btn primary" onClick={() => { setQuery(queryInput.trim()); setPage(0); }}>
            검색
          </button>
          <button
            type="button"
            className="admin-action-btn"
            onClick={() => {
              setQueryInput('');
              setQuery('');
              setTierFilter('');
              setSortKey('pang-desc');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-streamers-table">
          <colgroup>
            <col style={{ width: '22%' }} />
            <col style={{ width: '16%' }} />
            <col style={{ width: '22%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '14%' }} />
          </colgroup>
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
                <td colSpan={6} className="empty-msg">불러오는 중...</td>
              </tr>
            ) : visibleRows.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty-msg">조건에 맞는 스트리머가 없습니다.</td>
              </tr>
            ) : (
              visibleRows.map((streamer) => {
                const tierBadge = streamerTierMeta(streamer.streamerTier);
                return (
                  <tr key={streamer.id}>
                    <td>{shortenLoginId(streamer.loginId)}</td>
                    <td>{streamer.displayName}</td>
                    <td><span className={`admin-status-badge ${tierBadge.tone}`}>{tierBadge.label}</span></td>
                    <td>{formatDate(streamer.createdAt)}</td>
                    <td>{Number(streamer.totalReceivedPang ?? 0).toLocaleString()}</td>
                    <td>
                      <div className="admin-actions-inline">
                        <button
                          type="button"
                          className={`btn-tier ${streamer.streamerTier === 'GENERAL' ? 'active' : ''}`}
                          disabled={submittingId === streamer.id}
                          onClick={() => handleTierChange(streamer.id, 'GENERAL')}
                        >
                          일반
                        </button>
                        <button
                          type="button"
                          className={`btn-tier ${streamer.streamerTier === 'PARTNER' ? 'active' : ''}`}
                          disabled={submittingId === streamer.id}
                          onClick={() => handleTierChange(streamer.id, 'PARTNER')}
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
          <button type="button" className="admin-action-btn" disabled={result?.first ?? true} onClick={() => setPage((current) => Math.max(0, current - 1))}>
            이전
          </button>
          <span>페이지 {page + 1}{result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}</span>
          <button type="button" className="admin-action-btn" disabled={result?.last ?? true} onClick={() => setPage((current) => current + 1)}>
            다음
          </button>
        </div>
      </section>
    </AdminLayout>
  );
}
