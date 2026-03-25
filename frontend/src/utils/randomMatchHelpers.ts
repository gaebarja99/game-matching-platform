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

/** 방 만들기·목록 표시용 (구 데이터 호환: 크리스탈/서바이버 라벨 유지) */
const PUBG_LEGACY_TIER_LABELS: Record<string, string> = {
  CRYSTAL: '크리스탈',
  SURVIVOR: '서바이버',
};

/** PUBG 경쟁전 랭크(아이언~마스터) */
export const PUBG_RANKED_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'IRON', label: '아이언' },
  { value: 'BRONZE', label: '브론즈' },
  { value: 'SILVER', label: '실버' },
  { value: 'GOLD', label: '골드' },
  { value: 'PLATINUM', label: '플래티넘' },
  { value: 'DIAMOND', label: '다이아' },
  { value: 'MASTER', label: '마스터' },
];

export const PUBG_TIER_OPTIONS: { value: string; label: string }[] = PUBG_RANKED_TIER_OPTIONS;

export const PUBG_PERSPECTIVE_OPTIONS: { value: string; label: string }[] = [
  { value: 'TPP', label: '3인칭 (TPP)' },
  { value: 'FPP', label: '1인칭 (FPP)' },
];

/** PUBG 단순 4인 큐 — 선호 맵 칩 */
export const PUBG_RANDOM_MAP_CHIPS: { value: string; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'ERANGEL', label: '에란겔' },
  { value: 'MIRAMAR', label: '미라마' },
  { value: 'SANHOK', label: '사녹' },
  { value: 'VIKENDI', label: '비켄디' },
  { value: 'KARAKIN', label: '카라킨' },
  { value: 'TAEGO', label: '태이고' },
  { value: 'DESTON', label: '데스턴' },
  { value: 'RONDO', label: '론도' },
];

export function pubgPreferredMapLabel(value: string | null | undefined): string {
  if (value == null || value === '' || value === 'ALL') return '전체';
  const chip = PUBG_RANDOM_MAP_CHIPS.find((c) => c.value === value);
  return chip?.label ?? value;
}

export const PUBG_PLATFORM_RADIO: { value: 'STEAM' | 'KAKAO'; label: string }[] = [
  { value: 'STEAM', label: '스팀' },
  { value: 'KAKAO', label: '카카오' },
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

export const OVERWATCH_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'BRONZE', label: '브론즈' },
  { value: 'SILVER', label: '실버' },
  { value: 'GOLD', label: '골드' },
  { value: 'PLATINUM', label: '플래티넘' },
  { value: 'DIAMOND', label: '다이아' },
  { value: 'MASTER', label: '마스터' },
  { value: 'GRANDMASTER', label: '그랜드마스터' },
  { value: 'TOP500', label: '톱500' },
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
  { value: 'SOLO', label: '솔로' },
  { value: 'DUO', label: '듀오(2인)' },
  { value: 'SQUAD', label: '스쿼드(4인)' },
  { value: 'ONE_MAN_SQUAD', label: '1인 스쿼드' },
];

export const APEX_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'DUO', label: '듀오(2인)' },
  { value: 'SQUAD', label: '스쿼드(3인)' },
];

export const LEAGUE_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '1', label: '1인' },
  { value: 'DUO', label: '2인' },
  { value: '3', label: '3인' },
  { value: '4', label: '4인' },
  { value: '5', label: '5인' },
];

export const VALORANT_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '1', label: '1인' },
  { value: '2', label: '2인' },
  { value: '3', label: '3인' },
  { value: '4', label: '4인' },
  { value: '5', label: '5인(스탠다드)' },
];

/** CS2 랜덤 매칭·방 만들기 공통 인원 (프리미어/경쟁 최대 5인, 윙맨은 2인 고정) */
export const CS2_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '1', label: '1인' },
  { value: '2', label: '2인' },
  { value: '3', label: '3인' },
  { value: '4', label: '4인' },
  { value: '5', label: '5인' },
];

export const CS2_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: 'PREMIER', label: '프리미어' },
  { value: 'COMPETITIVE', label: '경쟁' },
  { value: 'WINGMAN', label: '윙맨' },
  { value: 'CASUAL_DM', label: '캐주얼/데스매치' },
];

/** 프리미어 평점 구간 (CS2 Premier rating) */
export const CS2_PREMIER_RATING_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'PR_0_4999', label: '0 – 4,999' },
  { value: 'PR_5000_9999', label: '5,000 – 9,999' },
  { value: 'PR_10000_14999', label: '10,000 – 14,999' },
  { value: 'PR_15000_19999', label: '15,000 – 19,999' },
  { value: 'PR_20000_24999', label: '20,000 – 24,999' },
  { value: 'PR_25000_29999', label: '25,000 – 29,999' },
  { value: 'PR_30000_PLUS', label: '30,000+' },
];

/** 경쟁전 클래식 스킬 그룹 */
export const CS2_COMPETITIVE_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'S1', label: '실버 I' },
  { value: 'S2', label: '실버 II' },
  { value: 'GN1', label: '골드 노바 I' },
  { value: 'GN2', label: '골드 노바 II' },
  { value: 'GNM', label: '골드 노바 마스터' },
  { value: 'MG1', label: '마스터 가디언 I' },
  { value: 'MG2', label: '마스터 가디언 II' },
  { value: 'DMG', label: '저명한 마스터 가디언' },
  { value: 'LE', label: '전설의 독수리' },
  { value: 'SMFC', label: '슈프림 마스터 퍼스트 클래스' },
  { value: 'GE', label: '글로벌 엘리트' },
];

/** 윙맨 전용 랭크 필터 */
export const CS2_WINGMAN_TIER_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '티어 전체' },
  { value: 'WM_SILVER', label: '실버' },
  { value: 'WM_GOLD', label: '골드' },
  { value: 'WM_NOVA', label: '노바' },
  { value: 'WM_MG', label: '마스터 가디언' },
  { value: 'WM_EAGLE', label: '이글' },
  { value: 'WM_GLOBAL', label: '글로벌' },
];

export const OVERWATCH_PARTY_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '선택' },
  { value: '1', label: '1인' },
  { value: '2', label: '2인' },
  { value: '3', label: '3인' },
  { value: '4', label: '4인' },
  { value: '5', label: '5인(풀파티)' },
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

/** 랜덤 매칭·홈 방 만들기 사이드바에 노출할 게임 (에이펙스 제외) */
export const RANDOM_MATCH_AND_ROOM_GAME_OPTIONS = GAME_OPTIONS.filter((g) => g.key !== 'APEX_LEGENDS');

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
    case 'COUNTER_STRIKE_2': return [{ value: '', label: '전체' }, ...CS2_MODE_OPTIONS.filter((o) => o.value).map((o) => ({ value: o.value, label: o.label }))];
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
    case 'COUNTER_STRIKE_2': return CS2_MODE_OPTIONS;
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

export function tierLabel(v: string, gameKey?: string, mode?: string): string {
  if (gameKey === 'PUBG') {
    const fromList = PUBG_RANKED_TIER_OPTIONS.find((o) => o.value === v)?.label;
    if (fromList) return fromList;
    if (v && PUBG_LEGACY_TIER_LABELS[v]) return PUBG_LEGACY_TIER_LABELS[v];
    return '티어 전체';
  }
  if (gameKey === 'COUNTER_STRIKE_2' && v) {
    const premier = CS2_PREMIER_RATING_OPTIONS.find((o) => o.value === v)?.label;
    if (premier) return premier;
    const comp = CS2_COMPETITIVE_TIER_OPTIONS.find((o) => o.value === v)?.label;
    if (comp) return comp;
    const wing = CS2_WINGMAN_TIER_OPTIONS.find((o) => o.value === v)?.label;
    if (wing) return wing;
  }
  const opts =
    gameKey === 'VALORANT' ? VALORANT_TIER_OPTIONS
    : gameKey === 'OVERWATCH' ? OVERWATCH_TIER_OPTIONS
    : TIER_OPTIONS;
  return opts.find((o) => o.value === v)?.label ?? '티어 전체';
}

export function tierOptionsForGame(gameKey: string, mode?: string) {
  if (gameKey === 'PUBG') {
    if (mode === 'RANKED') return PUBG_RANKED_TIER_OPTIONS;
    return [{ value: '', label: '무관' }];
  }
  if (gameKey === 'COUNTER_STRIKE_2') {
    if (mode === 'PREMIER') return CS2_PREMIER_RATING_OPTIONS;
    if (mode === 'COMPETITIVE') return CS2_COMPETITIVE_TIER_OPTIONS;
    if (mode === 'WINGMAN') return CS2_WINGMAN_TIER_OPTIONS;
    if (mode === 'CASUAL_DM') return [{ value: '', label: '무관' }];
    return [{ value: '', label: '티어 전체' }];
  }
  return gameKey === 'VALORANT' ? VALORANT_TIER_OPTIONS
    : gameKey === 'OVERWATCH' ? OVERWATCH_TIER_OPTIONS
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
  if (gameKey === 'COUNTER_STRIKE_2') return CS2_MODE_OPTIONS.find((o) => o.value === v)?.label ?? v;
  return v;
}

export function modeHasNoTier(gameKey: string, mode: string | undefined): boolean {
  if (!mode) return gameKey === 'OVERWATCH' || gameKey === 'COUNTER_STRIKE_2';
  if (gameKey === 'LEAGUE_OF_LEGENDS' && (mode === 'QUICK' || mode === 'ARAM')) return true;
  if (gameKey === 'VALORANT' && (mode === 'UNRATED' || mode === 'SPIKE_RUSH')) return true;
  if (gameKey === 'OVERWATCH' && mode !== 'ROLE_QUEUE_COMP') return true;
  if (gameKey === 'PUBG' && mode === 'NORMAL') return true;
  if (gameKey === 'COUNTER_STRIKE_2' && mode === 'CASUAL_DM') return true;
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

/** LoL·발로·오버워치2·CS2: 모드·티어·인원 없이 5인 FIFO 단순 큐 */
export function isSimpleFivePersonRandomMatch(game: string): boolean {
  return (
    game === 'LEAGUE_OF_LEGENDS' ||
    game === 'VALORANT' ||
    game === 'OVERWATCH' ||
    game === 'COUNTER_STRIKE_2'
  );
}

/** PUBG: 플랫폼별 4인 FIFO, 모드·인원 없음 */
export function isSimplePubgRandomMatch(game: string): boolean {
  return game === 'PUBG';
}

export function positionRequiredForRandomMatch(game: string, mode: string): boolean {
  if (game === 'PUBG') return false;
  if (game === 'COUNTER_STRIKE_2' && mode === 'CASUAL_DM') return false;
  if (isLolAram(game, mode)) return false;
  if (game === 'OVERWATCH') return mode !== 'OPEN_QUEUE';
  return true;
}

export type ControlledPartyOption = { value: string; label: string; disabled?: boolean };

export function getControlledPartyOptions(game: string, mode: string, tier?: string): ControlledPartyOption[] {
  const base = getMatchPartyOptions(game);
  if (base.length === 0) return base;
  if (isLolSoloRank(game, mode)) return [{ value: '1', label: '1명' }];
  if (isLolFlexRank(game, mode)) return base.filter((o) => o.value !== '4');

  if (game === 'VALORANT') {
    const competitive = mode === 'COMPETITIVE';
    const tierRestricted = tier === 'IMMORTAL' || tier === 'RADIANT';

    return base.map((o) => {
      if (!competitive) {
        return { ...o, disabled: false };
      }

        // 발로란트 경쟁전: 4인 큐 불가 + 인원 선택 필요(선택 비활성)
        if (o.value === '' || o.value === '4') return { ...o, disabled: true };

        // 발로란트 경쟁전: 불멸 이상은 1~2인만 허용(요청사항 기준)
      if (tierRestricted) {
        const allowed = o.value === '1' || o.value === '2';
        return { ...o, disabled: !allowed };
      }

      return { ...o, disabled: false };
    });
  }

  if (game === 'OVERWATCH') {
    const openQueue = mode === 'OPEN_QUEUE';
    const competitiveRoleQueue = mode === 'ROLE_QUEUE_COMP';
    const gmPlus = tier === 'GRANDMASTER' || tier === 'TOP500';

    const maxAllowedInRoleQueue = competitiveRoleQueue ? (gmPlus ? 2 : 5) : 2; // role queue quick play (no tier restriction)

    return base.map((o) => {
      // 선택 값은 항상 허용(시작 버튼 유효성 검사에서 추가로 제어)
      if (o.value === '') return { ...o, disabled: false };

      // 6인 고정은 오픈 큐에서만 허용
      if (o.value === '6') return { ...o, disabled: !openQueue };

      if (openQueue) {
        // 오픈 큐는 포지션 선택 비활성화, 인원은 1~5 중심
        const n = Number(o.value);
        const disabled = Number.isNaN(n) ? false : n > 6;
        return { ...o, disabled };
      }

      // 역할 고정 모드(경쟁/빠른 대전): 최대 인원 제한
      const n = Number(o.value);
      const disabled = Number.isNaN(n) ? false : n > maxAllowedInRoleQueue;
      return { ...o, disabled };
    });
  }

  if (game === 'PUBG') {
    const isRanked = mode === 'RANKED';
    const isNormal = mode === 'NORMAL';
    return base.map((o) => {
      if (o.value === '') return { ...o, disabled: false };
      if (!mode) return { ...o, disabled: true };
      if (isRanked) {
        const allowed = o.value === 'SOLO' || o.value === 'SQUAD';
        return { ...o, disabled: !allowed };
      }
      if (isNormal) return { ...o, disabled: false };
      return { ...o, disabled: true };
    });
  }

  if (game === 'COUNTER_STRIKE_2') {
    if (mode === 'WINGMAN') {
      return base.map((o) => ({
        ...o,
        disabled: o.value !== '' && o.value !== '2',
      }));
    }
    if (mode === 'PREMIER' || mode === 'COMPETITIVE') {
      return base.map((o) => {
        if (o.value === '') return { ...o, disabled: false };
        const n = Number(o.value);
        if (Number.isNaN(n)) return { ...o, disabled: false };
        return { ...o, disabled: n > 5 };
      });
    }
    if (mode === 'CASUAL_DM') {
      return base.map((o) => ({ ...o, disabled: false }));
    }
    return base.map((o) => ({ ...o, disabled: o.value !== '' }));
  }

  return base;
}

export function showPositionForRoom(gameKey: string, mode: string | undefined): boolean {
  if (gameKey === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM') return false;
  if (gameKey === 'COUNTER_STRIKE_2' && mode === 'CASUAL_DM') return false;
  return true;
}

/** PUBG 랜덤 매칭 대기열 메타(서버 position 컬럼, 32자 이하) */
export function encodePubgQueueMeta(params: {
  platform: string;
  mode: string;
  perspective?: string;
  partySize?: string;
}): string {
  const p = params.platform === 'KAKAO' ? 'K' : 'S';
  const m = params.mode === 'RANKED' ? 'R' : 'N';
  const pv = params.mode === 'NORMAL' ? (params.perspective === 'FPP' ? 'FP' : 'TP') : '-';
  const ps =
    params.partySize === 'SOLO' ? 'SO'
    : params.partySize === 'DUO' ? 'D2'
    : params.partySize === 'SQUAD' ? 'S4'
    : params.partySize === 'ONE_MAN_SQUAD' ? '1S'
    : '';
  return `pg:${p}:${m}:${pv}:${ps}`;
}

export function pubgPlatformLabel(v: string): string {
  return v === 'KAKAO' ? '카카오' : v === 'STEAM' ? '스팀' : v;
}

export function pubgPerspectiveLabel(v: string): string {
  return v === 'FPP' ? '1인칭 (FPP)' : v === 'TPP' ? '3인칭 (TPP)' : v;
}

export function getPubgRandomMatchRuleError(input: {
  mode: string;
  platform: string;
  perspective: string;
  partySize: string;
  /** 현재 인원 값이 규칙상 비활성 옵션인지 */
  partyOptionDisabled: boolean;
}): string {
  if (!input.mode) return '모드(일반전/경쟁전)를 선택해 주세요.';
  if (input.platform !== 'STEAM' && input.platform !== 'KAKAO') {
    return '플랫폼(스팀 또는 카카오)을 선택해 주세요.';
  }
  if (input.mode === 'NORMAL' && !input.perspective) {
    return '일반전에서는 시점(3인칭/1인칭)을 선택해 주세요.';
  }
  if (!input.partySize) return '인원을 선택해 주세요.';
  if (input.partyOptionDisabled) {
    return input.mode === 'RANKED'
      ? '경쟁전에서는 솔로 또는 스쿼드(4인)만 선택할 수 있습니다.'
      : '선택한 인원 구성을 확인해 주세요.';
  }
  return '';
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
  partySize: string,
  extras?: { platform?: string; perspective?: string }
): { label: string; value: string }[] {
  const gameLabel = GAME_OPTIONS.find((g) => g.key === game)?.label ?? game;
  const rows: { label: string; value: string }[] = [{ label: '게임', value: gameLabel }];
  if (game === 'PUBG' && extras?.platform) {
    rows.push({ label: '플랫폼', value: pubgPlatformLabel(extras.platform) });
  }
  if (game === 'PUBG' && extras?.platform && !mode && position && !String(position).startsWith('pg:')) {
    rows.push({ label: '선호 맵', value: pubgPreferredMapLabel(position) });
  }
  const modeOpts = getMatchModeOptions(game);
  if (modeOpts.length > 0 && mode) {
    rows.push({ label: '모드', value: modeLabel(game, mode) });
  }
  if (game === 'PUBG' && mode === 'NORMAL' && extras?.perspective) {
    rows.push({ label: '시점', value: pubgPerspectiveLabel(extras.perspective) });
  }
  if (getMatchPartyOptions(game).length > 0 && partySize) {
    rows.push({ label: '인원', value: partySizeLabel(partySize, game) });
  }
  if (game === 'PUBG' && mode === 'NORMAL') {
    rows.push({ label: '티어', value: '무관' });
  } else if (game === 'COUNTER_STRIKE_2' && mode === 'CASUAL_DM') {
    rows.push({ label: '티어', value: '무관' });
  } else if (createFormShowTier(game, mode)) {
    rows.push({ label: '티어', value: tierLabel(tier || '', game, mode) });
  }
  if (
    game !== 'PUBG'
    && !(game === 'COUNTER_STRIKE_2' && mode === 'CASUAL_DM')
    && !(game === 'LEAGUE_OF_LEGENDS' && mode === 'ARAM')
    && position
  ) {
    rows.push({ label: '포지션', value: position });
  }
  return rows;
}
