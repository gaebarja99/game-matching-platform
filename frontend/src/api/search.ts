import { apiFetch, decodeApiTextNewlines, normalizeRateLimitUserMessage } from './client';

function normalizePlayerFacingErrorMessage(msg: string | undefined): string | undefined {
  if (msg === undefined) return msg;
  let m = decodeApiTextNewlines(msg);
  m = normalizeRateLimitUserMessage(m) ?? m;
  return m;
}

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
  /** true면 DB 캐시를 쓰지 않고 API로 다시 받아 갱신 */
  forceRefresh?: boolean;
  /** true면 매치 ID 목록만 (상세는 match-detail API, LoL/TFT) */
  matchListOnly?: boolean;
  /** 발로란트: 티어는 /api/search/valorant/mmr 로 이어서 로드 */
  deferValorantMmr?: boolean;
  /** 계정 존재만 확인(발로 1차 프로브 등). 매치·MMR 제외 */
  accountOnly?: boolean;
  valorantPrefetchPuuid?: string;
  valorantPrefetchAccountRegion?: string;
  valorantPrefetchCardUrl?: string;
}

export interface PlayerSearchResponse {
  success: boolean;
  errorMessage?: string;
  game?: string;
  nickname?: string;
  /** 매치 행에 KDA 등 요약이 없고 matchId만 있을 때 */
  matchListOnly?: boolean;
  valorantMmrPending?: boolean;
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
    extras?: Record<string, unknown> & { listOnly?: boolean };
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
    avgDamage?: number;
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

  const data = response.data;
  if (!data.success && data.errorMessage) {
    const em = normalizePlayerFacingErrorMessage(data.errorMessage);
    return em === data.errorMessage ? data : { ...data, errorMessage: em };
  }
  return data;
}

export interface MatchDetailResponse {
  success: boolean;
  errorMessage?: string;
  game?: string;
  matchId?: string;
  payload?: Record<string, unknown>;
}

/** Records 매치 행 펼침 시 상세 데이터 로드 (발로/LoL/TFT/PUBG), 서버에서 DB 저장 후 payload 반환 */
export async function fetchValorantSearchMmr(request: {
  puuid: string;
  region: string;
  forceRefresh?: boolean;
}): Promise<{ success: boolean; errorMessage?: string; tierDisplay?: string }> {
  const response = await apiFetch<{ success: boolean; errorMessage?: string; tierDisplay?: string }>(
    '/api/search/valorant/mmr',
    {
      method: 'POST',
      body: JSON.stringify({
        puuid: request.puuid,
        region: request.region,
        forceRefresh: request.forceRefresh || undefined,
      }),
    },
  );
  if (!response.ok || !response.data) {
    throw new Error(response.message ?? 'MMR 요청에 실패했습니다.');
  }
  const data = response.data;
  if (!data.success && data.errorMessage) {
    const em = normalizePlayerFacingErrorMessage(data.errorMessage);
    return em === data.errorMessage ? data : { ...data, errorMessage: em };
  }
  return data;
}

export async function fetchMatchDetail(request: {
  game: string;
  matchId: string;
  region?: string;
  platform?: string;
  puuid?: string;
  /** 발로란트: 저장된 AI 분석 중 이 모델 행만 붙임. 생략 시 서버 기본 모델 */
  llmModel?: string;
  forceRefresh?: boolean;
}): Promise<MatchDetailResponse> {
  const response = await apiFetch<MatchDetailResponse>('/api/search/match-detail', {
    method: 'POST',
    body: JSON.stringify(request),
  });

  if (!response.ok || !response.data) {
    throw new Error(response.message ?? '매치 상세 요청에 실패했습니다.');
  }

  const data = response.data;
  if (!data.success && data.errorMessage) {
    const em = normalizePlayerFacingErrorMessage(data.errorMessage);
    return em === data.errorMessage ? data : { ...data, errorMessage: em };
  }
  return data;
}

/** DB에 저장된 AI 평가만 조회 (매치 상세·외부 전적 API 재호출 없음). 모델/게임 전환 시 사용. */
export async function fetchSavedAiEvaluation(request: {
  game: string;
  matchId: string;
  puuid?: string;
  playerName?: string;
  llmModel?: string;
}): Promise<Record<string, unknown> | null> {
  const q = new URLSearchParams({
    game: request.game,
    matchId: request.matchId,
  });
  if (request.puuid) q.set('puuid', request.puuid);
  if (request.playerName) q.set('playerName', request.playerName);
  if (request.llmModel) q.set('llmModel', request.llmModel);
  const response = await apiFetch<{ success?: boolean; records_ai_evaluation?: Record<string, unknown> }>(
    `/api/search/saved-ai-evaluation?${q}`,
  );
  if (!response.ok || !response.data) {
    return null;
  }
  const ev = response.data.records_ai_evaluation;
  return ev && typeof ev === 'object' ? ev : null;
}

/** POST /api/valorant/evaluations/match/{matchId} 응답 행 */
export interface ValorantAiEvaluationApiRow {
  matchId?: string;
  playerPuuid?: string;
  llmModel?: string | null;
  summary?: string | null;
  detailedComment?: string | null;
  score?: number | null;
  grade?: string | null;
  status?: string | null;
}

/**
 * 발로란트 매치 AI 평가 실행·DB 저장 후 결과 목록 반환.
 * `force: true`면 기존 평가가 있어도 선택한 모델로 다시 호출한다.
 */
export async function runValorantMatchAiEvaluation(request: {
  matchId: string;
  puuid?: string;
  model?: string;
  force?: boolean;
}): Promise<ValorantAiEvaluationApiRow[]> {
  const q = new URLSearchParams();
  if (request.puuid) q.set('puuid', request.puuid);
  if (request.model) q.set('model', request.model);
  if (request.force) q.set('force', 'true');
  const qs = q.toString();
  const path = `/api/valorant/evaluations/match/${encodeURIComponent(request.matchId)}${qs ? `?${qs}` : ''}`;
  const response = await apiFetch<ValorantAiEvaluationApiRow[]>(path, { method: 'POST' });
  if (!response.ok || !response.data) {
    throw new Error(response.message ?? 'AI 분석 요청에 실패했습니다.');
  }
  return response.data;
}

export interface RecordsAiEvaluationApiRow {
  matchId?: string;
  playerKey?: string;
  llmModel?: string | null;
  status?: string | null;
  grade?: string | null;
  score?: number | null;
  summary?: string | null;
  detailedComment?: string | null;
}

export async function runLolMatchAiEvaluation(request: {
  matchId: string;
  puuid: string;
  model?: string;
}): Promise<RecordsAiEvaluationApiRow> {
  const q = new URLSearchParams();
  q.set('puuid', request.puuid);
  if (request.model) q.set('model', request.model);
  const path = `/api/search/lol/evaluations/match/${encodeURIComponent(request.matchId)}?${q.toString()}`;
  const response = await apiFetch<RecordsAiEvaluationApiRow>(path, { method: 'POST' });
  if (!response.ok || !response.data) {
    throw new Error(response.message ?? 'LoL AI 분석 요청에 실패했습니다.');
  }
  return response.data;
}

export async function runPubgMatchAiEvaluation(request: {
  matchId: string;
  playerName?: string;
  accountId?: string;
}): Promise<RecordsAiEvaluationApiRow[]> {
  const q = new URLSearchParams();
  if (request.playerName) q.set('playerName', request.playerName);
  if (request.accountId) q.set('accountId', request.accountId);
  const qs = q.toString();
  const path = `/api/pubg/evaluations/match/${encodeURIComponent(request.matchId)}${qs ? `?${qs}` : ''}`;
  const response = await apiFetch<
    Array<{
      matchId?: string;
      accountId?: string;
      playerName?: string;
      summary?: string | null;
      detailedComment?: string | null;
    }>
  >(path, { method: 'POST' });
  if (!response.ok || !response.data) {
    throw new Error(response.message ?? 'PUBG AI 분석 요청에 실패했습니다.');
  }
  return response.data.map((row) => ({
    matchId: row.matchId,
    playerKey: row.accountId ?? row.playerName,
    llmModel: null,
    status: 'COMPLETED',
    grade: null,
    score: null,
    summary: row.summary ?? null,
    detailedComment: row.detailedComment ?? null,
  }));
}
