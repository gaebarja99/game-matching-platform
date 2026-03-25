import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StreamsLayout from '../components/StreamsLayout';
import LiveThumb from '../components/LiveThumb';
import { fetchLiveStreams, fetchRecentStreams, type StreamItem } from '../api/streams';
import { resolveProfileImageUrl } from '../api/client';

function gameLabel(game: string | undefined): string {
  const map: Record<string, string> = {
    LEAGUE_OF_LEGENDS: '리그 오브 레전드',
    VALORANT: '발로란트',
    OVERWATCH: '오버워치',
    PUBG: 'PUBG',
    COUNTER_STRIKE_2: '카운터 스트라이크 2',
    APEX_LEGENDS: '에이펙스 레전드',
    OTHERS: '기타',
  };
  return game ? (map[game] ?? game) : '게임';
}

function isStreamLiveRow(s: StreamItem): boolean {
  const st = (s.status || '').toUpperCase();
  return st === 'LIVE' && !s.endedAt;
}

function StreamCard({ s, showLiveBadge = true }: { s: StreamItem; showLiveBadge?: boolean }) {
  const isLive = showLiveBadge && isStreamLiveRow(s);
  const showLivePreview = isStreamLiveRow(s) && !!s.playbackUrl;

  return (
    <Link to={`/watch/${s.id}`} className="card">
      <div className="card-thumb">
        <div className="card-thumb-inner">
          {showLivePreview ? (
            <LiveThumb playbackUrl={s.playbackUrl!} />
          ) : (
            <div className="thumb-placeholder">{isLive ? '🔴 방송 중' : ''}</div>
          )}
        </div>
        {isLive && <span className="live-badge">LIVE</span>}
        {isLive && s.viewerCount != null && <span className="viewers">{s.viewerCount}명 시청 중</span>}
        {s.game && <span className="thumb-game">{gameLabel(s.game)}</span>}
      </div>
      <div className="card-body">
        <div className="card-avatar">
          {resolveProfileImageUrl(s.broadcasterProfileImageUrl) ? (
            <img src={resolveProfileImageUrl(s.broadcasterProfileImageUrl)!} alt="" />
          ) : (
            <span>{(s.broadcasterNickname || '?')[0]}</span>
          )}
        </div>
        <div className="card-info">
          <div className="card-title">{s.title || '방송'}</div>
          <div className="card-meta">
            {isLive && <span className="live-dot" />}
            <span>{s.broadcasterNickname || '방송자'}</span>
          </div>
          <span className="card-category">{gameLabel(s.game)}</span>
        </div>
      </div>
    </Link>
  );
}

export default function Streams() {
  const [liveList, setLiveList] = useState<StreamItem[]>([]);
  const [recentList, setRecentList] = useState<StreamItem[]>([]);
  const [loadingLive, setLoadingLive] = useState(true);
  const [loadingRecent, setLoadingRecent] = useState(true);

  useEffect(() => {
    setLoadingLive(true);
    fetchLiveStreams().then((list) => {
      setLiveList(list);
      setLoadingLive(false);
    });
    const t = setInterval(() => fetchLiveStreams().then(setLiveList), 5000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    setLoadingRecent(true);
    fetchRecentStreams(50).then((list) => {
      setRecentList(list);
      setLoadingRecent(false);
    });
    const tr = setInterval(() => fetchRecentStreams(50).then(setRecentList), 10000);
    return () => clearInterval(tr);
  }, []);

  return (
    <StreamsLayout>
      <section className="section">
        <div className="section-head">
          <h2 className="section-title"><span className="title-icon">🎵</span> 지금 라이브</h2>
          <Link to="/streams" className="section-more">전체보기</Link>
        </div>
        {loadingLive ? (
          <div className="empty">로딩 중...</div>
        ) : liveList.length === 0 ? (
          <div className="empty">현재 라이브 방송이 없습니다.</div>
        ) : (
          <div className="grid">
            {liveList.map((s) => (
              <StreamCard key={s.id} s={s} showLiveBadge />
            ))}
          </div>
        )}
      </section>

      <section className="section">
        <div className="section-head">
          <h2 className="section-title"><span className="title-icon">📺</span> 최근 방송</h2>
          <Link to="/streams" className="section-more">전체보기</Link>
        </div>
        {loadingRecent ? (
          <div className="empty">로딩 중...</div>
        ) : recentList.length === 0 ? (
          <div className="empty">최근 방송이 없습니다.</div>
        ) : (
          <div className="grid">
            {recentList.map((s) => (
              <StreamCard key={s.id} s={s} showLiveBadge={false} />
            ))}
          </div>
        )}
      </section>
    </StreamsLayout>
  );
}
