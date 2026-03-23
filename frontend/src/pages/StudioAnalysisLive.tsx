import { useState, useEffect } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface StreamRow {
  id: number;
  title: string;
  status?: string;
  startedAt?: string;
  endedAt?: string;
  viewerCount?: number;
}

function formatDuration(sec: number): string {
  if (sec == null || !Number.isFinite(sec) || sec < 0) return '00:00:00';
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = Math.floor(sec % 60);
  return [h, m, s].map((n) => (n < 10 ? '0' : '') + n).join(':');
}

function parseIsoToMs(iso: string | null | undefined): number | null {
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d.getTime();
}

export default function StudioAnalysisLive() {
  const [streams, setStreams] = useState<StreamRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [kpi, setKpi] = useState({
    liveCount: 0,
    liveDurationSec: 0,
    plays: 0,
    viewers: 0,
    watchTime: '00:00:00',
    maxCcu: 0,
    avgCcu: 0,
    retention: '0%',
    chat: '0명 (0%)',
    donation: '0 + (0)',
  });

  useEffect(() => {
    fetch(apiUrl('api/streams/by-user/me'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: StreamRow[]) => {
        const filtered = Array.isArray(list) ? list.filter((s) => s.status === 'LIVE' || s.status === 'ENDED') : [];
        setStreams(filtered);

        const now = Date.now();
        let totalSec = 0;
        let viewerSum = 0;
        let maxCcu = 0;
        filtered.forEach((s) => {
          const start = parseIsoToMs(s.startedAt);
          const end = s.endedAt ? parseIsoToMs(s.endedAt) : now;
          if (start != null && end != null) totalSec += Math.max(0, Math.floor((end - start) / 1000));
          const v = s.viewerCount ?? 0;
          viewerSum += v;
          if (v > maxCcu) maxCcu = v;
        });
        const avgCcu = filtered.length ? Math.round(viewerSum / filtered.length) : 0;

        setKpi({
          liveCount: filtered.length,
          liveDurationSec: totalSec,
          plays: 0,
          viewers: viewerSum,
          watchTime: '00:00:00',
          maxCcu,
          avgCcu,
          retention: '0%',
          chat: '0명 (0%)',
          donation: '0 + (0)',
        });
      })
      .catch(() => setStreams([]))
      .finally(() => setLoading(false));
  }, []);

  const dateRange = '2026.02.08.-2026.03.10.';

  const kpiCards = [
    { label: '진행한 라이브', value: `${kpi.liveCount}개` },
    { label: '라이브 시간', value: formatDuration(kpi.liveDurationSec) },
    { label: '재생수', value: `${kpi.plays}회` },
    { label: '시청자수', value: `${kpi.viewers}명` },
    { label: '시청 시간', value: kpi.watchTime },
    { label: '최대 동시 시청자', value: `${kpi.maxCcu}명` },
    { label: '평균 동시 시청자', value: `${kpi.avgCcu}명` },
    { label: '평균 시청 지속률', value: kpi.retention },
    { label: '채팅 참여자', value: kpi.chat },
    { label: '후원(건)', value: kpi.donation },
  ];

  const getStreamDuration = (s: StreamRow): number => {
    const start = parseIsoToMs(s.startedAt);
    const end = s.endedAt ? parseIsoToMs(s.endedAt) : Date.now();
    if (start == null || end == null) return 0;
    return Math.max(0, Math.floor((end - start) / 1000));
  };

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
            <th>최대 동시시청자</th>
            <th>평균 동시시청자</th>
            <th>총 시청시간</th>
            <th>평균 시청 지속 시간</th>
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
          ) : streams.length === 0 ? (
            <tr>
              <td colSpan={9} className="empty-msg">
                데이터가 없습니다.
              </td>
            </tr>
          ) : (
            streams.map((s) => {
              const v = s.viewerCount ?? 0;
              const dur = getStreamDuration(s);
              return (
                <tr key={s.id}>
                  <td>
                    {s.title || '(제목 없음)'}
                    {s.status === 'LIVE' && <span style={{ color: '#e91916', fontWeight: 600, marginLeft: 6 }}>LIVE</span>}
                  </td>
                  <td>0</td>
                  <td>{v}명</td>
                  <td>{v}명</td>
                  <td>{v}명</td>
                  <td>{formatDuration(dur)}</td>
                  <td>-</td>
                  <td>-</td>
                  <td>-</td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </StudioLayout>
  );
}
