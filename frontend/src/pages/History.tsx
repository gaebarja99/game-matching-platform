import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import StreamsLayout from '../components/StreamsLayout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl } from '../api/client';

interface WatchItem {
  streamId: number;
  streamerNickname: string;
  streamTitle: string;
  watchSeconds: number;
}

interface BroadcastItem {
  streamId: number;
  title: string;
  status: string;
  startedAt: string;
  endedAt: string;
  durationSeconds: number;
}

interface PangItem {
  streamId: number;
  title: string;
  totalPang: number;
}

function formatWatchTime(seconds: number): string {
  if (seconds < 60) return `${seconds}분`;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h === 0) return `${m}분`;
  return `${h}시간 ${m}분`;
}

function formatDateTime(isoStr: string): string {
  if (!isoStr) return '—';
  try {
    const d = new Date(isoStr);
    return d.toLocaleString('ko-KR', {
      month: 'numeric',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return isoStr;
  }
}

export default function History() {
  const { user } = useAuth();
  const [watchList, setWatchList] = useState<WatchItem[]>([]);
  const [broadcastList, setBroadcastList] = useState<BroadcastItem[]>([]);
  const [pangList, setPangList] = useState<PangItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      setWatchList([]);
      setBroadcastList([]);
      setPangList([]);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    Promise.all([
      fetch(apiUrl('api/history/watch'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
      fetch(apiUrl('api/history/broadcast'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
      fetch(apiUrl('api/history/pang'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
    ])
      .then(([w, b, p]) => {
        if (cancelled) return;
        setWatchList(Array.isArray(w?.list) ? w.list : []);
        setBroadcastList(Array.isArray(b?.list) ? b.list : []);
        setPangList(Array.isArray(p?.list) ? p.list : []);
      })
      .catch(() => {
        if (!cancelled) {
          setWatchList([]);
          setBroadcastList([]);
          setPangList([]);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [user]);

  return (
    <StreamsLayout sidebarVariant="simple">
      <h1 className="page-title">히스토리</h1>

      <div className="history-section">
        <h2><span className="section-icon">📺</span> 시청 기록</h2>
        {!user ? (
          <p className="history-empty">로그인하면 시청 기록을 볼 수 있습니다.</p>
        ) : loading ? (
          <p className="history-empty">불러오는 중...</p>
        ) : watchList.length === 0 ? (
          <p className="history-empty">시청 기록이 없습니다.</p>
        ) : (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>스트리머</th>
                  <th>방송 제목</th>
                  <th>시청 시간</th>
                </tr>
              </thead>
              <tbody>
                {watchList.map((item) => (
                  <tr key={item.streamId}>
                    <td>{item.streamerNickname ?? '—'}</td>
                    <td>
                      <Link to={`/watch/${item.streamId}`} className="history-link">{item.streamTitle || '—'}</Link>
                    </td>
                    <td>{formatWatchTime(Number(item.watchSeconds) || 0)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="history-section">
        <h2><span className="section-icon">🕐</span> 내 방송 시간</h2>
        {!user ? (
          <p className="history-empty">로그인하면 방송 기록을 볼 수 있습니다.</p>
        ) : loading ? (
          <p className="history-empty">불러오는 중...</p>
        ) : broadcastList.length === 0 ? (
          <p className="history-empty">방송 기록이 없습니다.</p>
        ) : (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>방송 제목</th>
                  <th>상태</th>
                  <th>방송 시간</th>
                  <th>시작/종료</th>
                </tr>
              </thead>
              <tbody>
                {broadcastList.map((item) => (
                  <tr key={item.streamId}>
                    <td>{item.title || '—'}</td>
                    <td>{item.status === 'LIVE' ? '방송중' : item.status === 'ENDED' ? '종료' : '대기'}</td>
                    <td>{formatWatchTime(Number(item.durationSeconds) || 0)}</td>
                    <td>
                      {item.startedAt && item.endedAt
                        ? `${formatDateTime(item.startedAt)} - ${formatDateTime(item.endedAt)}`
                        : item.startedAt
                          ? `${formatDateTime(item.startedAt)} -`
                          : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="history-section">
        <h2><span className="section-icon">💰</span> 방송별 받은 팡</h2>
        {!user ? (
          <p className="history-empty">로그인하면 방송별 받은 팡을 볼 수 있습니다.</p>
        ) : loading ? (
          <p className="history-empty">불러오는 중...</p>
        ) : pangList.length === 0 ? (
          <p className="history-empty">받은 팡 기록이 없습니다.</p>
        ) : (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>방송 제목</th>
                  <th>받은 팡</th>
                </tr>
              </thead>
              <tbody>
                {pangList.map((item) => (
                  <tr key={item.streamId}>
                    <td>{item.title || '—'}</td>
                    <td className="history-pang-value">{(Number(item.totalPang) || 0).toLocaleString()} ₩</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </StreamsLayout>
  );
}
