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
  subtitle: string;
  viewersLabel: string;
  liveCountLabel: string;
  badge?: string;
  accent: string;
  imageUrl?: string;
  routeGame?: string;
};

const GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치 2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: '카운터 스트라이크 2',
  APEX_LEGENDS: '에이펙스 레전드',
  OTHERS: '기타',
};

const CATEGORY_CARDS: CategoryCard[] = [
  {
    id: 'black-desert',
    group: 'GAME',
    title: '붉은사막',
    subtitle: '액션 RPG',
    viewersLabel: '1.3만명',
    liveCountLabel: '라이브 155개',
    badge: '드롭스',
    accent: 'desert',
    imageUrl: '/images/14_11_42__5ad9768e2e94b[H800-].jpg',
    routeGame: 'OTHERS',
  },
  {
    id: 'talk',
    group: 'ENT',
    title: 'talk',
    subtitle: '토크 / 소통',
    viewersLabel: '1.2만명',
    liveCountLabel: '라이브 198개',
    accent: 'talk',
  },
  {
    id: 'league',
    group: 'GAME',
    title: '리그 오브 레전드',
    subtitle: 'MOBA',
    viewersLabel: '6,690명',
    liveCountLabel: '라이브 154개',
    accent: 'league',
    imageUrl: '/images/16bd752dfb349cacf.jpg',
    routeGame: 'LEAGUE_OF_LEGENDS',
  },
  {
    id: 'slay-the-spire-2',
    group: 'GAME',
    title: '슬레이 더 스파이어 2',
    subtitle: '전략 카드',
    viewersLabel: '2,549명',
    liveCountLabel: '라이브 34개',
    badge: 'NEW',
    accent: 'spire',
  },
  {
    id: 'lost-ark',
    group: 'GAME',
    title: '로스트아크',
    subtitle: 'MMORPG',
    viewersLabel: '2,178명',
    liveCountLabel: '라이브 45개',
    accent: 'ark',
  },
  {
    id: 'eternal-return',
    group: 'GAME',
    title: '이터널 리턴',
    subtitle: '배틀로얄',
    viewersLabel: '1,948명',
    liveCountLabel: '라이브 68개',
    accent: 'return',
  },
  {
    id: 'mlb-show',
    group: 'SPORTS',
    title: 'MLB 더 쇼 26',
    subtitle: '스포츠',
    viewersLabel: '1,773명',
    liveCountLabel: '라이브 17개',
    accent: 'show',
  },
  {
    id: 'diablo',
    group: 'GAME',
    title: '디아블로 II: 레저렉션',
    subtitle: '액션 RPG',
    viewersLabel: '1,576명',
    liveCountLabel: '라이브 18개',
    accent: 'diablo',
  },
  {
    id: 'overwatch',
    group: 'GAME',
    title: '오버워치',
    subtitle: '팀 슈터',
    viewersLabel: '1,534명',
    liveCountLabel: '라이브 72개',
    accent: 'overwatch',
    routeGame: 'OVERWATCH',
  },
  {
    id: 'rimworld',
    group: 'GAME',
    title: '림월드',
    subtitle: '시뮬레이션',
    viewersLabel: '1,404명',
    liveCountLabel: '라이브 12개',
    accent: 'rimworld',
  },
  {
    id: 'music',
    group: 'ENT',
    title: '음악 / 노래',
    subtitle: '뮤직',
    viewersLabel: '1,045명',
    liveCountLabel: '라이브 49개',
    accent: 'music',
  },
  {
    id: 'lobotomy',
    group: 'GAME',
    title: '로보토미 코퍼레이션',
    subtitle: '경영 / 전략',
    viewersLabel: '1,004명',
    liveCountLabel: '라이브 2개',
    accent: 'lobotomy',
  },
  {
    id: 'wow',
    group: 'GAME',
    title: '월드 오브 워크래프트',
    subtitle: 'MMORPG',
    viewersLabel: '875명',
    liveCountLabel: '라이브 41개',
    badge: '드롭스',
    accent: 'wow',
  },
  {
    id: 'aion2',
    group: 'GAME',
    title: '아이온2',
    subtitle: 'MMORPG',
    viewersLabel: '777명',
    liveCountLabel: '라이브 34개',
    accent: 'aion',
  },
  {
    id: 'wuthering-waves',
    group: 'GAME',
    title: '명조:워더링 웨이브',
    subtitle: '오픈월드 액션',
    viewersLabel: '714명',
    liveCountLabel: '라이브 11개',
    accent: 'waves',
  },
  {
    id: 'path-of-exile',
    group: 'GAME',
    title: '패스 오브 엑자일',
    subtitle: '핵앤슬래시',
    viewersLabel: '601명',
    liveCountLabel: '라이브 9개',
    accent: 'poe',
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

function categoryImageStyle(card: CategoryCard) {
  if (card.imageUrl) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(7, 10, 18, 0.08), rgba(7, 10, 18, 0.7)), url(${card.imageUrl})`,
    };
  }
  return undefined;
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
        {isLive ? (
          <span className="stream-card-live-badge">LIVE</span>
        ) : (
          <span className="stream-card-ended-badge">종료</span>
        )}
        {stream.viewerCount != null && (
          <span className="stream-card-viewers">{formatViewerCount(stream.viewerCount)}</span>
        )}
      </div>
      <div className="stream-card-info">
        <div className="stream-card-avatar">
          {profileImage ? <img src={profileImage} alt="" /> : <span>{(stream.broadcasterNickname ?? 'G')[0]}</span>}
        </div>
        <div className="stream-card-meta">
          <strong className="stream-card-title">{stream.title || '방송 제목 없음'}</strong>
          <span className="stream-card-broadcaster">{stream.broadcasterNickname || '스트리머'}</span>
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
        <div className="streams-category-page">
          <header className="streams-category-head">
            <div>
              <p className="streams-category-kicker">GameMatcher Categories</p>
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

          <div className="streams-category-grid">
            {filteredCategories.map((card) => (
              <Link
                key={card.id}
                to={card.routeGame ? `/streams?game=${encodeURIComponent(card.routeGame)}` : '/streams'}
                className={`streams-category-card accent-${card.accent}`}
              >
                <div className="streams-category-cover" style={categoryImageStyle(card)}>
                  <div className="streams-category-overlay">
                    <span className="streams-category-viewers">{card.viewersLabel}</span>
                    {card.badge && <span className="streams-category-badge">{card.badge}</span>}
                  </div>
                  {!card.imageUrl && <span className="streams-category-logo">{card.title}</span>}
                </div>
                <div className="streams-category-copy">
                  <strong>{card.title}</strong>
                  <span>{card.liveCountLabel}</span>
                  <small>{card.subtitle}</small>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </StreamsLayout>
    );
  }

  const liveHeading = game ? `${gameLabel(game)} 라이브` : '지금 라이브';
  const recentHeading = game ? `${gameLabel(game)} 최근 방송` : '최근 방송';

  return (
    <StreamsLayout>
      <div className="streams-home">
        <section className="streams-section">
          <div className="streams-section-head">
            <h1 className="streams-section-title">🎵 {liveHeading}</h1>
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
            <h2 className="streams-section-title">📼 {recentHeading}</h2>
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
