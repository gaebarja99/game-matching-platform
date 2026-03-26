import { searchPlayer, type PlayerSearchResponse } from '../../api/search';

export type GameOption = {
  id: string;
  label: string;
  short: string;
  accent: string;
  fields: string[];
  placeholders: {
    nickname: string;
    tag?: string;
  };
  hint: string;
  tagLabel?: string;
  platformOptions?: { value: string; label: string }[];
};

export const GAMES: GameOption[] = [
  { id: 'lol', label: 'League of Legends', short: 'LoL', accent: '#2f80ed', fields: ['nickname', 'tag', 'count'], placeholders: { nickname: '소환사명', tag: 'KR1' }, hint: '라이엇 계정 기준으로 닉네임과 태그를 입력하면 최근 전적을 조회합니다.', tagLabel: '태그' },
  { id: 'tft', label: 'Teamfight Tactics', short: 'TFT', accent: '#7c5cff', fields: ['nickname', 'tag', 'count'], placeholders: { nickname: '닉네임', tag: 'KR1' }, hint: 'TFT도 닉네임과 태그 기준으로 최근 매치를 불러옵니다.', tagLabel: '태그' },
  { id: 'valorant', label: 'Valorant', short: 'VAL', accent: '#ff4d67', fields: ['nickname', 'tag', 'count'], placeholders: { nickname: '플레이어명', tag: 'KR1' }, hint: '닉네임과 태그를 입력하면 계정 API 기준 샤드로 최근 전적을 조회합니다.', tagLabel: '태그' },
  { id: 'pubg', label: 'PUBG', short: 'PUBG', accent: '#f0b429', fields: ['nickname', 'pubg_platform'], placeholders: { nickname: 'Steam 또는 Kakao 닉네임' }, hint: 'PUBG는 플랫폼을 같이 선택해야 정확히 검색됩니다.', platformOptions: [{ value: 'steam', label: 'Steam' }, { value: 'kakao', label: 'Kakao' }] },
  { id: 'overwatch', label: 'Overwatch 2', short: 'OW2', accent: '#ff9b3d', fields: ['nickname', 'tag'], placeholders: { nickname: 'BattleTag 이름', tag: '1234' }, hint: '오버워치는 BattleTag 이름과 숫자 태그 조합으로 검색합니다.', tagLabel: '배틀태그' },
  { id: 'apex', label: 'Apex Legends', short: 'APEX', accent: '#ff6f61', fields: ['nickname', 'apex_platform'], placeholders: { nickname: 'EA 또는 Origin 닉네임' }, hint: '에이펙스는 플랫폼에 따라 검색 결과가 달라질 수 있습니다.', platformOptions: [{ value: 'PC', label: 'PC' }, { value: 'PS4', label: 'PlayStation' }, { value: 'X1', label: 'Xbox' }] },
  { id: 'cs2', label: 'Counter-Strike 2', short: 'CS2', accent: '#61b15a', fields: ['nickname'], placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' }, hint: 'CS2는 Steam 식별자를 기준으로 검색합니다.' },
];

/**
 * 전적 프로필 URL 파싱.
 * - 태그 필요 게임: 우선 `location.hash`의 `#태그`(라이엇 표기와 동일), 없으면 예전 경로 `닉네임-태그` 호환.
 */
export function parseProfileSlug(
  gameId: string,
  playerSlug: string,
  urlHash?: string,
): { nickname: string; tagLine: string } {
  const meta = GAMES.find((g) => g.id === gameId);
  if (!meta?.fields.includes('tag')) {
    try {
      return { nickname: decodeURIComponent(playerSlug), tagLine: '' };
    } catch {
      return { nickname: playerSlug, tagLine: '' };
    }
  }
  const rawHash = urlHash?.startsWith('#') ? urlHash.slice(1) : (urlHash ?? '');
  let tagFromHash = '';
  if (rawHash) {
    try {
      tagFromHash = decodeURIComponent(rawHash);
    } catch {
      tagFromHash = rawHash;
    }
  }
  if (tagFromHash.trim()) {
    try {
      return { nickname: decodeURIComponent(playerSlug), tagLine: tagFromHash.trim() };
    } catch {
      return { nickname: playerSlug, tagLine: tagFromHash.trim() };
    }
  }
  let decodedSlug = playerSlug;
  try {
    decodedSlug = decodeURIComponent(playerSlug);
  } catch {
    decodedSlug = playerSlug;
  }
  // 경로에 `#` 또는 `%23` 이 포함된 경우 (브라우저 해시와 별개로 slug 한 덩어리로 올 때)
  const hashInSlug = decodedSlug.indexOf('#');
  if (hashInSlug > 0 && hashInSlug < decodedSlug.length - 1) {
    return {
      nickname: decodedSlug.slice(0, hashInSlug).trim(),
      tagLine: decodedSlug.slice(hashInSlug + 1).trim(),
    };
  }
  const last = decodedSlug.lastIndexOf('-');
  if (last <= 0) {
    return { nickname: decodedSlug, tagLine: '' };
  }
  const a = decodedSlug.slice(0, last).trim();
  const b = decodedSlug.slice(last + 1).trim();
  return { nickname: a, tagLine: b };
}

export function buildRecordsProfileUrl(
  gameId: string,
  nickname: string,
  tagLine: string,
  platform: string,
  count: number,
): string {
  const meta = GAMES.find((g) => g.id === gameId);
  const qs = new URLSearchParams();
  if (meta && (meta.fields.includes('pubg_platform') || meta.fields.includes('apex_platform')) && platform) {
    qs.set('platform', platform);
  }
  if (meta?.fields.includes('count') && count !== 5) {
    qs.set('count', String(count));
  }
  const q = qs.toString();
  const suffix = q ? `?${q}` : '';
  const nick = nickname.trim();
  if (!nick) {
    return `/records/${gameId}${suffix}`;
  }
  if (meta?.fields.includes('tag') && tagLine.trim()) {
    const base = `/records/${gameId}/${encodeURIComponent(nick)}${suffix}`;
    return `${base}#${encodeURIComponent(tagLine.trim())}`;
  }
  return `/records/${gameId}/${encodeURIComponent(nick)}${suffix}`;
}

/** Riot 연동 표시명 `게임명#태그` → 전적 검색용 */
export function parseRiotDisplayName(displayName: string | null | undefined): {
  gameName: string;
  tagLine: string;
} | null {
  const t = displayName?.trim();
  if (!t) return null;
  const i = t.indexOf('#');
  if (i <= 0 || i >= t.length - 1) return null;
  const gameName = t.slice(0, i).trim();
  const tagLine = t.slice(i + 1).trim();
  if (!gameName || !tagLine) return null;
  return { gameName, tagLine };
}

export const GAMES_WITH_MATCH_DETAIL = new Set(['valorant', 'lol', 'tft', 'pubg']);

export function formatWinRate(value: number | null | undefined) {
  if (value == null) return '-';
  const normalized = value > 1 ? value : value * 100;
  return `${normalized.toFixed(0)}%`;
}

export function getInitials(name?: string | null) {
  if (!name) return '?';
  return name.slice(0, 1).toUpperCase();
}

export type SearchFieldOverrides = {
  gameId?: string;
  nickname?: string;
  tagLine?: string;
  platform?: string;
  count?: number;
};

export async function fetchRecordsPlayerSearch(
  gameId: string,
  nickname: string,
  tagLine: string,
  platform: string,
  count: number,
  forceRefresh: boolean,
): Promise<PlayerSearchResponse> {
  const g = GAMES.find((item) => item.id === gameId) || GAMES[0];
  return searchPlayer({
    game: gameId,
    gameName: nickname.trim(),
    tagLine: g.fields.includes('tag') ? tagLine.trim() || undefined : undefined,
    platform: g.fields.includes('pubg_platform') || g.fields.includes('apex_platform') ? platform : undefined,
    count: g.fields.includes('count') ? count : undefined,
    forceRefresh: forceRefresh || undefined,
  });
}

/** 전적 상세 AI 분석용 OpenAI 모델 ID (백엔드 `ai.llm.model`과 동일 계열) */
export const RECORDS_AI_MODEL_OPTIONS: { value: string; label: string }[] = [
  { value: 'gpt-5-mini', label: 'gpt-5-mini' },
  { value: 'gpt-5.2', label: 'gpt-5.2' },
  { value: 'gpt-4o-mini', label: 'gpt-4o-mini' },
  { value: 'gpt-4o', label: 'gpt-4o' },
];
