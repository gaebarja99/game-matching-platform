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
  {
    id: 'lol',
    label: 'League of Legends',
    short: 'LoL',
    accent: '#2f80ed',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '소환사명', tag: 'KR1' },
    hint: '닉네임과 태그를 입력하면 최근 전적을 조회합니다.',
    tagLabel: '태그',
  },
  {
    id: 'tft',
    label: 'Teamfight Tactics',
    short: 'TFT',
    accent: '#7c5cff',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '소환사명', tag: 'KR1' },
    hint: 'TFT 닉네임과 태그 기준으로 최근 매치를 불러옵니다.',
    tagLabel: '태그',
  },
  {
    id: 'valorant',
    label: 'Valorant',
    short: 'VAL',
    accent: '#ff4d67',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '플레이어명', tag: 'KR1' },
    hint: '닉네임과 태그를 입력하면 계정 API 기준 전적을 조회합니다.',
    tagLabel: '태그',
  },
  {
    id: 'pubg',
    label: 'PUBG',
    short: 'PUBG',
    accent: '#f0b429',
    fields: ['nickname', 'pubg_platform'],
    placeholders: { nickname: 'Steam 또는 Kakao 닉네임' },
    hint: 'PUBG는 플랫폼을 함께 선택해야 정확한 검색이 가능합니다.',
    platformOptions: [
      { value: 'steam', label: 'Steam' },
      { value: 'kakao', label: 'Kakao' },
    ],
  },
  {
    id: 'overwatch',
    label: 'Overwatch 2',
    short: 'OW2',
    accent: '#ff9b3d',
    fields: ['nickname', 'tag'],
    placeholders: { nickname: 'BattleTag 이름', tag: '1234' },
    hint: '오버워치는 BattleTag 이름과 숫자 태그 조합으로 검색합니다.',
    tagLabel: '배틀태그',
  },
  {
    id: 'cs2',
    label: 'Counter-Strike 2',
    short: 'CS2',
    accent: '#61b15a',
    fields: ['nickname'],
    placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' },
    hint: 'CS2는 Steam 계정 기준으로 검색합니다.',
  },
];

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

  const hashInSlug = decodedSlug.indexOf('#');
  if (hashInSlug > 0 && hashInSlug < decodedSlug.length - 1) {
    return {
      nickname: decodedSlug.slice(0, hashInSlug).trim(),
      tagLine: decodedSlug.slice(hashInSlug + 1).trim(),
    };
  }

  const lastDash = decodedSlug.lastIndexOf('-');
  if (lastDash <= 0) {
    return { nickname: decodedSlug, tagLine: '' };
  }

  return {
    nickname: decodedSlug.slice(0, lastDash).trim(),
    tagLine: decodedSlug.slice(lastDash + 1).trim(),
  };
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
  if (meta?.fields.includes('pubg_platform') && platform) {
    qs.set('platform', platform);
  }
  if (meta?.fields.includes('count') && count !== 5) {
    qs.set('count', String(count));
  }
  const suffix = qs.toString() ? `?${qs.toString()}` : '';
  const nick = nickname.trim();
  if (!nick) return `/records/${gameId}${suffix}`;
  if (meta?.fields.includes('tag') && tagLine.trim()) {
    return `/records/${gameId}/${encodeURIComponent(nick)}${suffix}#${encodeURIComponent(tagLine.trim())}`;
  }
  return `/records/${gameId}/${encodeURIComponent(nick)}${suffix}`;
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
  const game = GAMES.find((item) => item.id === gameId) || GAMES[0];
  return searchPlayer({
    game: gameId,
    gameName: nickname.trim(),
    tagLine: game.fields.includes('tag') ? tagLine.trim() || undefined : undefined,
    platform: game.fields.includes('pubg_platform') ? platform : undefined,
    count: game.fields.includes('count') ? count : undefined,
    forceRefresh: forceRefresh || undefined,
  });
}

export const RECORDS_AI_MODEL_OPTIONS: { value: string; label: string }[] = [
  { value: 'gpt-5-mini', label: 'gpt-5-mini' },
  { value: 'gpt-5.2', label: 'gpt-5.2' },
  { value: 'gpt-4o-mini', label: 'gpt-4o-mini' },
  { value: 'gpt-4o', label: 'gpt-4o' },
];
