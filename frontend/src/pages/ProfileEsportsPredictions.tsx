import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  formatPredictionTime,
  getPredictionStorageKey,
  predictionMatches,
  type PredictionMatch,
  type StoredPrediction,
} from '../data/esportsPredictions';

function loadPredictions(storageKey: string) {
  try {
    const raw = localStorage.getItem(storageKey);
    if (!raw) return [] as StoredPrediction[];
    const parsed = JSON.parse(raw) as StoredPrediction[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function getStatusLabel(match: PredictionMatch) {
  return match.status === 'FINAL' ? '결과 확정' : '예측 가능';
}

export default function ProfileEsportsPredictions() {
  const { user } = useAuth();
  const storageKey = getPredictionStorageKey(user?.id);
  const [savedPredictions, setSavedPredictions] = useState<StoredPrediction[]>([]);

  useEffect(() => {
    setSavedPredictions(loadPredictions(storageKey));
  }, [storageKey]);

  const predictionMap = useMemo(
    () => new Map(savedPredictions.map((prediction) => [prediction.matchId, prediction])),
    [savedPredictions],
  );

  const matchGroups = useMemo(() => {
    const upcoming = predictionMatches.filter((match) => match.status === 'UPCOMING');
    const finished = predictionMatches.filter((match) => match.status === 'FINAL');
    return { upcoming, finished };
  }, []);

  const totalResolved = matchGroups.finished.length;
  const selectedResolved = matchGroups.finished.filter((match) => predictionMap.has(match.id)).length;
  const streakLabel = savedPredictions[0]?.selectedTeamName ?? '아직 선택 없음';

  const handlePick = (matchId: string, teamCode: string, teamName: string) => {
    const next = savedPredictions.filter((prediction) => prediction.matchId !== matchId);
    next.unshift({
      matchId,
      selectedTeamCode: teamCode,
      selectedTeamName: teamName,
      savedAt: new Date().toISOString(),
    });
    setSavedPredictions(next);
    localStorage.setItem(storageKey, JSON.stringify(next));
  };

  const clearPick = (matchId: string) => {
    const next = savedPredictions.filter((prediction) => prediction.matchId !== matchId);
    setSavedPredictions(next);
    localStorage.setItem(storageKey, JSON.stringify(next));
  };

  return (
    <div className="profile-esports-page esports-predictions-page">
      <section className="profile-esports-hero">
        <div className="profile-esports-hero-copy">
          <span className="profile-esports-eyebrow">Prediction Lounge</span>
          <h1>승부 예측</h1>
          <p>다가오는 빅매치를 골라두고, 결과가 확정되면 당첨 보관함에서 적중 여부를 바로 확인하세요.</p>
          <div className="profile-esports-hero-actions">
            <Link to="/profile/esports-rewards" className="prediction-primary-link">
              당첨 보관함 보기
            </Link>
            <span className="profile-esports-hero-note">최근 선택: {streakLabel}</span>
          </div>
        </div>

        <div className="profile-esports-hero-panel">
          <div className="profile-esports-hero-stat">
            <span>총 선택</span>
            <strong>{savedPredictions.length}</strong>
            <em>지금까지 저장한 픽</em>
          </div>
          <div className="profile-esports-hero-stat">
            <span>예측 대기</span>
            <strong>{matchGroups.upcoming.length}</strong>
            <em>오늘 고를 수 있는 경기</em>
          </div>
          <div className="profile-esports-hero-stat">
            <span>결과 확인</span>
            <strong>
              {selectedResolved}/{totalResolved}
            </strong>
            <em>종료 경기 중 내 선택 기록</em>
          </div>
        </div>
      </section>

      <section className="profile-esports-section">
        <div className="profile-esports-section-head">
          <div>
            <span className="profile-esports-section-kicker">Live Board</span>
            <h2>예측 가능한 경기</h2>
          </div>
          <span className="profile-esports-section-meta">{matchGroups.upcoming.length} matches open</span>
        </div>

        <div className="prediction-match-list">
          {matchGroups.upcoming.map((match) => {
            const saved = predictionMap.get(match.id);
            return (
              <article key={match.id} className="prediction-match-card prediction-match-card-upcoming">
                <div className="prediction-card-topline">
                  <span className="prediction-league-pill">{match.league}</span>
                  <span className="prediction-status-chip">{getStatusLabel(match)}</span>
                </div>

                <div className="prediction-card-head">
                  <div>
                    <h2>{match.stage}</h2>
                    <p>{formatPredictionTime(match.scheduledAt)}</p>
                  </div>
                  {saved ? <span className="prediction-picked-badge">내 픽 저장됨</span> : null}
                </div>

                <div className="prediction-teams">
                  <button
                    type="button"
                    className={`prediction-team-button ${saved?.selectedTeamCode === match.leftCode ? 'is-selected' : ''}`}
                    onClick={() => handlePick(match.id, match.leftCode, match.leftTeam)}
                  >
                    <span className="prediction-team-code">{match.leftCode}</span>
                    <strong>{match.leftTeam}</strong>
                    <small>왼쪽 팀 선택</small>
                  </button>

                  <span className="prediction-vs">VS</span>

                  <button
                    type="button"
                    className={`prediction-team-button ${saved?.selectedTeamCode === match.rightCode ? 'is-selected' : ''}`}
                    onClick={() => handlePick(match.id, match.rightCode, match.rightTeam)}
                  >
                    <span className="prediction-team-code">{match.rightCode}</span>
                    <strong>{match.rightTeam}</strong>
                    <small>오른쪽 팀 선택</small>
                  </button>
                </div>

                <div className="prediction-card-footer">
                  <div className="prediction-saved-text">
                    {saved ? (
                      <>
                        선택 완료 <strong>{saved.selectedTeamName}</strong>
                      </>
                    ) : (
                      '아직 선택하지 않았습니다.'
                    )}
                  </div>

                  <div className="prediction-actions">
                    {saved ? (
                      <button type="button" className="prediction-secondary-btn" onClick={() => clearPick(match.id)}>
                        선택 취소
                      </button>
                    ) : null}
                    <Link to="/profile/esports-rewards" className="prediction-inline-link">
                      결과 보관함
                    </Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <section className="profile-esports-section profile-esports-section-muted">
        <div className="profile-esports-section-head">
          <div>
            <span className="profile-esports-section-kicker">Archive</span>
            <h2>최근 종료 경기</h2>
          </div>
          <span className="profile-esports-section-meta">{matchGroups.finished.length} matches closed</span>
        </div>

        <div className="prediction-finished-grid">
          {matchGroups.finished.map((match) => {
            const saved = predictionMap.get(match.id);
            const winnerName = match.winnerCode === match.leftCode ? match.leftTeam : match.rightTeam;
            const hit = saved?.selectedTeamCode && saved.selectedTeamCode === match.winnerCode;
            return (
              <article key={match.id} className={`prediction-finished-card ${hit ? 'is-hit' : saved ? 'is-miss' : ''}`}>
                <div className="prediction-card-topline">
                  <span className="prediction-league-pill">{match.league}</span>
                  <span className="prediction-status-chip is-final">종료</span>
                </div>
                <h3>{match.stage}</h3>
                <p className="prediction-finished-matchup">
                  {match.leftTeam} <span>{match.leftScore ?? '-'}</span> : <span>{match.rightScore ?? '-'}</span> {match.rightTeam}
                </p>
                <div className="prediction-finished-meta">
                  <span>승자</span>
                  <strong>{winnerName}</strong>
                </div>
                <div className="prediction-finished-meta">
                  <span>내 선택</span>
                  <strong>{saved?.selectedTeamName ?? '선택 안 함'}</strong>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}
