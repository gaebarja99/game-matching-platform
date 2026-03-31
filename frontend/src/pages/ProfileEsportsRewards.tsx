import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  formatPredictionTime,
  getPredictionStorageKey,
  predictionMatches,
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

export default function ProfileEsportsRewards() {
  const { user } = useAuth();
  const storageKey = getPredictionStorageKey(user?.id);
  const [savedPredictions, setSavedPredictions] = useState<StoredPrediction[]>([]);

  useEffect(() => {
    setSavedPredictions(loadPredictions(storageKey));
  }, [storageKey]);

  const rewardRows = useMemo(() => {
    return savedPredictions
      .map((saved) => {
        const match = predictionMatches.find((item) => item.id === saved.matchId);
        if (!match) return null;
        const isResolved = match.status === 'FINAL' && !!match.winnerCode;
        const isHit = isResolved && match.winnerCode === saved.selectedTeamCode;
        return { saved, match, isResolved, isHit };
      })
      .filter(Boolean) as Array<{
      saved: StoredPrediction;
      match: (typeof predictionMatches)[number];
      isResolved: boolean;
      isHit: boolean;
    }>;
  }, [savedPredictions]);

  const hitCount = rewardRows.filter((row) => row.isHit).length;
  const missCount = rewardRows.filter((row) => row.isResolved && !row.isHit).length;
  const pendingCount = rewardRows.filter((row) => !row.isResolved).length;
  const hitRate = rewardRows.length > 0 ? Math.round((hitCount / rewardRows.length) * 100) : 0;

  return (
    <div className="profile-esports-page esports-rewards-page">
      <section className="profile-esports-hero rewards-hero">
        <div className="profile-esports-hero-copy">
          <span className="profile-esports-eyebrow">Result Vault</span>
          <h1>당첨 보관함</h1>
          <p>내가 선택한 경기의 적중 여부를 모아보고, 어떤 리그에서 강한지 한 화면에서 확인하세요.</p>
          <div className="profile-esports-hero-actions">
            <Link to="/profile/esports-predictions" className="prediction-primary-link">
              예측하러 가기
            </Link>
            <span className="profile-esports-hero-note">적중률 {hitRate}%</span>
          </div>
        </div>

        <div className="profile-esports-hero-panel">
          <div className="profile-esports-hero-stat is-hit">
            <span>적중</span>
            <strong>{hitCount}</strong>
            <em>정답을 맞춘 경기</em>
          </div>
          <div className="profile-esports-hero-stat is-miss">
            <span>미적중</span>
            <strong>{missCount}</strong>
            <em>선택과 결과가 달랐던 경기</em>
          </div>
          <div className="profile-esports-hero-stat">
            <span>대기 중</span>
            <strong>{pendingCount}</strong>
            <em>아직 결과가 안 나온 경기</em>
          </div>
        </div>
      </section>

      {rewardRows.length === 0 ? (
        <div className="prediction-empty prediction-empty-rich">
          <strong>아직 보관된 예측이 없습니다.</strong>
          <p>첫 승부 예측을 저장하면 결과가 여기에 카드 형태로 쌓입니다.</p>
          <Link to="/profile/esports-predictions" className="prediction-primary-link">
            승부 예측 시작하기
          </Link>
        </div>
      ) : (
        <>
          <section className="profile-esports-section">
            <div className="profile-esports-section-head">
              <div>
                <span className="profile-esports-section-kicker">Overview</span>
                <h2>결과 요약</h2>
              </div>
              <span className="profile-esports-section-meta">{rewardRows.length} picks tracked</span>
            </div>

            <div className="reward-overview-grid">
              <article className="reward-overview-card">
                <span>적중률</span>
                <strong>{hitRate}%</strong>
                <p>현재까지 저장된 예측 기준 누적 성과입니다.</p>
              </article>
              <article className="reward-overview-card">
                <span>최근 적중</span>
                <strong>{rewardRows.find((row) => row.isHit)?.match.stage ?? '아직 없음'}</strong>
                <p>마지막으로 맞춘 매치 스테이지를 표시합니다.</p>
              </article>
              <article className="reward-overview-card">
                <span>결과 대기</span>
                <strong>{pendingCount}경기</strong>
                <p>종료 후 자동으로 적중 여부를 다시 확인할 수 있습니다.</p>
              </article>
            </div>
          </section>

          <section className="profile-esports-section profile-esports-section-muted">
            <div className="profile-esports-section-head">
              <div>
                <span className="profile-esports-section-kicker">Vault Cards</span>
                <h2>보관된 경기 결과</h2>
              </div>
              <span className="profile-esports-section-meta">Newest first</span>
            </div>

            <div className="reward-list">
              {rewardRows.map(({ saved, match, isResolved, isHit }) => (
                <article key={`${saved.matchId}-${saved.savedAt}`} className={`reward-card ${isResolved ? (isHit ? 'is-hit' : 'is-miss') : 'is-pending'}`}>
                  <div className="reward-card-head">
                    <div>
                      <div className="prediction-league-pill">{match.league}</div>
                      <h2>{match.stage}</h2>
                      <p>{formatPredictionTime(match.scheduledAt)}</p>
                    </div>
                    <span className={`prediction-status-chip ${isResolved ? (isHit ? 'is-hit' : 'is-miss') : ''}`}>
                      {isResolved ? (isHit ? '적중' : '미적중') : '결과 대기'}
                    </span>
                  </div>

                  <div className="reward-card-scoreline">
                    <div className="reward-team-block">
                      <span>{match.leftCode}</span>
                      <strong>{match.leftTeam}</strong>
                    </div>
                    <div className="reward-score-badge">
                      {typeof match.leftScore === 'number' && typeof match.rightScore === 'number'
                        ? `${match.leftScore} : ${match.rightScore}`
                        : 'TBD'}
                    </div>
                    <div className="reward-team-block">
                      <span>{match.rightCode}</span>
                      <strong>{match.rightTeam}</strong>
                    </div>
                  </div>

                  <div className="reward-detail-grid">
                    <div className="reward-row">
                      <span>내 선택</span>
                      <strong>{saved.selectedTeamName}</strong>
                    </div>
                    <div className="reward-row">
                      <span>실제 승자</span>
                      <strong>
                        {match.winnerCode
                          ? match.winnerCode === match.leftCode
                            ? match.leftTeam
                            : match.rightTeam
                          : '결과 집계 중'}
                      </strong>
                    </div>
                    <div className="reward-row">
                      <span>판정</span>
                      <strong>{isResolved ? (isHit ? '성공' : '실패') : '대기'}</strong>
                    </div>
                    <div className="reward-row">
                      <span>저장 시각</span>
                      <strong>{formatPredictionTime(saved.savedAt)}</strong>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
