import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { banReportedUser, fetchAdminReports, unbanReportedUser, updateAdminReportStatus } from '../api/admin';
import type { AdminReportRow } from '../api/admin';

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

function reportStatusMeta(status: AdminReportRow['status']) {
  switch (status) {
    case 'PENDING':
      return { label: '접수 대기', tone: 'tone-warning' };
    case 'IN_REVIEW':
      return { label: '검토 중', tone: 'tone-accent' };
    case 'RESOLVED':
      return { label: '처리 완료', tone: 'tone-success' };
    case 'DISMISSED':
      return { label: '기각', tone: 'tone-muted' };
    default:
      return { label: status, tone: 'tone-neutral' };
  }
}

function userStatusMeta(status: AdminReportRow['reportedUserStatus']) {
  switch (status) {
    case 'ACTIVE':
      return { label: '활성', tone: 'tone-success' };
    case 'INACTIVE':
      return { label: '비활성', tone: 'tone-muted' };
    case 'SUSPENDED':
      return { label: '정지', tone: 'tone-danger' };
    case 'DELETED':
      return { label: '삭제', tone: 'tone-neutral' };
    default:
      return { label: status, tone: 'tone-neutral' };
  }
}

export default function AdminReports() {
  const { user, loading: authLoading } = useAuth();
  const [statusFilter, setStatusFilter] = useState('');
  const [queryInput, setQueryInput] = useState('');
  const [query, setQuery] = useState('');
  const [sortKey, setSortKey] = useState('latest');
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState<AdminReportRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState<number | null>(null);
  const [noteInputs, setNoteInputs] = useState<Record<number, string>>({});

  const load = async () => {
    setLoading(true);
    const response = await fetchAdminReports({ status: statusFilter, page, size: 20 });
    const nextRows = response.ok && response.data ? response.data : [];
    setRows(nextRows);
    setNoteInputs(
      nextRows.reduce<Record<number, string>>((acc, row) => {
        acc[row.id] = row.adminNote ?? '';
        return acc;
      }, {})
    );
    setLoading(false);
  };

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    void load();
  }, [page, statusFilter, user]);

  const filteredRows = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    const searched = !normalized
      ? rows
      : rows.filter((row) =>
          [row.reporterUsername, row.reportedUsername, row.reason, row.description, row.adminNote ?? '']
            .join(' ')
            .toLowerCase()
            .includes(normalized)
        );

    const sorted = [...searched];
    sorted.sort((a, b) => {
      const createdA = new Date(a.createdAt).getTime();
      const createdB = new Date(b.createdAt).getTime();
      const resolvedA = a.resolvedAt ? new Date(a.resolvedAt).getTime() : 0;
      const resolvedB = b.resolvedAt ? new Date(b.resolvedAt).getTime() : 0;
      switch (sortKey) {
        case 'oldest':
          return createdA - createdB;
        case 'resolved':
          return resolvedB - resolvedA || createdB - createdA;
        case 'latest':
        default:
          return createdB - createdA;
      }
    });
    return sorted;
  }, [query, rows, sortKey]);

  if (authLoading) {
    return (
      <AdminLayout
        title="신고 관리"
        description="관리자 권한과 신고 데이터를 확인하는 중입니다."
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

  const handleReportStatus = async (reportId: number, status: string) => {
    setSubmittingId(reportId);
    const response = await updateAdminReportStatus(reportId, status, noteInputs[reportId] ?? '');
    setSubmittingId(null);
    if (!response.ok) {
      window.alert(response.message ?? '신고 상태 변경에 실패했습니다.');
      return;
    }
    await load();
  };

  const handleBan = async (reportId: number, ban: boolean) => {
    setSubmittingId(reportId);
    const response = ban
      ? await banReportedUser(reportId, noteInputs[reportId] ?? '')
      : await unbanReportedUser(reportId, noteInputs[reportId] ?? '');
    setSubmittingId(null);
    if (!response.ok) {
      window.alert(response.message ?? '회원 제재 처리에 실패했습니다.');
      return;
    }
    await load();
  };

  return (
    <AdminLayout
      title="신고 관리"
      description="신고 접수 현황을 검토하고 상태 변경, 계정 제재와 해제를 같은 화면에서 처리합니다."
    >
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <input
            className="admin-search-input"
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="신고자, 대상자, 사유 검색"
          />
          <select className="admin-filter-select" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}>
            <option value="">전체 상태</option>
            <option value="PENDING">접수 대기</option>
            <option value="IN_REVIEW">검토 중</option>
            <option value="RESOLVED">처리 완료</option>
            <option value="DISMISSED">기각</option>
          </select>
          <select className="admin-filter-select" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
            <option value="latest">최신 등록순</option>
            <option value="oldest">오래된 등록순</option>
            <option value="resolved">최근 처리순</option>
          </select>
          <button type="button" className="admin-action-btn primary" onClick={() => setQuery(queryInput.trim())}>
            검색
          </button>
          <button
            type="button"
            className="admin-action-btn"
            onClick={() => {
              setQueryInput('');
              setQuery('');
              setPage(0);
              setSortKey('latest');
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
              <th>ID</th>
              <th>신고자</th>
              <th>대상자</th>
              <th>사유</th>
              <th>등록일</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="empty-msg">불러오는 중...</td></tr>
            ) : filteredRows.length === 0 ? (
              <tr><td colSpan={7} className="empty-msg">조건에 맞는 신고가 없습니다.</td></tr>
            ) : (
              filteredRows.map((report) => {
                const reportMeta = reportStatusMeta(report.status);
                const userMeta = userStatusMeta(report.reportedUserStatus);
                return (
                  <tr key={report.id}>
                    <td>{report.id}</td>
                    <td>{report.reporterUsername}</td>
                    <td>
                      <div>{report.reportedUsername}</div>
                      <div className="admin-top-gap">
                        <span className={`admin-status-badge ${userMeta.tone}`}>{userMeta.label}</span>
                      </div>
                    </td>
                    <td>
                      <div>{report.reason}</div>
                      <div className="admin-subtext">{report.description || '설명 없음'}</div>
                    </td>
                    <td>
                      <div>{formatDate(report.createdAt)}</div>
                      <div className="admin-subtext">처리일 {formatDate(report.resolvedAt)}</div>
                    </td>
                    <td>
                      <span className={`admin-status-badge ${reportMeta.tone}`}>{reportMeta.label}</span>
                    </td>
                    <td>
                      <div className="admin-actions-inline admin-actions-wrap">
                        <textarea
                          className="admin-note-input"
                          rows={3}
                          value={noteInputs[report.id] ?? ''}
                          onChange={(event) => setNoteInputs((current) => ({ ...current, [report.id]: event.target.value }))}
                          placeholder="관리자 메모 입력"
                        />
                        <button type="button" className="admin-action-btn" disabled={submittingId === report.id} onClick={() => handleReportStatus(report.id, 'IN_REVIEW')}>
                          검토
                        </button>
                        <button type="button" className="admin-action-btn" disabled={submittingId === report.id} onClick={() => handleReportStatus(report.id, 'RESOLVED')}>
                          완료
                        </button>
                        <button type="button" className="admin-action-btn" disabled={submittingId === report.id} onClick={() => handleReportStatus(report.id, 'DISMISSED')}>
                          기각
                        </button>
                        <button type="button" className="admin-action-btn" disabled={submittingId === report.id} onClick={() => handleBan(report.id, report.reportedUserStatus !== 'SUSPENDED')}>
                          {report.reportedUserStatus === 'SUSPENDED' ? '정지 해제' : '계정 정지'}
                        </button>
                      </div>
                      {report.adminNote ? <div className="admin-subtext">저장된 메모: {report.adminNote}</div> : null}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        <div className="admin-pagination">
          <button type="button" className="admin-action-btn" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>
            이전
          </button>
          <span>페이지 {page + 1}</span>
          <button type="button" className="admin-action-btn" disabled={rows.length < 20} onClick={() => setPage((current) => current + 1)}>
            다음
          </button>
        </div>
      </section>
    </AdminLayout>
  );
}
