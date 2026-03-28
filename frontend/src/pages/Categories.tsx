import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import StreamsLayout from '../components/StreamsLayout';
import { apiUrl } from '../api/client';
import { fetchLiveStreams, type StreamItem } from '../api/streams';
import './Categories.css';

type CategoryItem = {
  key: string;
  label: string;
  game: string;
  image: string;
};

const CATEGORY_ITEMS: CategoryItem[] = [
  {
    key: 'lol',
    label: '리그 오브 레전드',
    game: 'LEAGUE_OF_LEGENDS',
    image: apiUrl('/images/league-of-legends-card.png'),
  },
  {
    key: 'valorant',
    label: '발로란트',
    game: 'VALORANT',
    image: apiUrl('/images/valorant-card.png'),
  },
  {
    key: 'overwatch',
    label: '오버워치2',
    game: 'OVERWATCH',
    image: apiUrl('/images/overwatch2-card.png'),
  },
  {
    key: 'pubg',
    label: 'PUBG',
    game: 'PUBG',
    image: apiUrl('/images/pubg-card.png'),
  },
  {
    key: 'tft',
    label: '전략적 팀 전투: 신화와 전설',
    game: 'TEAMFIGHT_TACTICS',
    image: apiUrl('/images/TFT-card.png'),
  },
  {
    key: 'others',
    label: '기타 게임',
    game: 'OTHERS',
    image: apiUrl('/images/other-games-card.png'),
  },
];

function isLive(stream: StreamItem): boolean {
  return (stream.status || '').toUpperCase() === 'LIVE' && !stream.endedAt;
}

export default function Categories() {
  const [liveStreams, setLiveStreams] = useState<StreamItem[]>([]);

  useEffect(() => {
    fetchLiveStreams().then(setLiveStreams);
    const timer = setInterval(() => fetchLiveStreams().then(setLiveStreams), 5000);
    return () => clearInterval(timer);
  }, []);

  const liveCountByGame = useMemo(() => {
    const counts = new Map<string, number>();
    liveStreams.filter(isLive).forEach((stream) => {
      const game = stream.game || 'OTHERS';
      counts.set(game, (counts.get(game) || 0) + 1);
    });
    return counts;
  }, [liveStreams]);

  return (
    <StreamsLayout>
      <div className="categories-page">
        <div className="categories-head">
          <h1 className="categories-title">카테고리</h1>
        </div>

        <div className="categories-grid">
          {CATEGORY_ITEMS.map((item) => {
            const count = liveCountByGame.get(item.game) || 0;
            return (
              <Link key={item.key} to={`/streams?game=${item.game}`} className="category-card">
                <div className="category-card-media">
                  <img src={item.image} alt={item.label} />
                  <span className="category-card-count">{count}명</span>
                </div>
                <div className="category-card-body">
                  <h2 className="category-card-title">{item.label}</h2>
                  <p className="category-card-meta">라이브 {count}개</p>
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </StreamsLayout>
  );
}
