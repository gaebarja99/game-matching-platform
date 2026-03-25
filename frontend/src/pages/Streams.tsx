import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import StreamsLayout from '../components/StreamsLayout';
import LiveThumb from '../components/LiveThumb';
import { resolveProfileImageUrl } from '../api/client';
import { fetchLiveStreams, fetchRecentStreams, type StreamItem } from '../api/streams';
import './Streams.css';

type CategoryGroup = 'ALL' | 'GAME' | 'ESPORTS' | 'SPORTS' | 'ENT';

type CategoryCard = {
  id: string;
  group: CategoryGroup;
  title: string;
  imageUrl?: string;
  accent: string;
  routeGame?: string;
  queryGame?: string;
};

const GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: '카운터 스트라이크 2',
  APEX_LEGENDS: '에이팩스',
  OTHERS: '기타 게임',
};

const CATEGORY_CARDS: CategoryCard[] = [
  {
    id: 'league',
    group: 'GAME',
    title: '리그 오브 레전드',
    accent: 'league',
    imageUrl: '/images/league-of-legends-card.png',
    routeGame: 'LEAGUE_OF_LEGENDS',
    queryGame: 'LEAGUE_OF_LEGENDS',
  },
  {
    id: 'valorant',
    group: 'GAME',
    title: '발로란트',
    imageUrl: '/images/valorant-card.png',
    accent: 'return',
    routeGame: 'VALORANT',
    queryGame: 'VALORANT',
  },
  {
    id: 'overwatch2',
    group: 'GAME',
    title: '오버워치2',
    imageUrl: '/images/overwatch2-card.png',
    accent: 'starrail',
    routeGame: 'OVERWATCH',
    queryGame: 'OVERWATCH',
  },
  {
    id: 'pubg',
    group: 'GAME',
    title: 'PUBG',
    imageUrl: '/images/pubg-card.png',
    accent: 'show',
    routeGame: 'PUBG',
    queryGame: 'PUBG',
  },
  {
    id: 'apex',
    group: 'GAME',
    title: '에이팩스',
    imageUrl: '/images/apex-card.jpg',
    accent: 'wow',
    routeGame: 'APEX_LEGENDS',
    queryGame: 'APEX_LEGENDS',
  },
  {
    id: 'other-games',
    group: 'GAME',
    title: '기타 게임',
    imageUrl: '/images/other-games-card.png',
    accent: 'composite',
    queryGame: 'OTHERS',
  },
];

const CATEGORY_FILTERS: Array<{ key: CategoryGroup; label: string }> = [
  { key: 'ALL', label: '전체' },
  { key: 'GAME', label: '게임' },
  { key: 'ESPORTS', label: 'e스포츠' },
  { key: 'SPORTS', label: '스포츠' },
  { key: 'ENT', label: '엔터' },
];

function gameLabel(game?: string | null) {
  if (!game) return '기타';
  return GAME_LABELS[game] ?? game;
}

function formatViewerCount(count?: number) {
  if (count == null) return '';
  return `${count.toLocaleString('ko-KR')}명 시청 중`;
}

function formatCompactCount(count: number) {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1).replace('.0', '')}만명`;
  }
  return `${count.toLocaleString('ko-KR')}명`;
}

const FEATURED_CATEGORY_GAMES = new Set(['LEAGUE_OF_LEGENDS', 'VALORANT', 'OVERWATCH', 'PUBG', 'APEX_LEGENDS']);

function buildCategoryStats(card: CategoryCard, streams: StreamItem[]) {
  const matched = streams.filter((stream) => {
    if (!card.queryGame) return false;
    if (card.queryGame === 'OTHERS') {
      return !FEATURED_CATEGORY_GAMES.has(String(stream.game || ''));
    }
    return stream.game === card.queryGame;
  });
  const liveCount = matched.length;
  const viewerCount = matched.reduce((sum, stream) => sum + (stream.viewerCount ?? 0), 0);
  return {
    liveCount,
    viewerCount,
  };
}

function StreamCard({ stream }: { stream: StreamItem }) {
  const profileImage = resolveProfileImageUrl(stream.broadcasterProfileImageUrl);
  const isLive = String(stream.status || '').toUpperCase() === 'LIVE';

  return (
    <Link to={`/watch/${stream.id}`} className="stream-card">
      <div className="stream-card-thumb">
        {stream.playbackUrl ? (
          <LiveThumb playbackUrl={stream.playbackUrl} className="stream-card-thumb-media" />
        ) : (
          <div className="stream-card-thumb-fallback">{gameLabel(stream.game)}</div>
        )}
        {isLive ? <span className="stream-card-live-badge">LIVE</span> : <span className="stream-card-ended-badge">종료</span>}
        {stream.viewerCount != null && <span className="stream-card-viewers">{formatViewerCount(stream.viewerCount)}</span>}
      </div>
      <div className="stream-card-info">
        <div className="stream-card-avatar">
          {profileImage ? <img src={profileImage} alt="" /> : <span>{(stream.broadcasterNickname ?? 'G')[0]}</span>}
        </div>
        <div className="stream-card-meta">
          <strong className="stream-card-title">{stream.title || '방송 제목 없음'}</strong>
          <span className="stream-card-broadcaster">
            {stream.broadcasterNickname || '스트리머'}
            {stream.partner && (
              <span className="stream-partner-badge" title="파트너 스트리머" aria-label="파트너 스트리머">
                ✓
              </span>
            )}
          </span>
          <span className="stream-card-game">{gameLabel(stream.game)}</span>
        </div>
      </div>
    </Link>
  );
}

export default function Streams() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [liveStreams, setLiveStreams] = useState<StreamItem[]>([]);
  const [recentStreams, setRecentStreams] = useState<StreamItem[]>([]);
  const [loading, setLoading] = useState(true);

  const section = searchParams.get('section') || '';
  const game = searchParams.get('game') || '';
  const sort = searchParams.get('sort') || '';
  const categoryFilter = (searchParams.get('categoryTab') as CategoryGroup | null) || 'ALL';
  const showCategoryHub = section === 'categories';

  useEffect(() => {
    let alive = true;
    setLoading(true);

    Promise.all([fetchLiveStreams(), fetchRecentStreams(24)])
      .then(([live, recent]) => {
        if (!alive) return;
        setLiveStreams(live);
        setRecentStreams(recent);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, []);

  const filteredCategories = useMemo(
    () => CATEGORY_CARDS.filter((card) => categoryFilter === 'ALL' || card.group === categoryFilter),
    [categoryFilter],
  );

  const filteredLive = useMemo(() => {
    const list = game ? liveStreams.filter((stream) => stream.game === game) : liveStreams;
    if (sort === 'popular') {
      return [...list].sort((a, b) => (b.viewerCount ?? 0) - (a.viewerCount ?? 0));
    }
    return list;
  }, [game, liveStreams, sort]);

  const filteredRecent = useMemo(() => {
    const list = game ? recentStreams.filter((stream) => stream.game === game) : recentStreams;
    if (sort === 'popular') {
      return [...list].sort((a, b) => (b.viewerCount ?? 0) - (a.viewerCount ?? 0));
    }
    return list;
  }, [game, recentStreams, sort]);

  if (showCategoryHub) {
    return (
      <StreamsLayout>
        <div className="streams-category-page streams-category-page-chzzk">
          <header className="streams-category-head streams-category-head-chzzk">
            <div>
              <h1 className="streams-category-title">카테고리</h1>
            </div>
            <div className="streams-category-filters" role="tablist" aria-label="카테고리 그룹">
              {CATEGORY_FILTERS.map((filter) => (
                <button
                  key={filter.key}
                  type="button"
                  className={`streams-category-pill ${categoryFilter === filter.key ? 'active' : ''}`}
                  onClick={() => {
                    const next = new URLSearchParams(searchParams);
                    next.set('section', 'categories');
                    next.set('categoryTab', filter.key);
                    setSearchParams(next);
                  }}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </header>

          <div className="streams-category-grid streams-category-grid-chzzk">
            {filteredCategories.map((card) => {
              const stats = buildCategoryStats(card, liveStreams);
              const href = card.routeGame ? `/streams?game=${encodeURIComponent(card.routeGame)}` : '/streams';
              return (
                <Link key={card.id} to={href} className={`streams-category-card streams-category-card-chzzk accent-${card.accent}`}>
                  <div className="streams-category-cover streams-category-cover-chzzk" style={card.imageUrl ? { backgroundImage: `linear-gradient(180deg, rgba(5, 8, 16, 0.10), rgba(5, 8, 16, 0.48)), url(${card.imageUrl})` } : undefined}>
                    <div className="streams-category-overlay streams-category-overlay-chzzk">
                      <span className="streams-category-viewers">{formatCompactCount(stats.viewerCount)}</span>
                    </div>
                    {!card.imageUrl && <span className="streams-category-logo streams-category-logo-chzzk">{card.title}</span>}
                  </div>
                  <div className="streams-category-copy streams-category-copy-chzzk">
                    <strong>{card.title}</strong>
                    <span>라이브 {stats.liveCount}개</span>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      </StreamsLayout>
    );
  }

  const liveHeading = game ? `${gameLabel(game)} 라이브` : '현재 라이브';
  const recentHeading = game ? `${gameLabel(game)} 최근 방송` : '최근 방송';

  return (
    <StreamsLayout>
      <div className="streams-home">
        <section className="streams-section">
          <div className="streams-section-head">
            <h1 className="streams-section-title">{liveHeading}</h1>
            <Link to="/streams?section=categories" className="streams-section-link">
              카테고리 보기
            </Link>
          </div>
          {loading ? (
            <div className="streams-empty">라이브 방송을 불러오는 중입니다.</div>
          ) : filteredLive.length === 0 ? (
            <div className="streams-empty">현재 라이브 방송이 없습니다.</div>
          ) : (
            <div className="streams-grid">
              {filteredLive.slice(0, 12).map((stream) => (
                <StreamCard key={stream.id} stream={stream} />
              ))}
            </div>
          )}
        </section>

        <section className="streams-section">
          <div className="streams-section-head">
            <h2 className="streams-section-title">{recentHeading}</h2>
            {game && (
              <Link to="/streams" className="streams-section-link">
                전체 방송으로
              </Link>
            )}
          </div>
          {loading ? (
            <div className="streams-empty">최근 방송을 불러오는 중입니다.</div>
          ) : filteredRecent.length === 0 ? (
            <div className="streams-empty">최근 방송이 없습니다.</div>
          ) : (
            <div className="streams-grid">
              {filteredRecent.slice(0, 18).map((stream) => (
                <StreamCard key={stream.id} stream={stream} />
              ))}
            </div>
          )}
        </section>
      </div>
    </StreamsLayout>
  );
}
