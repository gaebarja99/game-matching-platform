import type { GameRoomItem } from '../api/gameRooms';

function parseGameOptionsFields(gameOptions?: string | null): {
  maxPlayers?: number;
  partySize?: string;
  mode?: string;
} {
  if (!gameOptions?.trim()) return {};
  try {
    const o = JSON.parse(gameOptions) as Record<string, unknown>;
    return {
      maxPlayers: typeof o.maxPlayers === 'number' && Number.isFinite(o.maxPlayers) ? o.maxPlayers : undefined,
      partySize: typeof o.partySize === 'string' ? o.partySize : undefined,
      mode: typeof o.mode === 'string' ? o.mode : undefined,
    };
  } catch {
    return {};
  }
}

/** 방 게임·옵션 기준 최대 인원 (목록/입장 제한용, Home 표시 로직과 동일) */
export function resolveRoomMaxPlayers(room: Pick<GameRoomItem, 'game' | 'gameOptions'>): number | null {
  const { game, gameOptions } = room;
  const op = parseGameOptionsFields(gameOptions);
  if (game === 'LEAGUE_OF_LEGENDS' || game === 'VALORANT' || game === 'OVERWATCH') {
    return op.maxPlayers ?? 5;
  }
  if (game === 'PUBG') {
    return op.maxPlayers ?? (op.partySize === 'SQUAD' ? 4 : 2);
  }
  if (game === 'COUNTER_STRIKE_2') {
    return op.maxPlayers ?? (op.mode === 'WINGMAN' ? 2 : 5);
  }
  return null;
}

export function getRoomCapacityMeta(room: Pick<GameRoomItem, 'game' | 'gameOptions' | 'memberCount'>): {
  maxPlayers: number | null;
  currentParticipants: number;
  isFull: boolean;
} {
  const maxPlayers = resolveRoomMaxPlayers(room);
  const currentParticipants = room.memberCount;
  const isFull = maxPlayers != null && currentParticipants >= maxPlayers;
  return { maxPlayers, currentParticipants, isFull };
}
