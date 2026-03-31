import { useEffect, useState } from 'react';
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

interface DonationItem {
  id: number;
  streamId: number;
  toUserId?: number;
  streamerNickname?: string;
  streamTitle?: string;
  amount: number;
  message?: string;
  createdAt: string;
}

function formatWatchTime(seconds: number): string {
  if (seconds < 60) return `${seconds}초`;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h === 0) return `${m}분`;
  return `${h}시간 ${m}분`;
}

function formatDateTime(isoStr: string): string {
  if (!isoStr) return '-';
  try {
    const d = new Date(isoStr);
    return d.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
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
  const [donationList, setDonationList] = useState<DonationItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      setWatchList([]);
      setBroadcastList([]);
      setDonationList([]);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    Promise.all([
      fetch(apiUrl('api/history/watch'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
      fetch(apiUrl('api/history/broadcast'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
      fetch(apiUrl('api/donate/me?page=0&size=50'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { items: [] })),
    ])
      .then(([watchResponse, broadcastResponse, donationResponse]) => {
        if (cancelled) return;
        setWatchList(Array.isArray(watchResponse?.list) ? watchResponse.list : []);
        setBroadcastList(Array.isArray(broadcastResponse?.list) ? broadcastResponse.list : []);
        setDonationList(Array.isArray(donationResponse?.items) ? donationResponse.items : []);
      })
      .catch(() => {
        if (cancelled) return;
        setWatchList([]);
        setBroadcastList([]);
        setDonationList([]);
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [user]);

  return (
    <StreamsLayout>
      <h1 className="page-title">히스토리</h1>

      <div className="history-section">
        <h2>
          <span className="section-icon">시청</span> 시청 기록
        </h2>
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
                    <td>{item.streamerNickname || '-'}</td>
                    <td>
                      <Link to={`/watch/${item.streamId}`} className="history-link">
                        {item.streamTitle || '-'}
                      </Link>
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
        <h2>
          <span className="section-icon">방송</span> 내 방송 기록
        </h2>
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
                  <th>시작 / 종료</th>
                </tr>
              </thead>
              <tbody>
                {broadcastList.map((item) => (
                  <tr key={item.streamId}>
                    <td>{item.title || '-'}</td>
                    <td>{item.status === 'LIVE' ? '방송중' : item.status === 'ENDED' ? '종료' : '대기'}</td>
                    <td>{formatWatchTime(Number(item.durationSeconds) || 0)}</td>
                    <td>
                      {item.startedAt && item.endedAt
                        ? `${formatDateTime(item.startedAt)} - ${formatDateTime(item.endedAt)}`
                        : item.startedAt
                          ? `${formatDateTime(item.startedAt)} -`
                          : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="history-section">
        <h2>
          <span className="section-icon">후원</span> 내 후원 기록
        </h2>
        {!user ? (
          <p className="history-empty">로그인하면 내가 후원한 내역을 볼 수 있습니다.</p>
        ) : loading ? (
          <p className="history-empty">불러오는 중...</p>
        ) : donationList.length === 0 ? (
          <p className="history-empty">후원 기록이 없습니다.</p>
        ) : (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>일시</th>
                  <th>스트리머</th>
                  <th>방송 제목</th>
                  <th>후원 팡</th>
                  <th>메시지</th>
                </tr>
              </thead>
              <tbody>
                {donationList.map((item) => (
                  <tr key={item.id}>
                    <td>{formatDateTime(item.createdAt)}</td>
                    <td>{item.streamerNickname || '-'}</td>
                    <td>
                      <Link to={`/watch/${item.streamId}`} className="history-link">
                        {item.streamTitle || '방송 보기'}
                      </Link>
                    </td>
                    <td className="history-pang-value">{(Number(item.amount) || 0).toLocaleString()}팡</td>
                    <td>{item.message?.trim() ? item.message : '-'}</td>
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
