import { apiFetch } from './client';

export interface GameRoomItem {
  id: number;
  title: string;
  memo?: string;
  game: string;
  gameOptions?: string;
  hostUserId: number;
  hostNickname?: string;
  groupChatRoomId?: number;
  closed: boolean;
  createdAt: string;
  memberCount: number;
  isMember?: boolean;
  isHost?: boolean;
}

export async function fetchGameRoomList(game?: string, closed?: boolean): Promise<GameRoomItem[]> {
  const params = new URLSearchParams();
  if (game) params.set('game', game);
  if (closed !== undefined) params.set('closed', String(closed));
  const { ok, data } = await apiFetch<{ list: GameRoomItem[] }>(`/api/game-rooms?${params.toString()}`);
  if (!ok || !data) return [];
  return data.list ?? [];
}

export type CreateGameRoomResult =
  | { ok: true; id: number; title: string; game: string; groupChatRoomId: number }
  | { ok: false; message?: string };

export async function createGameRoom(body: {
  title: string;
  memo?: string;
  deletePassword: string;
  game?: string;
  gameOptions?: string;
}): Promise<CreateGameRoomResult> {
  const { ok, data, message, status } = await apiFetch<{
    id: number;
    title: string;
    game: string;
    groupChatRoomId?: number | string | null;
  }>('/api/game-rooms', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
  if (!ok || !data || typeof data.id !== 'number') {
    return {
      ok: false,
      message: message ?? (status === 401 ? '로그인이 필요합니다.' : undefined),
    };
  }
  const raw = data.groupChatRoomId;
  const groupChatRoomId =
    typeof raw === 'number' && Number.isFinite(raw)
      ? raw
      : typeof raw === 'string' && /^\d+$/.test(raw)
        ? parseInt(raw, 10)
        : NaN;
  if (!Number.isFinite(groupChatRoomId)) {
    return { ok: false, message: '채팅방 연결 정보를 받지 못했습니다. 다시 시도해 주세요.' };
  }
  return { ok: true, id: data.id, title: data.title, game: data.game, groupChatRoomId };
}

export async function joinGameRoom(roomId: number): Promise<boolean> {
  const { ok } = await apiFetch(`/api/game-rooms/${roomId}/join`, { method: 'POST' });
  return ok;
}

export async function leaveGameRoom(roomId: number): Promise<{ ok: boolean; message?: string }> {
  const { ok, data } = await apiFetch<{ message?: string }>(`/api/game-rooms/${roomId}/leave`, { method: 'POST' });
  return { ok: !!ok, message: data?.message };
}

export async function closeGameRoom(roomId: number): Promise<boolean> {
  const { ok } = await apiFetch(`/api/game-rooms/${roomId}/close`, { method: 'POST' });
  return ok;
}

export async function deleteGameRoom(roomId: number, deletePassword: string): Promise<{ ok: boolean; message?: string }> {
  const { ok, data } = await apiFetch<{ message?: string }>(
    `/api/game-rooms/${roomId}`,
    { method: 'DELETE', body: JSON.stringify({ deletePassword }), headers: { 'Content-Type': 'application/json' } }
  );
  return { ok, message: data?.message };
}

export async function getGameRoomChatRoomId(roomId: number): Promise<number | null> {
  const { ok, data } = await apiFetch<{ groupChatRoomId: number }>(`/api/game-rooms/${roomId}/chat-room-id`);
  if (!ok || !data) return null;
  return data.groupChatRoomId;
}
