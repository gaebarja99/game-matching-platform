import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import StreamsLayout from '../components/StreamsLayout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

type TabType = 'all' | 'live' | 'recent';

interface FollowingUser {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
}

interface StreamItem {
  id: number;
  title: string | null;
  game: string | null;
  status: string | null;
  playbackUrl: string | null;
  externalUrl: string | null;
  userId: number | null;
  broadcasterNickname: string | null;
  broadcasterProfileImageUrl: string | null;
  viewerCount: number | null;
}

const GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: '카운터 스트라이크 2',
  OTHERS: '기타',
};

function gameLabel(game: string | null): string {
  return (game && GAME_LABELS[game]) || game || '';
}

export default function Following() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabType>('all');
  const [loading, setLoading] = useState(true);
  const [followList, setFollowList] = useState<FollowingUser[]>([]);
  const [streams, setStreams] = useState<StreamItem[]>([]);

  useEffect(() => {
    if (!user) {
      setFollowList([]);
      setStreams([]);
      setLoading(false);
      return;
    }

    setLoading(true);

    if (tab === 'all') {
      Promise.all([
        fetch(apiUrl('api/follow/list'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : { list: [] })),
        fetch(apiUrl('api/streams/following?type=all'), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
      ])
        .then(([followRes, streamsList]) => {
          const list = Array.isArray(followRes?.list) ? followRes.list : [];
          setFollowList(list);
          setStreams(Array.isArray(streamsList) ? streamsList : []);
        })
        .catch(() => {
          setFollowList([]);
          setStreams([]);
        })
        .finally(() => setLoading(false));
      return;
    }

    if (tab === 'live' || tab === 'recent') {
      fetch(apiUrl(`api/streams/following?type=${tab}`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : []))
        .then((list) => setStreams(Array.isArray(list) ? list : []))
        .catch(() => setStreams([]))
        .finally(() => setLoading(false));
      return;
    }

    setLoading(false);
  }, [user, tab]);

  if (!user) {
    return (
      <StreamsLayout>
        <h1 className="page-title">팔로잉</h1>
        <div className="following-tabs">
          <button type="button" className="active">전체</button>
          <button type="button">라이브</button>
          <button type="button">최근 영상</button>
        </div>
        <div className="following-content">
          <div className="following-empty">
            <div className="following-empty-icon">♥</div>
            <p className="following-empty-msg">로그인하면 팔로우한 채널과 방송을 모아볼 수 있습니다.</p>
            <Link to="/login" className="btn-go">로그인하기</Link>
          </div>
        </div>
      </StreamsLayout>
    );
  }

  const liveStreams = streams.filter((s) => (s.status || '').toUpperCase() === 'LIVE');
  const liveUserIds = new Set(liveStreams.map((s) => s.userId).filter(Boolean));
  const offlineList = followList.filter((f) => !liveUserIds.has(f.userId));

  const renderStreamCard = (s: StreamItem) => {
    const isLive = (s.status || '').toUpperCase() === 'LIVE';
    return (
      <Link key={s.id} to={`/watch/${s.id}`} className="card">
        <div className="card-thumb">
          {isLive && <span className="live-badge">LIVE</span>}
          {isLive && s.viewerCount != null && <span className="viewers">{s.viewerCount}명 시청 중</span>}
          <div className="thumb-placeholder">
            {isLive ? '생방송 중' : ''}
            {s.game && <br />}
            {s.game && <span style={{ fontSize: '0.75rem' }}>{gameLabel(s.game)}</span>}
          </div>
        </div>
        <div className="card-body">
          <div className="card-avatar">
            {resolveProfileImageUrl(s.broadcasterProfileImageUrl) ? (
              <img src={resolveProfileImageUrl(s.broadcasterProfileImageUrl)!} alt="" />
            ) : null}
          </div>
          <div className="card-info">
            <div className="card-title">{s.title || '방송'}{s.externalUrl ? ' 트위치/유튜브' : ''}</div>
            <div className="card-meta">
              {isLive && <span className="live-dot" />}
              {s.broadcasterNickname || '방송자'}
            </div>
            <div className="card-category">{gameLabel(s.game)}</div>
          </div>
        </div>
      </Link>
    );
  };

  const renderContent = () => {
    if (loading) {
      return <p className="following-empty-msg">로딩 중...</p>;
    }

    if (tab === 'all') {
      if (followList.length === 0) {
        return (
          <div className="following-empty">
            <div className="following-empty-icon">♥</div>
            <p className="following-empty-msg">팔로우한 채널이 없습니다.</p>
            <Link to="/streams" className="btn-go">다른 방송 보러 가기</Link>
          </div>
        );
      }
      const hasLive = liveStreams.length > 0;
      const hasOffline = offlineList.length > 0;
      if (!hasLive && !hasOffline) {
        return (
          <div className="following-empty">
            <div className="following-empty-icon">♥</div>
            <p className="following-empty-msg">팔로우한 채널이 없습니다.</p>
            <Link to="/streams" className="btn-go">다른 방송 보러 가기</Link>
          </div>
        );
      }
      return (
        <>
          {hasLive && (
            <div className="following-online-section">
              <h3 className="following-section-title">온라인</h3>
              <div className="grid">{liveStreams.map(renderStreamCard)}</div>
            </div>
          )}
          {hasOffline && (
            <div className="following-offline-section">
              <h3 className="following-section-title">오프라인</h3>
              <div className="channel-chips-wrap">
                {offlineList.map((f) => (
                  <Link key={f.userId} to="/streams" className="channel-chip">
                    <div className="channel-chip-avatar">
                      {resolveProfileImageUrl(f.profileImageUrl) ? <img src={resolveProfileImageUrl(f.profileImageUrl)!} alt="" /> : null}
                    </div>
                    <span className="channel-chip-name">{f.nickname || '유저'}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </>
      );
    }

    if (streams.length === 0) {
      return (
        <div className="following-empty">
          <div className="following-empty-icon">♥</div>
          <p className="following-empty-msg">{tab === 'live' ? '진행 중인 라이브가 없습니다.' : '팔로우한 채널이 없습니다.'}</p>
          <Link to="/streams" className="btn-go">다른 방송 보러 가기</Link>
        </div>
      );
    }

    return <div className="grid">{streams.map(renderStreamCard)}</div>;
  };

  return (
    <StreamsLayout>
      <h1 className="page-title">팔로잉</h1>
      <div className="following-tabs">
        <button type="button" className={tab === 'all' ? 'active' : ''} onClick={() => setTab('all')}>전체</button>
        <button type="button" className={tab === 'live' ? 'active' : ''} onClick={() => setTab('live')}>라이브</button>
        <button type="button" className={tab === 'recent' ? 'active' : ''} onClick={() => setTab('recent')}>최근 영상</button>
      </div>
      <div className="following-content">{renderContent()}</div>
    </StreamsLayout>
  );
}
