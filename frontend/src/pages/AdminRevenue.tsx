import { useEffect, useMemo, useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useAuth } from '../contexts/AuthContext';
import { fetchAdminRevenue } from '../api/admin';
import type { AdminRevenueSummary } from '../api/admin';

type RevenueCardKey =
  | 'total-payment'
  | 'total-donation-pang'
  | 'streamer-settlement'
  | 'settlement-fee'
  | 'subscription-sales'
  | 'adfree-sales'
  | 'mileage-sales'
  | 'platform-revenue';

type DetailRow = {
  label: string;
  value: number;
  format: (value?: number) => string;
};

function formatWon(value?: number) {
  return `${Math.round(Number(value ?? 0)).toLocaleString()}원`;
}

function formatPang(value?: number) {
  return `${Math.round(Number(value ?? 0)).toLocaleString()}팡`;
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
      return { label: '결제완료', tone: 'tone-success' };
    case 'FAILED':
      return { label: '실패', tone: 'tone-danger' };
    case 'CANCELLED':
      return { label: '환불', tone: 'tone-muted' };
    default:
      return { label: status, tone: 'tone-neutral' };
  }
}

function buildTrendPath(values: number[], width: number, height: number) {
  if (values.length === 0) return '';
  const max = Math.max(...values, 1);
  const paddingX = 16;
  const drawableWidth = Math.max(width - paddingX * 2, 1);
  return values
    .map((value, index) => {
      const x = values.length === 1 ? width / 2 : paddingX + (index / (values.length - 1)) * drawableWidth;
      const y = height - (value / max) * (height - 16) - 8;
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
    })
    .join(' ');
}

function buildTrendPoints(values: number[], width: number, height: number) {
  if (values.length === 0) return [];
  const max = Math.max(...values, 1);
  const paddingX = 16;
  const drawableWidth = Math.max(width - paddingX * 2, 1);
  return values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : paddingX + (index / (values.length - 1)) * drawableWidth;
    const y = height - (value / max) * (height - 16) - 8;
    return { x, y, value, index };
  });
}

export default function AdminRevenue() {
  const { user, loading: authLoading } = useAuth();
  const [summary, setSummary] = useState<AdminRevenueSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [hoveredTrendIndex, setHoveredTrendIndex] = useState<number | null>(null);
  const [selectedCard, setSelectedCard] = useState<RevenueCardKey>('total-payment');
  const now = useMemo(() => new Date(), []);
  const [filterYear, setFilterYear] = useState(now.getFullYear());
  const [filterMonth, setFilterMonth] = useState(now.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const currentYear = Number(summary?.currentYear ?? now.getFullYear());
  const currentMonth = Number(summary?.currentMonth ?? now.getMonth() + 1);
  const isCurrentSelection = selectedYear === currentYear && selectedMonth === currentMonth;
  const selectedMonthLabel = `${selectedYear}년 ${selectedMonth}월`;
  const isCashFlowCard = selectedCard === 'total-payment';
  const isPangFlowCard = selectedCard === 'total-donation-pang' || selectedCard === 'streamer-settlement';
  const isMileageFlowCard = selectedCard === 'mileage-sales';

  const totalRequestedWon = Number(summary?.totalRequestedWon ?? 0);
  const completedSalesWon = Number(summary?.completedSalesWon ?? 0);
  const cancelledSalesWon = Number(summary?.cancelledSalesWon ?? 0);
  const failedSalesWon = Number(summary?.failedSalesWon ?? 0);
  const todayCompletedWon = Number(summary?.todayCompletedWon ?? 0);
  const todayPlatformRevenueWon = Number(summary?.todayPlatformRevenueWon ?? summary?.todayCompletedWon ?? 0);
  const monthPlatformRevenueWon = Number(summary?.monthPlatformRevenueWon ?? summary?.monthCompletedWon ?? 0);
  const mileageSalesWon = Number(summary?.mileageSalesWon ?? 0);
  const subscriptionSalesWon = Number(summary?.subscriptionSalesWon ?? 0);
  const adFreeSalesWon = Number(summary?.adFreeSalesWon ?? 0);
  const platformRevenueWon = Number(summary?.platformRevenueWon ?? 0);
  const totalDonationPang = Number(summary?.totalDonationPang ?? 0);
  const monthUsedPang = Number(summary?.monthUsedPang ?? summary?.usedPang ?? totalDonationPang);
  const monthSettledPang = Number(summary?.monthCompletedSettlementPang ?? summary?.completedSettlementPang ?? 0);
  const monthSettlementCommissionPang = Number(
    summary?.monthCompletedSettlementCommissionPang ?? summary?.completedSettlementCommissionPang ?? 0,
  );
  const usedPang = Number(summary?.usedPang ?? totalDonationPang);
  const settledPang = Number(summary?.completedSettlementPang ?? 0);
  const remainingPang = Number(summary?.remainingPang ?? 0);
  const grantedMileage = Number(summary?.grantedMileage ?? 0);
  const usedMileage = Number(summary?.usedMileage ?? mileageSalesWon);
  const remainingMileage = Number(summary?.remainingMileage ?? Math.max(grantedMileage - usedMileage, 0));
  const monthSettlementCommissionWon = Math.round(monthSettlementCommissionPang * 1.2);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    setLoading(true);
    fetchAdminRevenue({ year: selectedYear, month: selectedMonth })
      .then((response) => setSummary(response.ok ? response.data ?? null : null))
      .finally(() => setLoading(false));
  }, [selectedMonth, selectedYear, user]);

  const handleRevenueSearch = () => {
    setSelectedYear(filterYear);
    setSelectedMonth(filterMonth);
  };

  const yearOptions = useMemo(() => {
    const currentYear = summary?.currentYear ?? now.getFullYear();
    return Array.from({ length: 5 }, (_, index) => currentYear - index);
  }, [now, summary?.currentYear]);

  const statusBars = useMemo(() => {
    const rows = [
      { key: 'cash-month', label: `${selectedMonthLabel} 충전`, value: completedSalesWon, tone: 'tone-success' },
      ...(isCurrentSelection
        ? [{ key: 'cash-today', label: '오늘 충전', value: todayCompletedWon, tone: 'tone-neutral' as const }]
        : []),
      { key: 'cash-refund', label: `${selectedMonthLabel} 환불`, value: cancelledSalesWon, tone: 'tone-muted' },
      { key: 'cash-failed', label: `${selectedMonthLabel} 실패`, value: failedSalesWon, tone: 'tone-danger' },
    ];
    const max = Math.max(...rows.map((row) => row.value), 1);
    return rows.map((row) => ({
      ...row,
      width: `${Math.max((row.value / max) * 100, row.value > 0 ? 8 : 0)}%`,
    }));
  }, [cancelledSalesWon, completedSalesWon, failedSalesWon, isCurrentSelection, selectedMonthLabel, todayCompletedWon]);

  const metricBars = useMemo(() => {
    const rows = [
      { label: '이번 달 플랫폼 매출', value: monthPlatformRevenueWon, format: formatWon },
      ...(isCurrentSelection ? [{ label: '금일 플랫폼 매출', value: todayPlatformRevenueWon, format: formatWon }] : []),
      { label: '마일리지 결제', value: mileageSalesWon, format: formatWon },
      { label: '구독권 매출', value: subscriptionSalesWon, format: formatWon },
      { label: '광고제거 매출', value: adFreeSalesWon, format: formatWon },
    ].sort((a, b) => b.value - a.value);

    const max = Math.max(...rows.map((row) => row.value), 1);
    return rows.map((row) => ({
      ...row,
      ratio: `${Math.max((row.value / max) * 100, row.value > 0 ? 10 : 0)}%`,
    }));
  }, [adFreeSalesWon, isCurrentSelection, mileageSalesWon, monthPlatformRevenueWon, subscriptionSalesWon, todayPlatformRevenueWon]);

  const detailMetricBars = useMemo(() => {
    const rows: DetailRow[] =
      isPangFlowCard
        ? [
            { label: `${selectedMonthLabel} 사용 팡`, value: monthUsedPang, format: formatPang },
            { label: `${selectedMonthLabel} 정산 팡`, value: monthSettledPang, format: formatPang },
            { label: `${selectedMonthLabel} 정산 수수료`, value: monthSettlementCommissionPang, format: formatPang },
            { label: '현재 미사용 팡', value: remainingPang, format: formatPang },
          ]
        : isMileageFlowCard
          ? [
                { label: '누적 지급 마일리지', value: grantedMileage, format: formatWon },
                { label: `${selectedMonthLabel} 사용 마일리지`, value: mileageSalesWon, format: formatWon },
                { label: '현재 미사용 마일리지', value: remainingMileage, format: formatWon },
              ]
            : isCashFlowCard
              ? [
                  { label: `${selectedMonthLabel} 충전`, value: completedSalesWon, format: formatWon },
                  ...(isCurrentSelection ? [{ label: '오늘 충전', value: todayCompletedWon, format: formatWon }] : []),
                  { label: `${selectedMonthLabel} 환불`, value: cancelledSalesWon, format: formatWon },
                  { label: `${selectedMonthLabel} 실패`, value: failedSalesWon, format: formatWon },
                ]
              : [
                { label: `${selectedMonthLabel} 정산 수수료`, value: monthSettlementCommissionWon, format: formatWon },
                { label: '구독권 매출', value: subscriptionSalesWon, format: formatWon },
                { label: '광고제거 매출', value: adFreeSalesWon, format: formatWon },
                { label: '마일리지 결제', value: mileageSalesWon, format: formatWon },
                { label: `${selectedMonthLabel} 플랫폼 매출`, value: monthPlatformRevenueWon, format: formatWon },
              ];

    const max = Math.max(...rows.map((row) => row.value), 1);
    return rows.map((row) => ({
      ...row,
      ratio: `${Math.max((row.value / max) * 100, row.value > 0 ? 10 : 0)}%`,
    }));
  }, [
    adFreeSalesWon,
    cancelledSalesWon,
    completedSalesWon,
    failedSalesWon,
    isCashFlowCard,
    isCurrentSelection,
    isMileageFlowCard,
    isPangFlowCard,
    grantedMileage,
    mileageSalesWon,
    monthPlatformRevenueWon,
    remainingMileage,
    remainingPang,
    monthSettledPang,
    monthSettlementCommissionPang,
    monthSettlementCommissionWon,
    monthUsedPang,
    selectedCard,
    selectedMonthLabel,
    subscriptionSalesWon,
    todayCompletedWon,
  ]);

  const focusPanelTitle = useMemo(() => {
    if (isCashFlowCard) return '현금 흐름';
    if (isPangFlowCard) return '팡 흐름';
    if (isMileageFlowCard) return '마일리지 흐름';
    return '플랫폼 수익 분석';
  }, [isCashFlowCard, isMileageFlowCard, isPangFlowCard]);

  const focusPanelDescription = useMemo(() => {
    if (isCashFlowCard) return `${selectedMonthLabel} 현금 결제 흐름입니다.`;
    if (isPangFlowCard) return `${selectedMonthLabel} 사용 내역과 현재 미사용 팡을 함께 보는 흐름입니다.`;
    if (isMileageFlowCard) return `${selectedMonthLabel} 사용 내역과 현재 미사용 마일리지를 함께 보는 흐름입니다.`;
    return `${selectedMonthLabel} 플랫폼 수익 구조입니다.`;
  }, [isCashFlowCard, isMileageFlowCard, isPangFlowCard, selectedMonthLabel]);

  const topCards = [
    { key: 'total-payment' as RevenueCardKey, label: '누적 결제', value: formatWon(completedSalesWon) },
    { key: 'total-donation-pang' as RevenueCardKey, label: '사용된 팡', value: formatPang(monthUsedPang) },
    { key: 'streamer-settlement' as RevenueCardKey, label: '정산된 팡', value: formatPang(monthSettledPang) },
    { key: 'settlement-fee' as RevenueCardKey, label: '정산 수수료', value: formatWon(Math.round(monthSettlementCommissionPang * 1.2)) },
    { key: 'subscription-sales' as RevenueCardKey, label: '구독권 매출', value: formatWon(subscriptionSalesWon) },
    { key: 'adfree-sales' as RevenueCardKey, label: '광고제거 매출', value: formatWon(adFreeSalesWon) },
    { key: 'mileage-sales' as RevenueCardKey, label: '마일리지 결제', value: formatWon(mileageSalesWon) },
    { key: 'platform-revenue' as RevenueCardKey, label: '플랫폼 매출', value: formatWon(platformRevenueWon) },
  ];

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
    <AdminLayout title="매출 관리" description="회사 매출 흐름과 플랫폼 수익 지표를 운영자가 빠르게 확인할 수 있는 화면입니다.">
      <section className="admin-panel admin-filter-panel">
        <div className="admin-toolbar">
          <select className="admin-filter-select" value={filterYear} onChange={(event) => setFilterYear(Number(event.target.value))}>
            {yearOptions.map((year) => (
              <option key={year} value={year}>
                {year}년
              </option>
            ))}
          </select>
          <select className="admin-filter-select" value={filterMonth} onChange={(event) => setFilterMonth(Number(event.target.value))}>
            {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
              <option key={month} value={month}>
                {month}월
              </option>
            ))}
          </select>
          <button type="button" className="admin-cta secondary admin-filter-submit" onClick={handleRevenueSearch}>
            조회
          </button>
        </div>
      </section>

      <section className="admin-metric-grid admin-metric-grid-wide">
        {topCards.map((card) => {
          const active = selectedCard === card.key;
          return (
            <button
              key={card.key}
              type="button"
              className="admin-metric-card"
              onClick={() => setSelectedCard(card.key)}
              style={{
                textAlign: 'left',
                cursor: 'pointer',
                outline: active ? '2px solid #2563eb' : 'none',
                boxShadow: active ? '0 0 0 3px rgba(37, 99, 235, 0.12)' : undefined,
              }}
            >
              <span>{card.label}</span>
              <strong>{card.value}</strong>
            </button>
          );
        })}
      </section>

      <section className="admin-overview-grid admin-overview-grid-dual">
        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>{focusPanelTitle}</h3>
              <p>{focusPanelDescription}</p>
            </div>
          </div>
          {loading ? (
            <p className="admin-subtext">불러오는 중입니다.</p>
          ) : (
            <div className="admin-chart-list">
              {selectedCard === 'total-payment'
                ? statusBars.map((row) => (
                    <div key={row.key} className="admin-chart-row">
                      <div className="admin-chart-label">
                        <strong>{row.label}</strong>
                        <span>{formatWon(row.value)}</span>
                      </div>
                      <div className="admin-bar-track">
                        <div className={`admin-bar-fill ${row.tone}`} style={{ width: row.width }} />
                      </div>
                    </div>
                  ))
                : detailMetricBars.map((row) => (
                    <div key={row.label} className="admin-chart-row">
                      <div className="admin-chart-label">
                        <strong>{row.label}</strong>
                        <span>{row.format(row.value)}</span>
                      </div>
                      <div className="admin-bar-track">
                        <div className="admin-bar-fill tone-accent" style={{ width: row.ratio }} />
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
              <p>
                {selectedYear}년 {selectedMonth}월 기준 분리된 지표만 비교합니다.
              </p>
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
                    key={`${point.x}-${point.y}`}
                    cx={point.x}
                    cy={point.y}
                    r={hoveredTrendIndex === index ? 5 : 4}
                    className="admin-line-point"
                    onMouseEnter={() => setHoveredTrendIndex(index)}
                    onMouseLeave={() => setHoveredTrendIndex(null)}
                  />
                ))}
              </svg>
              {hoveredTrendIndex !== null && recentTrend.entries[hoveredTrendIndex] ? (
                <div className="admin-line-tooltip">
                  <strong>{recentTrend.entries[hoveredTrendIndex].label}</strong>
                  <span>{formatWon(recentTrend.entries[hoveredTrendIndex].value)}</span>
                </div>
              ) : null}
            </div>
          )}
        </article>

        <article className="admin-panel">
          <div className="admin-section-head">
            <div>
              <h3>운영 메모</h3>
              <p>집계 기준과 핵심 수치를 한눈에 확인할 수 있도록 정리했습니다.</p>
            </div>
          </div>
          <div className="admin-note-copy">
            <p>구독권과 광고제거 매출은 현금 결제 기준으로 집계하며, 마일리지 사용분은 별도 항목으로 분리해 확인합니다.</p>
            <p>플랫폼 매출은 정산 수수료와 광고제거 매출을 기준으로 계산하고, 마일리지 결제분은 제외합니다.</p>
          </div>
          <div className="admin-summary-list">
            <div><strong>누적 결제</strong><span>{formatWon(totalRequestedWon)}</span></div>
            <div><strong>사용된 팡</strong><span>{formatPang(usedPang)}</span></div>
            <div><strong>정산된 팡</strong><span>{formatPang(settledPang)}</span></div>
            <div><strong>정산 수수료</strong><span>{formatWon(Math.round((summary?.completedSettlementCommissionPang ?? 0) * 1.2))}</span></div>
            <div><strong>구독권 매출</strong><span>{formatWon(subscriptionSalesWon)}</span></div>
            <div><strong>광고제거 매출</strong><span>{formatWon(adFreeSalesWon)}</span></div>
            <div><strong>마일리지 결제</strong><span>{formatWon(mileageSalesWon)}</span></div>
            <div><strong>플랫폼 매출</strong><span>{formatWon(platformRevenueWon)}</span></div>
          </div>
        </article>
      </section>

      <section className="admin-panel">
        <div className="admin-section-head">
          <div>
            <h3>최근 주문 내역</h3>
            <p>현금 결제와 마일리지 결제를 최근 순으로 확인합니다.</p>
          </div>
        </div>
        {loading ? (
          <p className="admin-subtext">불러오는 중입니다.</p>
        ) : !summary || summary.recentOrders.length === 0 ? (
          <p className="admin-subtext">표시할 주문이 없습니다.</p>
        ) : (
          <div className="admin-table-wrap admin-revenue-orders-wrap">
            <table className="admin-table admin-revenue-orders-table">
              <thead>
                <tr>
                  <th>주문번호</th>
                  <th>회원</th>
                  <th>유형</th>
                  <th>금액</th>
                  <th>상태</th>
                  <th>시각</th>
                </tr>
              </thead>
              <tbody>
                {summary.recentOrders.map((order) => {
                  const status = orderStatusTone(order.status);
                  return (
                    <tr key={`${order.orderId}-${order.id}`}>
                      <td>
                        <span className="admin-order-id">{order.orderId}</span>
                      </td>
                      <td>
                        <div className="admin-table-user">
                          <strong>{order.displayName || '-'}</strong>
                          <span>{shortenLoginId(order.loginId)}</span>
                        </div>
                      </td>
                      <td>
                        <span className="admin-order-kind">{order.kind.replaceAll('_', ' ')}</span>
                      </td>
                      <td>
                        <strong className="admin-order-amount">{formatWon(order.amountWon)}</strong>
                      </td>
                      <td>
                        <span className={`admin-status-chip ${status.tone}`}>{status.label}</span>
                      </td>
                      <td>
                        <span className="admin-order-time">{formatDate(order.createdAt)}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AdminLayout>
  );
}
