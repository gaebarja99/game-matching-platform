const GM_RANDOM_MATCH_PENDING_KEY = 'gm_random_match_pending';

export type RandomMatchPendingSnapshot = {
  game: string;
  mode: string;
  tier: string;
  partySize: string;
  position: string | null;
};

export function saveRandomMatchPending(s: RandomMatchPendingSnapshot): void {
  try {
    sessionStorage.setItem(GM_RANDOM_MATCH_PENDING_KEY, JSON.stringify(s));
  } catch {
    /* ignore */
  }
}

export function loadRandomMatchPending(): RandomMatchPendingSnapshot | null {
  try {
    const raw = sessionStorage.getItem(GM_RANDOM_MATCH_PENDING_KEY);
    if (!raw) return null;
    const o = JSON.parse(raw) as RandomMatchPendingSnapshot;
    if (typeof o.game !== 'string') return null;
    return {
      game: o.game,
      mode: typeof o.mode === 'string' ? o.mode : '',
      tier: typeof o.tier === 'string' ? o.tier : '',
      partySize: typeof o.partySize === 'string' ? o.partySize : '',
      position: o.position === null || typeof o.position === 'string' ? o.position : null,
    };
  } catch {
    return null;
  }
}

export function clearRandomMatchPending(): void {
  try {
    sessionStorage.removeItem(GM_RANDOM_MATCH_PENDING_KEY);
  } catch {
    /* ignore */
  }
}
