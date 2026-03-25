/** Shared random-match form options and labels (used by Home + RandomMatchChatModal). */

export const TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'IRON', label: '아이언' },
  { value: 'BRONZE', label: '브론즈' },
  { value: 'SILVER', label: '실버' },
  { value: 'GOLD', label: '골드' },
  { value: 'PLATINUM', label: '플래티넘' },
  { value: 'EMERALD', label: '에메랄드' },
  { value: 'DIAMOND', label: '다이아' },
  { value: 'MASTER', label: '마스터' },
];

export const PUBG_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'BRONZE', label: '브론즈' },
  { value: 'SILVER', label: '실버' },
  { value: 'GOLD', label: '골드' },
  { value: 'PLATINUM', label: '플래티넘' },
  { value: 'CRYSTAL', label: '크리스탈' },
  { value: 'DIAMOND', label: '다이아' },
  { value: 'MASTER', label: '마스터' },
  { value: 'SURVIVOR', label: '서바이버' },
];

export const VALORANT_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'IRON', label: '아이언' },
  { value: 'BRONZE', label: '브론즈' },
  { value: 'SILVER', label: '실버' },
  { value: 'GOLD', label: '골드' },
  { value: 'PLATINUM', label: '플래티넘' },
  { value: 'DIAMOND', label: '다이아몬드' },
  { value: 'ASCENDANT', label: '초월자' },
  { value: 'IMMORTAL', label: '불멸' },
  { value: 'RADIANT', label: '레디언트' },
];

export const RANK_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'SOLO', label: '솔로 랭크' },
  { value: 'FLEX', label: '자유 랭크' },
  { value: 'QUICK', label: '빠른 대전' },
  { value: 'ARAM', label: '칼바람 나락' },
];

export const VALORANT_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'COMPETITIVE', label: '경쟁전' },
  { value: 'UNRATED', label: '일반전' },
  { value: 'SPIKE_RUSH', label: '스파이크 돌격' },
];

export const OVERWATCH_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'ROLE_QUEUE_COMP', label: '역할 고정 - 경쟁전' },
  { value: 'QUICK_PLAY', label: '빠른 대전' },
  { value: 'OPEN_QUEUE', label: '자유 모드' },
];

export const PUBG_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'NORMAL', label: '일반전' },
  { value: 'RANKED', label: '경쟁전' },
];

export const PUBG_PLATFORM_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'STEAM', label: '스팀' },
  { value: 'KAKAO', label: '카카오' },
];

export const APEX_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'BATTLE_ROYALE_RANKED', label: '배틀로얄(랭크)' },
  { value: 'BATTLE_ROYALE', label: '배틀로얄' },
  { value: 'ARENAS', label: '아레나' },
];

export const PUBG_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'DUO', label: '듀오(2인)' },
  { value: 'SQUAD', label: '스쿼드(4인)' },
];

export const APEX_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'DUO', label: '듀오(2인)' },
  { value: 'SQUAD', label: '스쿼드(3인)' },
];

export const LEAGUE_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'DUO', label: '듀오(2인)' },
  { value: '3', label: '3인' },
  { value: '4', label: '4인' },
  { value: '5', label: '5인' },
];

export const VALORANT_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '5', label: '5인(스탠다드)' },
];

export const CS2_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '2', label: '2인' },
  { value: '5', label: '5인' },
];

export const OVERWATCH_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '2', label: '2인' },
  { value: '6', label: '6인(고정)' },
];

export const GAME_OPTIONS: { key: string; label: string }[] = [
  { key: 'LEAGUE_OF_LEGENDS', label: '리그 오브 레전드' },
  { key: 'VALORANT', label: '발로란트' },
  { key: 'OVERWATCH', label: '오버워치2' },
  { key: 'PUBG', label: 'PUBG' },
  { key: 'COUNTER_STRIKE_2', label: 'CS2' },
  { key: 'APEX_LEGENDS', label: '에이펙스' },
];

export const MATCH_GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: 'CS2',
  APEX_LEGENDS: '에이펙스',
};

export function getSearchModeOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS': return RANK_OPTIONS;
    case 'VALORANT': return VALORANT_MODE_OPTIONS;
    case 'OVERWATCH': return OVERWATCH_MODE_OPTIONS;
    case 'PUBG': return [{ value: '', label: '전체' }, ...PUBG_MODE_OPTIONS.filter((o) => o.value).map((o) => ({ value: o.value, label: o.label }))];
    case 'APEX_LEGENDS': return APEX_MODE_OPTIONS;
    case 'COUNTER_STRIKE_2': return [{ value: '', label: '전체' }];
    default: return RANK_OPTIONS;
  }
}

export function getMatchModeOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS': return RANK_OPTIONS;
    case 'VALORANT': return VALORANT_MODE_OPTIONS;
    case 'OVERWATCH': return OVERWATCH_MODE_OPTIONS;
    case 'PUBG': return PUBG_MODE_OPTIONS;
    case 'APEX_LEGENDS': return APEX_MODE_OPTIONS;
    default: return [];
  }
}

export function getMatchPartyOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS': return LEAGUE_PARTY_OPTIONS;
    case 'VALORANT': return VALORANT_PARTY_OPTIONS;
    case 'OVERWATCH': return OVERWATCH_PARTY_OPTIONS;
    case 'PUBG': return PUBG_PARTY_OPTIONS;
    case 'COUNTER_STRIKE_2': return CS2_PARTY_OPTIONS;
    case 'APEX_LEGENDS': return APEX_PARTY_OPTIONS;
    default: return [];
  }
}

export function tierLabel(v: string, gameKey?: string): string {
  const opts =
    gameKey === 'PUBG' ? PUBG_TIER_OPTIONS
    : gameKey === 'VALORANT' ? VALORANT_TIER_OPTIONS
    : TIER_OPTIONS;
  return opts.find((o) => o.value === v)?.label ?? '티어 전체';
}

export function tierOptionsForGame(gameKey: string) {
  return gameKey === 'PUBG' ? PUBG_TIER_OPTIONS
    : gameKey === 'VALORANT' ? VALORANT_TIER_OPTIONS
    : TIER_OPTIONS;
}

export function rankLabel(v: string): string {
  return RANK_OPTIONS.find((o) => o.value === v)?.label ?? '전체';
}

export function modeLabel(gameKey: string, v: string): string {
  if (!v) return '전체';
  if (gameKey === 'VALORANT') return VALORANT_MODE_OPTIONS.find((o) => o.value === v)?.label ?? v;
  if (gameKey === 'OVERWATCH') return OVERWATCH_MODE_OPTIONS.find((o) => o.value === v)?.label ?? v;
  if (gameKey === 'APEX_LEGENDS') return APEX_MODE_OPTIONS.find((o) => o.value === v)?.label ?? v;
  if (gameKey === 'PUBG') return PUBG_MODE_OPTIONS.find((o) => o.value === v)?.label ?? v;
  return v;
}

export function modeHasNoTier(gameKey: string, mode: string | undefined): boolean {
  if (!mode) return false;
  if (gameKey === 'LEAGUE_OF_LEGENDS' && (mode === 'QUICK' || mode === 'ARAM')) return true;
  if (gameKey === 'VALORANT' && (mode === 'UNRATED' || mode === 'SPIKE_RUSH')) return true;
  if (gameKey === 'OVERWATCH' && (mode === 'QUICK_PLAY' || mode === 'OPEN_QUEUE')) return true;
  if (gameKey === 'PUBG' && mode === 'NORMAL') return true;
  if (gameKey === 'APEX_LEGENDS' && (mode === 'BATTLE_ROYALE' || mode === 'ARENAS')) return true;
  return false;
}

export function createFormShowTier(game: string, rankOrMode: string): boolean {
  return !modeHasNoTier(game, rankOrMode || undefined);
}

export function isLolSoloRank(game: string, mode: string): boolean {
  return game === 'LEAGUE_OF_LEGENDS' && mode === 'SOLO';
}

export function isLolFlexRank(game: string, mode: string): boolean {
  return game === 'LEAGUE_OF_LEGENDS' && mode === 'FLEX';
}

export function isLolAram(game: string, mode: string): boolean {
  return game === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM';
}

export function positionRequiredForRandomMatch(game: string, mode: string): boolean {
  if (game === 'PUBG') return false;
  if (isLolAram(game, mode)) return false;
  return true;
}

export function getControlledPartyOptions(game: string, mode: string): { value: string; label: string }[] {
  const base = getMatchPartyOptions(game);
  if (base.length === 0) return base;
  if (isLolSoloRank(game, mode)) return [{ value: '1', label: '1명' }];
  if (isLolFlexRank(game, mode)) return base.filter((o) => o.value !== '4');
  return base;
}

export function showPositionForRoom(gameKey: string, mode: string | undefined): boolean {
  if (gameKey === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM') return false;
  return true;
}

export function partySizeLabel(value: string | undefined, roomGame: string): string {
  if (!value) return '-';
  const options =
    roomGame === 'PUBG' ? PUBG_PARTY_OPTIONS
    : roomGame === 'APEX_LEGENDS' ? APEX_PARTY_OPTIONS
    : roomGame === 'LEAGUE_OF_LEGENDS' ? LEAGUE_PARTY_OPTIONS
    : roomGame === 'VALORANT' ? VALORANT_PARTY_OPTIONS
    : roomGame === 'COUNTER_STRIKE_2' ? CS2_PARTY_OPTIONS
    : roomGame === 'OVERWATCH' ? OVERWATCH_PARTY_OPTIONS
    : [];
  return options.find((o) => o.value === value)?.label ?? value;
}

export function describeRandomMatchSummary(
  game: string,
  mode: string,
  tier: string,
  position: string | null,
  partySize: string
): { label: string; value: string }[] {
  const gameLabel = GAME_OPTIONS.find((g) => g.key === game)?.label ?? game;
  const rows: { label: string; value: string }[] = [{ label: '게임', value: gameLabel }];
  const modeOpts = getMatchModeOptions(game);
  if (modeOpts.length > 0 && mode) {
    rows.push({ label: '모드', value: modeLabel(game, mode) });
  }
  if (getMatchPartyOptions(game).length > 0 && partySize) {
    rows.push({ label: '인원', value: partySizeLabel(partySize, game) });
  }
  if (createFormShowTier(game, mode) && tier) {
    rows.push({ label: '티어', value: tierLabel(tier, game) });
  }
  if (game !== 'PUBG' && !(game === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM') && position) {
    rows.push({ label: '포지션', value: position });
  }
  return rows;
}
