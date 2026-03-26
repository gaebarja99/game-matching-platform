import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { useAuth } from '../contexts/AuthContext';
import { getMyMatchSessions, type MatchSessionListItem } from '../api/match';

const GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: 'CS2',
};

function formatHistoryTime(createdAt: string): string {
  try {
    return new Date(createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: true });
  } catch {
    return '';
  }
}

export default function MatchHistory() {
  const { user } = useAuth();
  const [list, setList] = useState<MatchSessionListItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getMyMatchSessions()
      .then(setList)
      .finally(() => setLoading(false));
  }, [user]);

  if (!user) {
    return (
      <Layout>
        <div className="duo-section">
          <p>로그인하면 매칭 채팅 이력을 볼 수 있습니다.</p>
          <Link to="/login" className="btn-write">로그인</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <section className="duo-section">
        <div className="duo-section-header">
          <h2 className="duo-section-title">랜덤 매칭 채팅 이력</h2>
          <Link to="/" className="link-all">메인으로</Link>
        </div>
        <p className="duo-stats">매칭 후 나눈 채팅방을 다시 볼 수 있습니다.</p>
        {loading ? (
          <div className="duo-empty">로딩 중...</div>
        ) : list.length === 0 ? (
          <div className="duo-empty">아직 매칭 이력이 없습니다. 메인에서 랜덤 매칭을 시도해 보세요.</div>
        ) : (
          <ul className="match-history-list">
            {list.map((s) => (
              <li key={s.id} className="match-history-item">
                <Link to={`/match-chat/${s.id}`} className="match-history-link">
                  <div className="match-history-avatar">
                    <span className="match-history-avatar-initial">{(GAME_LABELS[s.game] ?? s.game)[0]}</span>
                  </div>
                  <div className="match-history-content">
                    <span className="match-history-name">{GAME_LABELS[s.game] ?? s.game}</span>
                    <span className="match-history-preview">채팅 보기</span>
                  </div>
                  <span className="match-history-time">{formatHistoryTime(s.createdAt)}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </Layout>
  );
}
