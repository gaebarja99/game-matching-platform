import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ChannelLayout from '../components/ChannelLayout';
import { apiUrl, resolveProfileImageUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';

interface StreamItem {
  id: number;
  title?: string;
  game?: string;
  status?: string;
  broadcasterNickname?: string;
  thumbnailUrl?: string;
}

export default function Channel() {
  const { user } = useAuth();
  const [streams, setStreams] = useState<StreamItem[]>([]);
  const [loading, setLoading] = useState(!!user);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }

    fetch(apiUrl('api/streams/by-user/me'), { credentials: 'include' })
      .then((response) => (response.ok ? response.json() : []))
      .then((list: StreamItem[]) => setStreams(Array.isArray(list) ? list : []))
      .catch(() => setStreams([]))
      .finally(() => setLoading(false));
  }, [user]);

  if (!user) {
    return (
      <ChannelLayout>
        <div className="channel-login-msg">
          <p>내 채널을 보려면 로그인해 주세요.</p>
          <Link to="/login">로그인하기</Link>
        </div>
      </ChannelLayout>
    );
  }

  const name = user.nickname ?? user.username ?? user.loginId ?? '사용자';
  const hasStreams = streams.length > 0;

  return (
    <ChannelLayout>
      <div className="channel-header">
        <div className="channel-avatar">
          {resolveProfileImageUrl(user.profileImageUrl) ? (
            <img src={resolveProfileImageUrl(user.profileImageUrl)!} alt="" />
          ) : (
            <svg className="avatar-placeholder" viewBox="0 0 24 24" fill="currentColor" width={48} height={48}>
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          )}
        </div>
        <div className="channel-info">
          <h1>{name}</h1>
          <p className="channel-meta">팔로워 0명</p>
          <Link to="/profile">프로필 보기 &gt;</Link>
        </div>
      </div>

      <div className="channel-tabs">
        <Link to="/channel" className="active">
          홈
        </Link>
        <a href="#" onClick={(event) => event.preventDefault()} style={{ cursor: 'default', opacity: 0.6 }}>
          동영상
        </a>
        <Link to="/community">커뮤니티</Link>
        <a href="#" onClick={(event) => event.preventDefault()} style={{ cursor: 'default', opacity: 0.6 }}>
          정보
        </a>
      </div>

      {loading ? (
        <div className="channel-empty">
          <div className="channel-empty-icon">📺</div>
          <p className="channel-empty-title">불러오는 중...</p>
        </div>
      ) : !hasStreams ? (
        <div className="channel-empty">
          <div className="channel-empty-icon">📺</div>
          <p className="channel-empty-title">등록된 콘텐츠가 없습니다.</p>
          <p className="channel-empty-desc">방송을 시작해 보세요.</p>
          <Link to="/studio/live" className="btn-studio">
            방송하기
          </Link>
        </div>
      ) : (
        <div className="channel-grid">
          {streams.map((stream) => {
            const isLive = (stream.status || '').toUpperCase() === 'LIVE';

            return (
              <div key={stream.id} className="channel-card">
                <Link to={`/watch/${stream.id}`}>
                  <div className="channel-card-thumb">
                    {stream.thumbnailUrl ? (
                      <img src={stream.thumbnailUrl} alt="" />
                    ) : (
                      <div
                        style={{
                          width: '100%',
                          height: '100%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: '#6b6b70',
                        }}
                      >
                        방송
                      </div>
                    )}
                    {isLive ? <span className="live-badge">LIVE</span> : <div className="ended">종료</div>}
                  </div>
                  <div className="channel-card-body">
                    <div className="channel-card-title">{stream.title || '방송'}</div>
                    <div className="channel-card-meta">
                      {stream.broadcasterNickname ?? ''} · {stream.game ?? ''}
                    </div>
                  </div>
                </Link>
              </div>
            );
          })}
        </div>
      )}
    </ChannelLayout>
  );
}
