/** `game-rooms.gameOptions` JSON 파싱 (목록·상세 모달 공통) */
export type ParsedGameRoomOptions = {
  tier?: string;
  mode?: string;
  position?: string;
  maxPlayers?: number;
  myPosition?: string;
  partnerPosition?: string;
  primaryRole?: string;
  secondaryRole?: string;
  findPosition?: string;
  preferredMap?: string;
  preferredMethod?: string;
  preferredLegend?: string;
  platform?: string;
  partySize?: string;
};

export function parseGameOptions(s?: string | null): ParsedGameRoomOptions {
  if (!s || !s.trim()) return {};
  try {
    const o = JSON.parse(s) as Record<string, unknown>;
    return {
      tier: typeof o.tier === 'string' ? o.tier : undefined,
      mode: typeof o.mode === 'string' ? o.mode : undefined,
      position: typeof o.position === 'string' ? o.position : undefined,
      maxPlayers: typeof o.maxPlayers === 'number' ? o.maxPlayers : undefined,
      myPosition: typeof o.myPosition === 'string' ? o.myPosition : undefined,
      partnerPosition: typeof o.partnerPosition === 'string' ? o.partnerPosition : undefined,
      primaryRole: typeof o.primaryRole === 'string' ? o.primaryRole : undefined,
      secondaryRole: typeof o.secondaryRole === 'string' ? o.secondaryRole : undefined,
      findPosition: typeof o.findPosition === 'string' ? o.findPosition : undefined,
      preferredMap: typeof o.preferredMap === 'string' ? o.preferredMap : undefined,
      preferredMethod: typeof o.preferredMethod === 'string' ? o.preferredMethod : undefined,
      preferredLegend: typeof o.preferredLegend === 'string' ? o.preferredLegend : undefined,
      platform: typeof o.platform === 'string' ? o.platform : undefined,
      partySize: typeof o.partySize === 'string' ? o.partySize : undefined,
    };
  } catch {
    return {};
  }
}
