import { TIER_OPTIONS } from './randomMatchHelpers';

/** 방 만들기 큐 (value는 API/저장용) */
export const LOL_ROOM_QUEUE_OPTIONS = [
  { value: 'SOLO', label: '솔로 랭크' },
  { value: 'FLEX', label: '자유 랭크' },
  { value: 'QUICK', label: '신속 대전' },
  { value: 'ARAM', label: '칼바람 나락' },
] as const;

export const LOL_LANES = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'] as const;
export type LolLane = (typeof LOL_LANES)[number];

export const LOL_LANE_LABELS: Record<LolLane, string> = {
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  ADC: '원딜',
  SUPPORT: '서포',
};

/** 신속 대전 주·부 역할군 (채우기 + 5라인) */
export const LOL_QUICK_ROLE_ORDER = ['FILL', 'TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'] as const;
export type LolQuickRole = (typeof LOL_QUICK_ROLE_ORDER)[number];

export const LOL_QUICK_ROLE_LABELS: Record<LolQuickRole, string> = {
  FILL: '채우기',
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  ADC: '원딜',
  SUPPORT: '서포',
};

export function isLolQuickRole(s: string): s is LolQuickRole {
  return (LOL_QUICK_ROLE_ORDER as readonly string[]).includes(s);
}

const RANK_VALUES = TIER_OPTIONS.map((o) => o.value).filter(Boolean) as string[];

export function compareRankTier(a: string, b: string): number {
  return RANK_VALUES.indexOf(a) - RANK_VALUES.indexOf(b);
}

export function normalizeTierRange(minV: string, maxV: string): { min: string; max: string } {
  if (!minV && !maxV) return { min: '', max: '' };
  if (!minV) return { min: '', max: maxV };
  if (!maxV) return { min: minV, max: '' };
  return compareRankTier(minV, maxV) > 0 ? { min: maxV, max: minV } : { min: minV, max: maxV };
}

export function partySlotsFromSize(partySize: string): number {
  if (partySize === '1') return 1;
  if (partySize === 'DUO') return 2;
  if (partySize === '3') return 3;
  if (partySize === '4') return 4;
  if (partySize === '5') return 5;
  return 0;
}

/** 방 만들기 전용 인원 (값은 기존 gameOptions와 동일: DUO = 2인) */
const LOL_CREATE_P2 = { value: 'DUO', label: '2인' } as const;
const LOL_CREATE_P3 = { value: '3', label: '3인' } as const;
const LOL_CREATE_P4 = { value: '4', label: '4인' } as const;
const LOL_CREATE_P5 = { value: '5', label: '5인' } as const;

/** 큐 유형별 선택 가능한 인원 (솔로 2 고정, 자유 2·3·5, 신속/칼바람 2·3·4·5) */
export function lolCreatePartySizeOptions(queue: string): { value: string; label: string }[] {
  if (queue === 'SOLO') return [{ ...LOL_CREATE_P2 }];
  if (queue === 'FLEX') return [{ ...LOL_CREATE_P2 }, { ...LOL_CREATE_P3 }, { ...LOL_CREATE_P5 }];
  if (queue === 'QUICK' || queue === 'ARAM') {
    return [{ ...LOL_CREATE_P2 }, { ...LOL_CREATE_P3 }, { ...LOL_CREATE_P4 }, { ...LOL_CREATE_P5 }];
  }
  return [{ ...LOL_CREATE_P2 }, { ...LOL_CREATE_P3 }, { ...LOL_CREATE_P4 }, { ...LOL_CREATE_P5 }];
}

export function normalizeLolCreatePartySizeForQueue(queue: string, current: string): string {
  const opts = lolCreatePartySizeOptions(queue);
  const allowed = new Set(opts.map((o) => o.value));
  if (current && allowed.has(current)) return current;
  return opts[0]?.value ?? 'DUO';
}

export function validateLolRoomForm(input: {
  queue: string;
  hostPrimary: string | null;
  hostSecondary: string | null;
  recruiting: LolLane[];
  recruitingQuick: LolQuickRole[];
  partySize: string;
}): string | null {
  if (input.queue === 'ARAM') return null;
  if (input.queue === 'QUICK') {
    if (!input.hostPrimary || !isLolQuickRole(input.hostPrimary)) return '주 역할군을 선택해 주세요.';
    if (!input.hostSecondary || !isLolQuickRole(input.hostSecondary)) return '부 역할군을 선택해 주세요.';
    if (input.hostPrimary === input.hostSecondary) return '주·부 역할군은 서로 달라야 합니다.';
    if (!input.recruitingQuick.length) return '찾는 포지션을 1개 이상 선택해 주세요.';
    const cap = Math.max(0, partySlotsFromSize(input.partySize) - 1);
    if (input.recruitingQuick.length > cap) {
      return `찾는 포지션은 최대 ${cap}개까지 선택할 수 있습니다.`;
    }
    const u = new Set(input.recruitingQuick);
    if (u.size !== input.recruitingQuick.length) return '같은 역할을 중복 선택할 수 없습니다.';
    for (const r of input.recruitingQuick) {
      if (r !== 'FILL' && (r === input.hostPrimary || r === input.hostSecondary)) {
        return '찾는 포지션에 주·부 역할과 같은 라인(채우기 제외)을 넣을 수 없습니다.';
      }
    }
    return null;
  }
  const laneKeys = LOL_LANES as readonly string[];
  if (!input.hostPrimary || !laneKeys.includes(input.hostPrimary)) {
    return '내 포지션을 선택해 주세요.';
  }
  if (input.queue === 'SOLO') {
    if (input.recruiting.length !== 1) {
      return '솔로 랭크에서는 구인 포지션을 정확히 1개 선택해 주세요.';
    }
    if (input.recruiting[0] === input.hostPrimary) {
      return '구인 포지션은 내 포지션과 달라야 합니다.';
    }
    return null;
  }
  if (input.queue === 'FLEX') {
    const cap = Math.max(0, partySlotsFromSize(input.partySize) - 1);
    if (input.recruiting.length > cap) {
      return `구인 포지션은 최대 ${cap}개까지 선택할 수 있습니다.`;
    }
    if (input.recruiting.some((l) => l === input.hostPrimary)) {
      return '구인 목록에 내 포지션을 넣을 수 없습니다.';
    }
    const u = new Set(input.recruiting);
    if (u.size !== input.recruiting.length) return '같은 포지션을 중복 선택할 수 없습니다.';
  }
  return null;
}

export function buildLolRoomGameOptions(input: {
  queue: string;
  partySize: string;
  hostPrimary: string | null;
  hostSecondary: string | null;
  recruiting: LolLane[];
  recruitingQuick: LolQuickRole[];
  tierMin: string;
}): Record<string, string | undefined> {
  return {
    mode: input.queue,
    partySize: input.partySize || undefined,
    position: input.hostPrimary || undefined,
    hp: input.hostPrimary || undefined,
    hs: input.queue === 'QUICK' ? (input.hostSecondary || undefined) : undefined,
    rp: input.queue !== 'QUICK' && input.recruiting.length ? input.recruiting.join(',') : undefined,
    rq: input.queue === 'QUICK' && input.recruitingQuick.length ? input.recruitingQuick.join(',') : undefined,
    tm: input.tierMin || undefined,
  };
}

export function parseRecruitingLanes(rp?: string): LolLane[] {
  if (!rp?.trim()) return [];
  return rp.split(',').filter((x): x is LolLane => (LOL_LANES as readonly string[]).includes(x));
}

/** 신속 대전 찾는 포지션(rq: FILL,TOP,...) */
export function parseQuickSeekingRq(rq?: string): LolQuickRole[] {
  if (!rq?.trim()) return [];
  return rq.split(',').filter((x): x is LolQuickRole => isLolQuickRole(x));
}

export function formatQuickSeekingSummary(roles: LolQuickRole[]): string {
  if (!roles.length) return '-';
  return roles.map((r) => LOL_QUICK_ROLE_LABELS[r]).join(', ');
}

export function lolQueueLabel(mode: string): string {
  return LOL_ROOM_QUEUE_OPTIONS.find((x) => x.value === mode)?.label ?? mode;
}

function tierOptLabel(v: string): string {
  return TIER_OPTIONS.find((t) => t.value === v)?.label ?? v;
}

export function formatLolTierRangeCell(tm?: string, tx?: string, legacyTier?: string): string {
  if (tm && tx) return `${tierOptLabel(tm)} ~ ${tierOptLabel(tx)}`;
  if (tm) return `${tierOptLabel(tm)} 이상`;
  if (tx) return `${tierOptLabel(tx)} 이하`;
  if (legacyTier) return tierOptLabel(legacyTier);
  return '-';
}

export function formatRecruitingSummary(lanes: LolLane[]): string {
  if (!lanes.length) return '-';
  return lanes.map((l) => LOL_LANE_LABELS[l]).join(', ');
}
