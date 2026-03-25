import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { fetchAdminRevenue } from '../api/admin';
import type { AdminRevenueSummary } from '../api/admin';

function formatWon(value?: number) {
  return `${Number(value ?? 0).toLocaleString()}원`;
}

function formatPang(value?: number) {
  return `${Number(value ?? 0).toLocaleString()}팡`;
}

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function formatHourLabel(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
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

function orderStatusTone(status: string) {
  switch (status) {
    case 'COMPLETED':
      return { label: '완료', tone: 'tone-success' };
    case 'PENDING':
      return { label: '대기', tone: 'tone-warning' };
    case 'FAILED':
      return { label: '실패', tone: 'tone-danger' };
    case 'CANCELLED':
      return { label: '취소', tone: 'tone-muted' };
    default:
      return { label: status, tone: 'tone-neutral' };
  }
}

function buildTrendPath(values: number[], width: number, height: number) {
  if (values.length === 0) return '';
  const max = Math.max(...values, 1);
  return values
    .map((value, index) => {
      const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
      const y = height - (value / max) * (height - 16) - 8;
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
    })
    .join(' ');
}

function buildTrendPoints(values: number[], width: number, height: number) {
  if (values.length === 0) return [];
  const max = Math.max(...values, 1);
  return values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
    const y = height - (value / max) * (height - 16) - 8;
    return { x, y, value, index };
  });
}

export default function AdminRevenue() {
  const { user, loading: authLoading } = useAuth();
  const [summary, setSummary] = useState<AdminRevenueSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [hoveredTrendIndex, setHoveredTrendIndex] = useState<number | null>(null);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    setLoading(true);
    fetchAdminRevenue()
      .then((response) => setSummary(response.ok ? response.data ?? null : null))
      .finally(() => setLoading(false));
  }, [user]);

  const statusBars = useMemo(() => {
    if (!summary) return [];
    const rows = [
      { key: 'completed', label: '완료 매출', value: summary.completedSalesWon, tone: 'tone-success' },
      { key: 'pending', label: '정산 대기', value: summary.pendingSalesWon, tone: 'tone-warning' },
      { key: 'cancelled', label: '취소 금액', value: summary.cancelledSalesWon, tone: 'tone-muted' },
      { key: 'failed', label: '실패 금액', value: summary.failedSalesWon, tone: 'tone-danger' },
    ];
    const max = Math.max(...rows.map((row) => row.value), 1);
    return rows.map((row) => ({
      ...row,
      width: `${Math.max((row.value / max) * 100, row.value > 0 ? 8 : 0)}%`,
    }));
  }, [summary]);

  const metricBars = useMemo(() => {
    if (!summary) return [];
    const rows = [
      { label: '플랫폼 매출', value: summary.platformRevenueWon, format: formatWon },
      { label: '이번 달 완료', value: summary.monthCompletedWon, format: formatWon },
      { label: '오늘 완료', value: summary.todayCompletedWon, format: formatWon },
      { label: '구독권 매출', value: summary.subscriptionSalesWon, format: formatWon },
      { label: '광고제거 매출', value: summary.adFreeSalesWon, format: formatWon },
    ].sort((a, b) => b.value - a.value);

    const max = Math.max(...rows.map((row) => row.value), 1);
    return rows.map((row) => ({
      ...row,
      ratio: `${Math.max((row.value / max) * 100, row.value > 0 ? 10 : 0)}%`,
    }));
  }, [summary]);

  const recentTrend = useMemo(() => {
    const grouped = new Map<string, { label: string; value: number }>();

    (summary?.recentOrders ?? [])
      .slice()
      .reverse()
      .forEach((order) => {
        if (!order.createdAt) return;
        const date = new Date(order.createdAt);
        if (Number.isNaN(date.getTime())) return;
        const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}-${date.getHours()}`;
        const current = grouped.get(key);
        if (current) {
          current.value += Number(order.amountWon ?? 0);
          return;
        }
        grouped.set(key, {
          label: formatHourLabel(order.createdAt),
          value: Number(order.amountWon ?? 0),
        });
      });

    const entries = Array.from(grouped.values());
    const values = entries.map((entry) => entry.value);
    return {
      entries,
      values,
      path: buildTrendPath(values, 560, 180),
      points: buildTrendPoints(values, 560, 180),
    };
  }, [summary]);

  if (authLoading) {
    return (
      <AdminLayout title="매출 관리" description="관리자 권한과 매출 데이터를 확인하는 중입니다.">
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
      title="매출 관리"
      description="회사 매출 흐름과 플랫폼 수익 지표를 운영자가 빠르게 확인할 수 있는 화면입니다."
    >
      <section className="admin-metric-grid admin-metric-grid-wide">
        <article className="admin-metric-card">
          <span>누적 요청 금액</span>
          <strong>{formatWon(summary?.totalRequestedWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>완료 매출</span>
          <strong>{formatWon(summary?.completedSalesWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>오늘 완료 매출</span>
          <strong>{formatWon(summary?.todayCompletedWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>이번 달 완료 매출</span>
          <strong>{formatWon(summary?.monthCompletedWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>구독권 매출</span>
          <strong>{formatWon(summary?.subscriptionSalesWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>광고제거 매출</span>
          <strong>{formatWon(summary?.adFreeSalesWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>플랫폼 매출</span>
          <strong>{formatWon(summary?.platformRevenueWon)}</strong>
        </article>
        <article className="admin-metric-card">
          <span>스트리머 정산 추정</span>
          <strong>{formatPang(summary?.completedSettlementPang)}</strong>
        </article>
      </section>

      <section className="admin-overview-grid admin-overview-grid-dual">
        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>상태별 매출 그래프</h3>
              <p>주문 상태별 금액을 막대 그래프로 비교합니다.</p>
            </div>
          </div>
          {loading ? (
            <p className="admin-subtext">불러오는 중입니다.</p>
          ) : (
            <div className="admin-chart-list">
              {statusBars.map((row) => (
                <div key={row.key} className="admin-chart-row">
                  <div className="admin-chart-label">
                    <strong>{row.label}</strong>
                    <span>{formatWon(row.value)}</span>
                  </div>
                  <div className="admin-bar-track">
                    <div className={`admin-bar-fill ${row.tone}`} style={{ width: row.width }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </article>

        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>핵심 지표 비교</h3>
              <p>플랫폼 매출과 운영용 세부 매출 항목을 함께 비교합니다.</p>
            </div>
          </div>
          {loading ? (
            <p className="admin-subtext">불러오는 중입니다.</p>
          ) : (
            <div className="admin-chart-list">
              {metricBars.map((row) => (
                <div key={row.label} className="admin-chart-row">
                  <div className="admin-chart-label">
                    <strong>{row.label}</strong>
                    <span>{row.format(row.value)}</span>
                  </div>
                  <div className="admin-bar-track subtle">
                    <div className="admin-bar-fill tone-accent" style={{ width: row.ratio }} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </article>
      </section>

      <section className="admin-overview-grid admin-overview-grid-dual">
        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>최근 주문 추이</h3>
              <p>최근 주문 금액을 시간 단위로 묶어 선형 그래프로 확인합니다.</p>
            </div>
          </div>
          {loading ? (
            <p className="admin-subtext">불러오는 중입니다.</p>
          ) : !summary || recentTrend.values.length === 0 ? (
            <p className="admin-subtext">그래프로 표시할 최근 주문이 없습니다.</p>
          ) : (
            <div className="admin-line-chart">
              <svg viewBox="0 0 560 180" preserveAspectRatio="none" aria-hidden>
                <path d="M 0 170 L 560 170" className="admin-line-grid" />
                <path d="M 0 120 L 560 120" className="admin-line-grid" />
                <path d="M 0 70 L 560 70" className="admin-line-grid" />
                <path d={recentTrend.path} className="admin-line-path" />
                {recentTrend.points.map((point, index) => (
                  <circle
                    key={`${recentTrend.entries[index]?.label ?? index}-${point.value}`}
                    cx={point.x}
                    cy={point.y}
                    r={hoveredTrendIndex === index ? 6 : 4}
                    className="admin-line-point"
                    onMouseEnter={() => setHoveredTrendIndex(index)}
                    onMouseLeave={() => setHoveredTrendIndex((current) => (current === index ? null : current))}
                  >
                    <title>{`${recentTrend.entries[index]?.label ?? '-'} 매출 ${formatWon(point.value)}`}</title>
                  </circle>
                ))}
              </svg>
              {hoveredTrendIndex !== null && recentTrend.entries[hoveredTrendIndex] ? (
                <div className="admin-line-tooltip">
                  <strong>{recentTrend.entries[hoveredTrendIndex].label}</strong>
                  <span>{formatWon(recentTrend.entries[hoveredTrendIndex].value)}</span>
                </div>
              ) : (
                <p className="admin-subtext">그래프 점 위에 마우스를 올리면 시간대별 매출이 표시됩니다.</p>
              )}
            </div>
          )}
        </article>

        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>운영 메모</h3>
              <p>플랫폼 매출은 후원 수수료 환산액과 상품 매출을 합산한 운영 기준치입니다.</p>
            </div>
          </div>
          <p className="admin-subtext">
            일반 스트리머는 30%, 파트너 스트리머는 20% 기준으로 후원 수수료를 계산했고, 여기에 구독권/광고제거 매출을 더했습니다.
          </p>
          <p className="admin-subtext">
            실제 회계 확정 금액과 다를 수 있으므로 운영 참고 지표로 보면 됩니다.
          </p>
          <div className="admin-detail-list">
            <div><strong>플랫폼 매출</strong><span>{formatWon(summary?.platformRevenueWon)}</span></div>
            <div><strong>구독권 매출</strong><span>{formatWon(summary?.subscriptionSalesWon)}</span></div>
            <div><strong>광고제거 매출</strong><span>{formatWon(summary?.adFreeSalesWon)}</span></div>
            <div><strong>완료 팡 수량</strong><span>{formatPang(summary?.completedPang)}</span></div>
            <div><strong>누적 후원 팡</strong><span>{formatPang(summary?.totalDonationPang)}</span></div>
            <div><strong>정산 대기</strong><span>{formatWon(summary?.pendingSalesWon)}</span></div>
          </div>
        </article>
      </section>

      <section className="admin-panel admin-table-card">
        <div className="admin-section-head">
          <div>
            <h3>최근 주문</h3>
            <p>최근 결제 주문 흐름을 상태와 함께 빠르게 확인합니다.</p>
          </div>
        </div>
        <table className="data-table admin-revenue-orders-table">
          <colgroup>
            <col style={{ width: '24%' }} />
            <col style={{ width: '16%' }} />
            <col style={{ width: '16%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '12%' }} />
          </colgroup>
          <thead>
            <tr>
              <th>주문번호</th>
              <th>회원</th>
              <th>종류</th>
              <th>금액</th>
              <th>팡</th>
              <th>상태</th>
              <th>등록일</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="empty-msg">불러오는 중입니다.</td></tr>
            ) : !summary || summary.recentOrders.length === 0 ? (
              <tr><td colSpan={7} className="empty-msg">표시할 최근 주문이 없습니다.</td></tr>
            ) : (
              summary.recentOrders.map((order) => {
                const badge = orderStatusTone(order.status);
                return (
                  <tr key={order.id}>
                    <td>{order.orderId}</td>
                    <td>
                      <div>{order.displayName || '-'}</div>
                      <div className="admin-subtext">{shortenLoginId(order.loginId || '-')}</div>
                    </td>
                    <td>{order.kind}</td>
                    <td>{formatWon(order.amountWon)}</td>
                    <td>{formatPang(order.pangAmount)}</td>
                    <td><span className={`admin-status-badge ${badge.tone}`}>{badge.label}</span></td>
                    <td>{formatDate(order.createdAt)}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </section>
    </AdminLayout>
  );
}
