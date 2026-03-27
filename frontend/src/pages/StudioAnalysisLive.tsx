import { useEffect, useMemo, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface AnalysisRow {
  id: number;
  title: string;
  durationSeconds: number;
  playCount: number;
  totalViewers: number;
  maxConcurrentViewers: number;
  avgConcurrentViewers: number;
  watchTimeSeconds: number;
  avgWatchSeconds: number;
  chatParticipants: number;
  chatParticipationRate: number;
  donationAmount: number;
  donationCount: number;
}

interface LiveAnalysisResponse {
  liveCount: number;
  totalDurationSeconds: number;
  avgDurationSeconds: number;
  playCount: number;
  totalViewers: number;
  totalWatchTimeSeconds: number;
  avgWatchSeconds: number;
  maxConcurrentViewers: number;
  avgConcurrentViewers: number;
  retentionRate: number;
  chatParticipants: number;
  chatParticipationRate: number;
  donationAmount: number;
  donationCount: number;
  rows: AnalysisRow[];
}

const EMPTY_ANALYSIS: LiveAnalysisResponse = {
  liveCount: 0,
  totalDurationSeconds: 0,
  avgDurationSeconds: 0,
  playCount: 0,
  totalViewers: 0,
  totalWatchTimeSeconds: 0,
  avgWatchSeconds: 0,
  maxConcurrentViewers: 0,
  avgConcurrentViewers: 0,
  retentionRate: 0,
  chatParticipants: 0,
  chatParticipationRate: 0,
  donationAmount: 0,
  donationCount: 0,
  rows: [],
};

function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) return '00:00:00';
  const safeSeconds = Math.floor(seconds);
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const remainSeconds = safeSeconds % 60;
  return [hours, minutes, remainSeconds]
    .map((value) => value.toString().padStart(2, '0'))
    .join(':');
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('ko-KR').format(value ?? 0);
}

function formatPeople(value: number): string {
  return `${formatNumber(value)}명`;
}

function formatPercent(value: number): string {
  return `${formatNumber(value)}%`;
}

function formatDonation(amount: number, count: number): string {
  return `${formatNumber(amount)} + (${formatNumber(count)})`;
}

function getDateRangeLabel(rows: AnalysisRow[]): string {
  if (rows.length === 0) return '집계 데이터 없음';
  return '전체 방송 기준 집계';
}

export default function StudioAnalysisLive() {
  const [analysis, setAnalysis] = useState<LiveAnalysisResponse>(EMPTY_ANALYSIS);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetch(apiUrl('api/studio/analytics/live'), { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('failed');
        }
        return response.json();
      })
      .then((data: LiveAnalysisResponse) => {
        if (!cancelled) {
          setAnalysis({
            ...EMPTY_ANALYSIS,
            ...data,
            rows: Array.isArray(data?.rows) ? data.rows : [],
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setAnalysis(EMPTY_ANALYSIS);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const dateRange = useMemo(() => getDateRangeLabel(analysis.rows), [analysis.rows]);

  const kpiCards = [
    { label: '진행한 라이브', value: `${formatNumber(analysis.liveCount)}개` },
    { label: '라이브 시간', value: formatDuration(analysis.totalDurationSeconds) },
    { label: '재생수', value: `${formatNumber(analysis.playCount)}회` },
    { label: '시청자수', value: formatPeople(analysis.totalViewers) },
    { label: '평균 시청 시간', value: formatDuration(analysis.avgWatchSeconds) },
    { label: '최대 동시 시청자', value: formatPeople(analysis.maxConcurrentViewers) },
    { label: '평균 동시 시청자', value: formatPeople(analysis.avgConcurrentViewers) },
    { label: '평균 시청 지속률', value: formatPercent(analysis.retentionRate) },
    {
      label: '채팅 참여자',
      value: `${formatPeople(analysis.chatParticipants)} (${formatPercent(analysis.chatParticipationRate)})`,
    },
    { label: '후원(건)', value: formatDonation(analysis.donationAmount, analysis.donationCount) },
  ];

  return (
    <StudioLayout>
      <h1 className="page-title">라이브 스트리밍 분석</h1>
      <div className="toolbar">
        <input type="text" readOnly value={dateRange} />
        <button type="button" className="btn-query">조회</button>
        <button type="button" className="btn-download">다운로드</button>
      </div>

      <div className="kpi-grid">
        {kpiCards.map((card) => (
          <div key={card.label} className="kpi-card">
            <div className="kpi-label">{card.label}</div>
            <div className="kpi-value">{card.value}</div>
          </div>
        ))}
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>영상</th>
            <th>재생수</th>
            <th>총 시청자</th>
            <th>최대 동시 시청자</th>
            <th>평균 동시 시청자</th>
            <th>총 방송 시간</th>
            <th>평균 시청 시간</th>
            <th>채팅 참여</th>
            <th>후원</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={9} className="empty-msg">
                로딩 중...
              </td>
            </tr>
          ) : analysis.rows.length === 0 ? (
            <tr>
              <td colSpan={9} className="empty-msg">
                집계할 데이터가 없습니다.
              </td>
            </tr>
          ) : (
            analysis.rows.map((row) => (
              <tr key={row.id}>
                <td>{row.title || '(제목 없음)'}</td>
                <td>{formatNumber(row.playCount)}회</td>
                <td>{formatPeople(row.totalViewers)}</td>
                <td>{formatPeople(row.maxConcurrentViewers)}</td>
                <td>{formatPeople(row.avgConcurrentViewers)}</td>
                <td>{formatDuration(row.watchTimeSeconds)}</td>
                <td>{formatDuration(row.avgWatchSeconds)}</td>
                <td>{formatPeople(row.chatParticipants)} ({formatPercent(row.chatParticipationRate)})</td>
                <td>{formatDonation(row.donationAmount, row.donationCount)}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </StudioLayout>
  );
}
