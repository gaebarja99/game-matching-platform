import { apiFetch } from './client';

export interface PlayerSearchRequest {
  game: string;
  gameName?: string;
  tagLine?: string;
  nickname?: string;
  steamId?: string;
  platform?: string;
  region?: string;
  count?: number;
  queueType?: number;
}

export interface PlayerSearchResponse {
  success: boolean;
  errorMessage?: string;
  game?: string;
  nickname?: string;
  playerInfo?: {
    puuid?: string;
    gameName?: string;
    tagLine?: string;
    summonerLevel?: string;
    profileIconId?: string;
    tier?: string;
    rank?: string;
    lp?: string;
    steamId?: string;
    avatarUrl?: string;
    rawData?: Record<string, unknown>;
  };
  matches?: Array<{
    matchId?: string;
    gameMode?: string;
    champion?: string;
    agent?: string;
    win?: boolean;
    kills?: number;
    deaths?: number;
    assists?: number;
    kda?: number;
    cs?: number;
    playtime?: number;
    playedAt?: string;
    extras?: Record<string, unknown>;
  }>;
  stats?: {
    totalGames?: number;
    wins?: number;
    losses?: number;
    winRate?: number;
    avgKills?: number;
    avgDeaths?: number;
    avgAssists?: number;
    avgKda?: number;
    mostUsedChampionOrAgent?: string;
  };
}

export async function searchPlayer(request: PlayerSearchRequest): Promise<PlayerSearchResponse> {
  const response = await apiFetch<PlayerSearchResponse>('/api/search/player', {
    method: 'POST',
    body: JSON.stringify(request),
  });

  if (!response.ok || !response.data) {
    throw new Error(response.message ?? '전적 검색 요청에 실패했습니다.');
  }

  return response.data;
}
