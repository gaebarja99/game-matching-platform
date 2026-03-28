import { searchPlayer, type PlayerSearchResponse } from '../../api/search';

export type GameOption = {
  id: string;
  label: string;
  short: string;
  accent: string;
  cardImage: string;
  brandLabel?: string;
  fields: string[];
  placeholders: {
    nickname: string;
    tag?: string;
  };
  hint: string;
  tagLabel?: string;
  platformOptions?: { value: string; label: string }[];
  regionOptions?: { value: string; label: string }[];
};

export type SearchFieldOverrides = {
  gameId?: string;
  nickname?: string;
  tagLine?: string;
  platform?: string;
  region?: string;
  count?: number;
};

function parseHexRgb(hex: string): { r: number; g: number; b: number } | null {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m) return null;
  const n = parseInt(m[1], 16);
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
}

/** 전적 랜딩(검색 폼·버튼) — 선택 게임 브랜드 색과 맞춤 */
export function getRecordsLandingCssVars(accent: string): Record<string, string> {
  const a = accent.trim();
  const mid = `color-mix(in srgb, ${a} 78%, #0c0c0e)`;
  const rgb = parseHexRgb(a);
  const soft = rgb ? `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.14)` : 'rgba(0, 230, 118, 0.14)';
  const glow = rgb ? `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.22)` : 'rgba(0, 230, 118, 0.22)';
  return {
    '--records-landing-accent': a,
    '--records-landing-accent-mid': mid,
    '--records-landing-accent-soft': soft,
    '--records-landing-accent-glow': glow,
  };
}

export const GAMES: GameOption[] = [
  {
    id: 'lol',
    label: 'League of Legends',
    short: 'LoL',
    accent: '#2f80ed',
    cardImage: '/images/lol-card-records.png',
    brandLabel: 'RIOT GAMES',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '소환사명', tag: 'KR1' },
    hint: '리그 오브 레전드는 닉네임과 태그 또는 서버 코드로 최근 전적을 조회합니다.',
    tagLabel: '태그/서버',
  },
  {
    id: 'tft',
    label: 'Teamfight Tactics',
    short: 'TFT',
    accent: '#7c5cff',
    cardImage: '/images/tft-card-records.png',
    brandLabel: 'RIOT GAMES',
    fields: ['nickname', 'tag', 'count'],
    placeholders: { nickname: '닉네임', tag: 'KR1' },
    hint: 'TFT는 닉네임과 태그 또는 서버 코드 기준으로 최근 매치를 불러옵니다.',
    tagLabel: '태그/서버',
  },
  {
    id: 'valorant',
    label: 'Valorant',
    short: 'VAL',
    accent: '#ff4d67',
    cardImage: '/images/valorant-card-records.png',
    brandLabel: 'VALORANT',
    fields: ['nickname', 'tag', 'count', 'region'],
    placeholders: { nickname: '플레이어명', tag: 'KR1' },
    hint: '발로란트는 닉네임, 태그, 서버를 선택해 계정 API 기준으로 최근 전적을 조회합니다.',
    tagLabel: '태그',
    regionOptions: [
      { value: 'kr', label: 'KR' },
      { value: 'ap', label: 'AP' },
      { value: 'na', label: 'NA' },
      { value: 'eu', label: 'EU' },
      { value: 'latam', label: 'LATAM' },
      { value: 'br', label: 'BR' },
    ],
  },
  {
    id: 'pubg',
    label: 'PUBG',
    short: 'PUBG',
    accent: '#f0b429',
    cardImage: '/images/pubg-card-records.png',
    brandLabel: 'BATTLEGROUNDS',
    fields: ['nickname', 'pubg_platform'],
    placeholders: { nickname: 'Steam 또는 Kakao 닉네임' },
    hint: 'PUBG는 플랫폼을 같이 선택해야 더 정확하게 검색됩니다.',
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
    cardImage: '/images/overwatch-card-records.png',
    brandLabel: 'OVERWATCH 2',
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
    cardImage: '/images/cs2-card-records.png',
    brandLabel: 'COUNTER-STRIKE 2',
    fields: ['nickname'],
    placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' },
    hint: 'CS2는 Steam 계정을 기준으로 검색합니다.',
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
  const hashTag = rawHash ? decodeURIComponent(rawHash) : '';
  if (hashTag.trim()) {
    return { nickname: decodeURIComponent(playerSlug), tagLine: hashTag.trim() };
  }

  const decodedSlug = decodeURIComponent(playerSlug);
  const hashIndex = decodedSlug.indexOf('#');
  if (hashIndex > 0 && hashIndex < decodedSlug.length - 1) {
    return {
      nickname: decodedSlug.slice(0, hashIndex).trim(),
      tagLine: decodedSlug.slice(hashIndex + 1).trim(),
    };
  }

  const dashIndex = decodedSlug.lastIndexOf('-');
  if (dashIndex <= 0) {
    return { nickname: decodedSlug, tagLine: '' };
  }

  return {
    nickname: decodedSlug.slice(0, dashIndex).trim(),
    tagLine: decodedSlug.slice(dashIndex + 1).trim(),
  };
}

export function buildRecordsProfileUrl(
  gameId: string,
  nickname: string,
  tagLine: string,
  platform: string,
  count: number,
  region?: string,
): string {
  const meta = GAMES.find((g) => g.id === gameId);
  const qs = new URLSearchParams();

  if (meta?.fields.includes('pubg_platform') && platform) {
    qs.set('platform', platform);
  }
  if (region) {
    qs.set('region', region);
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

export function parseRiotDisplayName(displayName: string | null | undefined):
  | { gameName: string; tagLine: string }
  | null {
  const text = displayName?.trim();
  if (!text) return null;
  const index = text.indexOf('#');
  if (index <= 0 || index >= text.length - 1) return null;
  return {
    gameName: text.slice(0, index).trim(),
    tagLine: text.slice(index + 1).trim(),
  };
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

export async function fetchRecordsPlayerSearch(
  gameId: string,
  nickname: string,
  tagLine: string,
  platform: string,
  count: number,
  forceRefresh: boolean,
  region?: string,
): Promise<PlayerSearchResponse> {
  const game = GAMES.find((item) => item.id === gameId) || GAMES[0];
  return searchPlayer({
    game: gameId,
    gameName: nickname.trim(),
    tagLine: game.fields.includes('tag') ? tagLine.trim() || undefined : undefined,
    platform: game.fields.includes('pubg_platform') ? platform : undefined,
    region: region || undefined,
    count: game.fields.includes('count') ? count : undefined,
    forceRefresh: forceRefresh || undefined,
    matchListOnly: gameId === 'lol' || gameId === 'tft' ? true : undefined,
    deferValorantMmr: gameId === 'valorant' ? true : undefined,
  });
}

export const RECORDS_AI_MODEL_OPTIONS: { value: string; label: string }[] = [
  { value: 'gpt-5-mini', label: 'gpt-5-mini' },
  { value: 'gpt-5.2', label: 'gpt-5.2' },
  { value: 'gpt-4o-mini', label: 'gpt-4o-mini' },
  { value: 'gpt-4o', label: 'gpt-4o' },
];
