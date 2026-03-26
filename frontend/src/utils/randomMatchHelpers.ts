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
  { value: 'QUICK', label: '신속 대전' },
];

export const LOL_QUEUE_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: 'FLEX', label: '자유 랭크' },
  { value: 'QUICK', label: '신속 대전' },
];

export const VALORANT_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: 'COMPETITIVE', label: '경쟁전' },
  { value: 'UNRATED', label: '일반전' },
  { value: 'SPIKE_RUSH', label: '스파이크 돌격' },
];

export const OVERWATCH_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: 'ROLE_QUEUE_COMP', label: '역할 고정 - 경쟁전' },
  { value: 'OPEN_QUEUE_COMP', label: '자유 - 경쟁전' },
  { value: 'QUICK_PLAY', label: '빠른 대전' },
];

export const PUBG_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: 'NORMAL', label: '일반전' },
  { value: 'RANKED', label: '경쟁전' },
];

export const PUBG_PLATFORM_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'STEAM', label: '스팀' },
  { value: 'KAKAO', label: '카카오' },
];

export const PUBG_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: 'DUO', label: '듀오(2인)' },
  { value: 'SQUAD', label: '스쿼드(4인)' },
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
  { value: '5', label: '5인 파티' },
];

export const CS2_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '2', label: '2인' },
  { value: '5', label: '5인' },
];

export const OVERWATCH_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '2', label: '2인' },
  { value: '6', label: '6인 고정' },
];

export const GAME_OPTIONS: { key: string; label: string }[] = [
  { key: 'LEAGUE_OF_LEGENDS', label: '리그 오브 레전드' },
  { key: 'VALORANT', label: '발로란트' },
  { key: 'OVERWATCH', label: '오버워치2' },
  { key: 'PUBG', label: 'PUBG' },
  { key: 'COUNTER_STRIKE_2', label: 'CS2' },
];

export const MATCH_GAME_LABELS: Record<string, string> = {
  LEAGUE_OF_LEGENDS: '리그 오브 레전드',
  VALORANT: '발로란트',
  OVERWATCH: '오버워치2',
  PUBG: 'PUBG',
  COUNTER_STRIKE_2: 'CS2',
};

export function getSearchModeOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS':
      return RANK_OPTIONS;
    case 'VALORANT':
      return VALORANT_MODE_OPTIONS;
    case 'OVERWATCH':
      return OVERWATCH_MODE_OPTIONS;
    case 'PUBG':
      return [{ value: '', label: '전체' }, ...PUBG_MODE_OPTIONS.map((option) => ({ value: option.value, label: option.label }))];
    case 'COUNTER_STRIKE_2':
      return [{ value: '', label: '전체' }];
    default:
      return RANK_OPTIONS;
  }
}

export function getMatchModeOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS':
      return LOL_QUEUE_TYPE_OPTIONS;
    case 'VALORANT':
      return VALORANT_MODE_OPTIONS;
    case 'OVERWATCH':
      return OVERWATCH_MODE_OPTIONS;
    case 'PUBG':
      return PUBG_MODE_OPTIONS;
    default:
      return [];
  }
}

export function getMatchPartyOptions(game: string): { value: string; label: string }[] {
  switch (game) {
    case 'LEAGUE_OF_LEGENDS':
      return LEAGUE_PARTY_OPTIONS;
    case 'VALORANT':
      return VALORANT_PARTY_OPTIONS;
    case 'OVERWATCH':
      return OVERWATCH_PARTY_OPTIONS;
    case 'PUBG':
      return PUBG_PARTY_OPTIONS;
    case 'COUNTER_STRIKE_2':
      return CS2_PARTY_OPTIONS;
    default:
      return [];
  }
}

export function tierLabel(value: string, gameKey?: string): string {
  const options =
    gameKey === 'PUBG'
      ? PUBG_TIER_OPTIONS
      : gameKey === 'VALORANT'
        ? VALORANT_TIER_OPTIONS
        : TIER_OPTIONS;
  return options.find((option) => option.value === value)?.label ?? '티어 전체';
}

export function tierOptionsForGame(gameKey: string) {
  return gameKey === 'PUBG'
    ? PUBG_TIER_OPTIONS
    : gameKey === 'VALORANT'
      ? VALORANT_TIER_OPTIONS
      : TIER_OPTIONS;
}

export function rankLabel(value: string): string {
  return RANK_OPTIONS.find((option) => option.value === value)?.label ?? '전체';
}

export function modeLabel(gameKey: string, value: string): string {
  if (!value) return '전체';
  if (gameKey === 'VALORANT') return VALORANT_MODE_OPTIONS.find((option) => option.value === value)?.label ?? value;
  if (gameKey === 'OVERWATCH') return OVERWATCH_MODE_OPTIONS.find((option) => option.value === value)?.label ?? value;
  if (gameKey === 'PUBG') return PUBG_MODE_OPTIONS.find((option) => option.value === value)?.label ?? value;
  return value;
}

export function modeHasNoTier(gameKey: string, mode: string | undefined): boolean {
  if (gameKey === 'VALORANT') return true;
  if (gameKey === 'PUBG') return true;
  if (gameKey === 'COUNTER_STRIKE_2') return true;
  if (!mode) return false;
  if (gameKey === 'LEAGUE_OF_LEGENDS' && (mode === 'SOLO' || mode === 'FLEX' || mode === 'QUICK')) return true;
  if (gameKey === 'OVERWATCH') return mode !== 'OPEN_QUEUE_COMP';
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

export function isLolAram(_game: string, _mode: string): boolean {
  return false;
}

export function positionRequiredForRandomMatch(game: string, mode: string): boolean {
  if (game === 'PUBG') return false;
  if (isLolAram(game, mode)) return false;
  return true;
}

export function getControlledPartyOptions(game: string, mode: string): { value: string; label: string }[] {
  const base = getMatchPartyOptions(game);
  if (base.length === 0) return base;
  if (isLolSoloRank(game, mode)) return [{ value: '1', label: '1인' }];
  if (isLolFlexRank(game, mode)) return base.filter((option) => option.value !== '4');
  return base;
}

export function showPositionForRoom(gameKey: string, mode: string | undefined): boolean {
  if (gameKey === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM') return false;
  return true;
}

export function partySizeLabel(value: string | undefined, roomGame: string): string {
  if (!value) return '-';
  const options =
    roomGame === 'PUBG'
      ? PUBG_PARTY_OPTIONS
      : roomGame === 'LEAGUE_OF_LEGENDS'
        ? LEAGUE_PARTY_OPTIONS
        : roomGame === 'VALORANT'
          ? VALORANT_PARTY_OPTIONS
          : roomGame === 'COUNTER_STRIKE_2'
            ? CS2_PARTY_OPTIONS
            : roomGame === 'OVERWATCH'
              ? OVERWATCH_PARTY_OPTIONS
              : [];
  return options.find((option) => option.value === value)?.label ?? value;
}

export function describeRandomMatchSummary(
  game: string,
  mode: string,
  tier: string,
  position: string | null,
  partySize: string,
): { label: string; value: string }[] {
  const gameLabel = GAME_OPTIONS.find((item) => item.key === game)?.label ?? game;
  const rows: { label: string; value: string }[] = [{ label: '게임', value: gameLabel }];
  const modeOptions = getMatchModeOptions(game);

  if (modeOptions.length > 0 && mode) {
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
