import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { fetchAdminSettlements } from '../api/admin';
import type { AdminSettlementRow } from '../api/admin';

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

function formatNumber(value?: number | null, suffix = '') {
  return `${Number(value ?? 0).toLocaleString()}${suffix}`;
}

export default function AdminSettlements() {
  const { user, loading: authLoading } = useAuth();
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [sortKey, setSortKey] = useState('created-desc');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<{
    content: AdminSettlementRow[];
    totalPages: number;
    first: boolean;
    last: boolean;
  } | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminSettlements({ query, page, size: 12 });
    setResult(response.ok && response.data ? response.data : { content: [], totalPages: 0, first: true, last: true });
    setLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [page, query, user]);

  const rows = result?.content ?? [];

  const visibleRows = useMemo(() => {
    const nextRows = [...rows];
    nextRows.sort((a, b) => {
      const createdA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const createdB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      switch (sortKey) {
        case 'created-asc':
          return createdA - createdB;
        case 'amount-desc':
          return (b.amountWon ?? 0) - (a.amountWon ?? 0);
        case 'amount-asc':
          return (a.amountWon ?? 0) - (b.amountWon ?? 0);
        case 'created-desc':
        default:
          return createdB - createdA;
      }
    });
    return nextRows;
  }, [rows, sortKey]);

  if (authLoading) {
    return (
      <AdminLayout title="정산 관리" description="관리자 권한과 정산 신청 정보를 확인하는 중입니다.">
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
      title="정산 관리"
      description="결제 주문은 제외하고, 스트리머 환전/정산 신청 건만 운영자가 확인하는 화면입니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="정산번호, 아이디, 이름 검색"
          />
          <select className="admin-filter-select" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
            <option value="created-desc">최근 신청순</option>
            <option value="created-asc">오래된 신청순</option>
            <option value="amount-desc">정산금액 큰 순</option>
            <option value="amount-asc">정산금액 작은 순</option>
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
              setSortKey('created-desc');
              setPage(0);
            }}
          >
            초기화
          </button>
        </div>
      </section>

      <section className="admin-panel admin-table-card">
        <table className="data-table admin-settlements-table">
          <colgroup>
            <col style={{ width: '9%' }} />
            <col style={{ width: '15%' }} />
            <col style={{ width: '13%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '14%' }} />
            <col style={{ width: '15%' }} />
            <col style={{ width: '22%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>정산번호</th>
              <th>회원</th>
              <th>신청 팡</th>
              <th>수수료</th>
              <th>실정산 팡</th>
              <th>정산금액</th>
              <th>신청일</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="empty-msg">불러오는 중입니다.</td>
              </tr>
            ) : visibleRows.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-msg">조건에 맞는 정산 신청이 없습니다.</td>
              </tr>
            ) : (
              visibleRows.map((settlement) => (
                <tr key={settlement.id}>
                  <td>{settlement.orderId}</td>
                  <td>
                    <div>{settlement.displayName || '-'}</div>
                    <div className="admin-subtext">{settlement.loginId || '-'}</div>
                  </td>
                  <td>{formatNumber(settlement.pangAmount, ' 팡')}</td>
                  <td>
                    <div>{formatNumber(settlement.commissionPang, ' 팡')}</div>
                    <div className="admin-subtext">수수료율 {settlement.commissionPercent ?? 0}%</div>
                  </td>
                  <td>{formatNumber(settlement.netPang ?? settlement.amountWon, ' 팡')}</td>
                  <td>{formatNumber(settlement.amountWon, '원')}</td>
                  <td>{formatDate(settlement.createdAt)}</td>
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
          <span>페이지 {page + 1}{result?.totalPages ? ` / ${Math.max(result.totalPages, 1)}` : ''}</span>
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
