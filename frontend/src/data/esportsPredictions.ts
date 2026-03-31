export type PredictionStatus = 'UPCOMING' | 'FINAL';

export interface PredictionMatch {
  id: string;
  league: string;
  stage: string;
  scheduledAt: string;
  leftTeam: string;
  rightTeam: string;
  leftCode: string;
  rightCode: string;
  leftScore?: number;
  rightScore?: number;
  winnerCode?: string;
  status: PredictionStatus;
}

export interface StoredPrediction {
  matchId: string;
  selectedTeamCode: string;
  selectedTeamName: string;
  savedAt: string;
}

export const predictionMatches: PredictionMatch[] = [
  {
    id: 'pred-lck-1',
    league: 'LCK',
    stage: '정규 시즌 1R',
    scheduledAt: '2026-03-24T17:00:00+09:00',
    leftTeam: 'T1',
    rightTeam: 'Gen.G',
    leftCode: 'T1',
    rightCode: 'GEN',
    status: 'UPCOMING',
  },
  {
    id: 'pred-lck-2',
    league: 'LCK',
    stage: '정규 시즌 1R',
    scheduledAt: '2026-03-24T19:30:00+09:00',
    leftTeam: 'Hanwha Life Esports',
    rightTeam: 'KT Rolster',
    leftCode: 'HLE',
    rightCode: 'KT',
    status: 'UPCOMING',
  },
  {
    id: 'pred-fs-1',
    league: '퍼스트 스탠드',
    stage: 'Grand Final',
    scheduledAt: '2026-03-16T17:00:00+09:00',
    leftTeam: 'Hanwha Life Esports',
    rightTeam: 'Karmine Corp',
    leftCode: 'HLE',
    rightCode: 'KC',
    leftScore: 3,
    rightScore: 1,
    winnerCode: 'HLE',
    status: 'FINAL',
  },
  {
    id: 'pred-fs-2',
    league: '퍼스트 스탠드',
    stage: 'Semifinal',
    scheduledAt: '2026-03-15T20:30:00+09:00',
    leftTeam: 'Karmine Corp',
    rightTeam: 'CTBC Flying Oyster',
    leftCode: 'KC',
    rightCode: 'CFO',
    leftScore: 3,
    rightScore: 2,
    winnerCode: 'KC',
    status: 'FINAL',
  },
  {
    id: 'pred-lec-1',
    league: 'LEC',
    stage: 'Regular Season',
    scheduledAt: '2026-03-22T21:00:00+09:00',
    leftTeam: 'Fnatic',
    rightTeam: 'G2 Esports',
    leftCode: 'FNC',
    rightCode: 'G2',
    leftScore: 1,
    rightScore: 2,
    winnerCode: 'G2',
    status: 'FINAL',
  },
];

export function getPredictionStorageKey(userId?: number | string | null) {
  return `gamematcher-esports-predictions:${userId ?? 'guest'}`;
}

export function formatPredictionTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('ko-KR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  } catch {
    return iso;
  }
}
