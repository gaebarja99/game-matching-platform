import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import ChannelLayout from '../components/ChannelLayout';
import { apiUrl, resolveProfileImageUrl } from '../api/client';

interface StreamItem {
  id: number;
  userId?: number;
  title?: string;
  game?: string;
  status?: string;
  broadcasterNickname?: string;
  thumbnailUrl?: string;
  followerCount?: number;
  createdAt?: string;
  startedAt?: string;
  endedAt?: string;
  visibleInRecent?: boolean;
  canManage?: boolean;
}

interface ChannelCommunityPost {
  id: number;
  userId: number;
  title: string;
  content: string;
  createdAt: string;
}

interface ChannelProfile {
  id: number;
  loginId?: string;
  username?: string;
  nickname?: string;
  bio?: string;
  profileImageUrl?: string;
  followerCount?: number;
}

const CHANNEL_COMMUNITY_STORAGE_KEY = 'gamematcher-channel-community-posts';

function loadChannelCommunityPosts(userId?: number | null): ChannelCommunityPost[] {
  try {
    const raw = localStorage.getItem(CHANNEL_COMMUNITY_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as ChannelCommunityPost[];
    if (!Array.isArray(parsed)) return [];
    if (!userId) return parsed;
    return parsed.filter((post) => post.userId === userId);
  } catch {
    return [];
  }
}

function formatChannelDate(value?: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  });
}

function formatDuration(totalMinutes: number): string {
  const safeMinutes = Math.max(0, totalMinutes);
  const hours = Math.floor(safeMinutes / 60);
  const minutes = safeMinutes % 60;
  if (hours <= 0) return `${minutes}분`;
  return `${hours}시간 ${minutes}분`;
}

export default function Channel() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [streams, setStreams] = useState<StreamItem[]>([]);
  const [profile, setProfile] = useState<ChannelProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [communityPosts, setCommunityPosts] = useState<ChannelCommunityPost[]>([]);

  const paramUserId = Number(searchParams.get('userId') || 0) || null;
  const channelUserId = paramUserId ?? user?.id ?? null;
  const isOwnChannel = !!user?.id && !!channelUserId && user.id === channelUserId;

  const currentTab = (() => {
    const tab = searchParams.get('tab');
    if (tab === 'community' || tab === 'info') return tab;
    return 'home';
  })();

  useEffect(() => {
    if (!channelUserId) {
      setLoading(false);
      setProfile(null);
      setStreams([]);
      setCommunityPosts([]);
      return;
    }

    setLoading(true);
    Promise.all([
      fetch(apiUrl(`api/profile/public/${channelUserId}`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : null))
        .then((payload: ChannelProfile | null) => setProfile(payload))
        .catch(() => setProfile(null)),
      fetch(apiUrl(isOwnChannel ? 'api/streams/by-user/me' : `api/streams/by-user/${channelUserId}`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : []))
        .then((list: StreamItem[]) => setStreams(Array.isArray(list) ? list : []))
        .catch(() => setStreams([])),
    ]).finally(() => setLoading(false));

    const syncPosts = () => setCommunityPosts(loadChannelCommunityPosts(channelUserId));
    syncPosts();
    window.addEventListener('storage', syncPosts);
    return () => window.removeEventListener('storage', syncPosts);
  }, [channelUserId, isOwnChannel]);

  const name = profile?.nickname || profile?.username || profile?.loginId || '사용자';
  const followerCount = Number(profile?.followerCount ?? 0);
  const hasStreams = streams.length > 0;

  const firstStreamDate = useMemo(() => {
    if (!streams.length) return '-';
    const timestamps = streams
      .map((stream) => stream.createdAt ?? stream.startedAt)
      .filter((value): value is string => Boolean(value))
      .map((value) => new Date(value))
      .filter((date) => !Number.isNaN(date.getTime()))
      .sort((a, b) => a.getTime() - b.getTime());
    return timestamps.length ? formatChannelDate(timestamps[0].toISOString()) : '-';
  }, [streams]);

  const totalBroadcastMinutes = useMemo(() => {
    return streams.reduce((sum, stream) => {
      const start = stream.startedAt ? new Date(stream.startedAt) : null;
      const end = stream.endedAt ? new Date(stream.endedAt) : null;
      if (!start || !end) return sum;
      if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return sum;
      const minutes = Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000));
      return sum + minutes;
    }, 0);
  }, [streams]);

  const communityPostsSorted = useMemo(() => {
    return [...communityPosts].sort((a, b) => {
      const ta = new Date(a.createdAt).getTime();
      const tb = new Date(b.createdAt).getTime();
      return (Number.isNaN(tb) ? 0 : tb) - (Number.isNaN(ta) ? 0 : ta);
    });
  }, [communityPosts]);

  if (!channelUserId) {
    return (
      <ChannelLayout>
        <div className="channel-login-msg">
          <p>채널을 불러올 수 없습니다.</p>
          <Link to="/login">로그인하기</Link>
        </div>
      </ChannelLayout>
    );
  }

  async function toggleVisibility(stream: StreamItem) {
    const nextVisible = stream.visibleInRecent === false;
    const response = await fetch(apiUrl(`api/streams/${stream.id}/channel-visibility`), {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ visibleInRecent: nextVisible }),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      alert(payload?.message || '영상 공개 상태를 변경하지 못했습니다.');
      return;
    }
    setStreams((current) =>
      current.map((item) => (item.id === stream.id ? { ...item, ...(payload as StreamItem) } : item))
    );
  }

  async function deleteStream(stream: StreamItem) {
    if (!window.confirm(`"${stream.title || '이 영상'}"을(를) 삭제할까요?`)) return;
    const response = await fetch(apiUrl(`api/streams/${stream.id}/channel`), {
      method: 'DELETE',
      credentials: 'include',
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      alert(payload?.message || '영상을 삭제하지 못했습니다.');
      return;
    }
    setStreams((current) => current.filter((item) => item.id !== stream.id));
  }

  return (
    <ChannelLayout>
      <div className="channel-shell">
        <div className="channel-header">
          <div className="channel-avatar">
            {resolveProfileImageUrl(profile?.profileImageUrl) ? (
              <img src={resolveProfileImageUrl(profile?.profileImageUrl)!} alt="" />
            ) : (
              <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={48} height={48}>
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
              </svg>
            )}
          </div>
          <div className="channel-info">
            <h1>{name}</h1>
            <p className="channel-meta">팔로워 {followerCount}명</p>
            <p className="channel-greeting">{profile?.bio?.trim() || '안녕하세요!'}</p>
            <Link to={isOwnChannel ? '/profile' : `/channel?userId=${channelUserId}`}>프로필 보기 &gt;</Link>
          </div>
        </div>

        <div className="channel-tabs">
          <Link to={isOwnChannel ? '/channel' : `/channel?userId=${channelUserId}`} className={currentTab === 'home' ? 'active' : ''}>홈</Link>
          <Link to={isOwnChannel ? '/channel?tab=community' : `/channel?userId=${channelUserId}&tab=community`} className={currentTab === 'community' ? 'active' : ''}>커뮤니티</Link>
          <Link to={isOwnChannel ? '/channel?tab=info' : `/channel?userId=${channelUserId}&tab=info`} className={currentTab === 'info' ? 'active' : ''}>정보</Link>
        </div>

        {currentTab === 'home' ? (
          loading ? (
            <div className="channel-empty"><p className="channel-empty-title">불러오는 중...</p></div>
          ) : !hasStreams ? (
            <div className="channel-empty">
              <p className="channel-empty-title">등록된 콘텐츠가 없습니다.</p>
              <p className="channel-empty-desc">방송을 시작해 보세요.</p>
              {isOwnChannel ? <Link to="/studio/live" className="btn-studio">방송하기</Link> : null}
            </div>
          ) : (
            <div className="channel-grid">
              {streams.map((stream) => {
                const isLive = (stream.status || '').toUpperCase() === 'LIVE';
                const isHidden = stream.visibleInRecent === false;
                return (
                  <div key={stream.id} className={`channel-card${isHidden ? ' is-hidden' : ''}`}>
                    <Link to={`/watch/${stream.id}`} className="channel-card-link">
                      <div className="channel-card-thumb">
                        {stream.thumbnailUrl ? <img src={stream.thumbnailUrl} alt="" /> : null}
                        {isLive ? <span className="live-badge">LIVE</span> : <div className="ended">종료</div>}
                        {isHidden ? <span className="channel-hidden-badge">숨김</span> : null}
                      </div>
                      <div className="channel-card-body">
                        <div className="channel-card-title">{stream.title || '방송'}</div>
                        <div className="channel-card-meta">{name} · {stream.game ?? ''}</div>
                      </div>
                    </Link>
                    {stream.canManage ? (
                      <div className="channel-card-actions">
                        <button type="button" className="channel-card-action" onClick={() => toggleVisibility(stream)}>
                          {isHidden ? '다시 공개' : '숨기기'}
                        </button>
                        <button type="button" className="channel-card-action danger" onClick={() => deleteStream(stream)}>
                          삭제
                        </button>
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          )
        ) : null}

        {currentTab === 'community' ? (
          <div className="channel-info-stack">
            <section className="channel-panel">
              <div className="channel-panel-header">
                <div>
                  <h2>채널 커뮤니티</h2>
                  <p>스트리머가 팬들과 방송 소식, 공지, 일상을 나누는 채널 전용 소통 공간입니다.</p>
                </div>
                {isOwnChannel ? (
                  <Link to="/channel/write" className="channel-panel-action channel-panel-action--primary">
                    글쓰기
                  </Link>
                ) : null}
              </div>

              {communityPostsSorted.length === 0 ? (
                <p className="channel-panel-empty">아직 작성된 커뮤니티 글이 없습니다.</p>
              ) : (
                <ul className="channel-community-list" aria-label="채널 커뮤니티 글 목록">
                  {communityPostsSorted.map((post) => (
                    <li key={post.id}>
                      <article className="channel-community-card">
                        <div className="channel-community-card-head">
                          <h3 className="channel-community-card-title">{post.title}</h3>
                          <time className="channel-community-meta" dateTime={post.createdAt}>
                            {formatChannelDate(post.createdAt)}
                          </time>
                        </div>
                        <p className="channel-community-excerpt">{post.content}</p>
                      </article>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        ) : null}

        {currentTab === 'info' ? (
          <div className="channel-info-stack">
            <section className="channel-panel">
              <h2>정보</h2>
              <p className="channel-panel-empty">등록된 정보 링크가 없습니다.</p>
            </section>

            <section className="channel-panel">
              <div className="channel-stats-grid">
                <div className="channel-stat-row">
                  <span>첫 방송일</span>
                  <strong>{firstStreamDate}</strong>
                </div>
                <div className="channel-stat-row">
                  <span>총 방송 시간</span>
                  <strong>{formatDuration(totalBroadcastMinutes)}</strong>
                </div>
              </div>
            </section>
          </div>
        ) : null}
      </div>
    </ChannelLayout>
  );
}
