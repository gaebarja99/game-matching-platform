import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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
  APEX_LEGENDS: '에이펙스',
  OTHERS: '기타',
};

function gameLabel(game: string | null): string {
  return (game && GAME_LABELS[game]) || game || '기타';
}

export default function ProfileFollowing() {
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

    fetch(apiUrl(`api/streams/following?type=${tab}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list) => setStreams(Array.isArray(list) ? list : []))
      .catch(() => setStreams([]))
      .finally(() => setLoading(false));
  }, [tab, user]);

  const liveStreams = streams.filter((stream) => (stream.status || '').toUpperCase() === 'LIVE');
  const liveUserIds = new Set(liveStreams.map((stream) => stream.userId).filter(Boolean));
  const offlineList = followList.filter((follow) => !liveUserIds.has(follow.userId));

  const renderStreamCard = (stream: StreamItem) => {
    const isLive = (stream.status || '').toUpperCase() === 'LIVE';

    return (
      <Link key={stream.id} to={`/watch/${stream.id}`} className="card">
        <div className="card-thumb">
          {isLive ? <span className="live-badge">LIVE</span> : null}
          {isLive && stream.viewerCount != null ? (
            <span className="viewers">{stream.viewerCount.toLocaleString('ko-KR')}명 시청 중</span>
          ) : null}
          <div className="thumb-placeholder">
            {isLive ? '방송 중' : '최근 방송'}
            <br />
            <span style={{ fontSize: '0.75rem' }}>{gameLabel(stream.game)}</span>
          </div>
        </div>
        <div className="card-body">
          <div className="card-avatar">
            {resolveProfileImageUrl(stream.broadcasterProfileImageUrl) ? (
              <img src={resolveProfileImageUrl(stream.broadcasterProfileImageUrl)!} alt="" />
            ) : null}
          </div>
          <div className="card-info">
            <div className="card-title">{stream.title || '방송 제목 없음'}</div>
            <div className="card-meta">{stream.broadcasterNickname || '스트리머'}</div>
            <div className="card-category">{gameLabel(stream.game)}</div>
          </div>
        </div>
      </Link>
    );
  };

  const renderContent = () => {
    if (loading) {
      return <p className="following-empty-msg">불러오는 중입니다.</p>;
    }

    if (tab === 'all') {
      if (followList.length === 0) {
        return (
          <div className="following-empty">
            <div className="following-empty-icon">F</div>
            <p className="following-empty-msg">팔로잉한 채널이 없습니다.</p>
            <Link to="/streams" className="btn-go">방송 둘러보기</Link>
          </div>
        );
      }

      return (
        <>
          {liveStreams.length > 0 ? (
            <div className="following-online-section">
              <h3 className="following-section-title">라이브</h3>
              <div className="grid">{liveStreams.map(renderStreamCard)}</div>
            </div>
          ) : null}
          {offlineList.length > 0 ? (
            <div className="following-offline-section">
              <h3 className="following-section-title">오프라인</h3>
              <div className="channel-chips-wrap">
                {offlineList.map((follow) => (
                  <Link key={follow.userId} to="/channel" className="channel-chip">
                    <div className="channel-chip-avatar">
                      {resolveProfileImageUrl(follow.profileImageUrl) ? <img src={resolveProfileImageUrl(follow.profileImageUrl)!} alt="" /> : null}
                    </div>
                    <span className="channel-chip-name">{follow.nickname || '유저'}</span>
                  </Link>
                ))}
              </div>
            </div>
          ) : null}
        </>
      );
    }

    if (streams.length === 0) {
      return (
        <div className="following-empty">
          <div className="following-empty-icon">F</div>
          <p className="following-empty-msg">{tab === 'live' ? '진행 중인 라이브가 없습니다.' : '최근 방송이 없습니다.'}</p>
          <Link to="/streams" className="btn-go">방송 둘러보기</Link>
        </div>
      );
    }

    return <div className="grid">{streams.map(renderStreamCard)}</div>;
  };

  return (
    <div className="following-page">
      <h1 className="page-title">팔로잉 채널</h1>
      <div className="following-tabs">
        <button type="button" className={tab === 'all' ? 'active' : ''} onClick={() => setTab('all')}>전체</button>
        <button type="button" className={tab === 'live' ? 'active' : ''} onClick={() => setTab('live')}>라이브</button>
        <button type="button" className={tab === 'recent' ? 'active' : ''} onClick={() => setTab('recent')}>최근 방송</button>
      </div>
      <div className="following-content">{renderContent()}</div>
    </div>
  );
}
