import { apiFetch } from './client';

export interface MatchSessionMember {
  userId: number;
  nickname?: string;
  profileImageUrl?: string;
}

export interface MatchSessionInfo {
  id: number;
  game: string;
  createdAt: string;
  members: MatchSessionMember[];
}

export interface MatchConfig {
  game: string;
  mode?: string;
  partySize?: string;
  position?: string;
  // 티어는 매칭 제한값이 아닌 희망 타겟으로 관리
  targetTier?: string;
  tierPolicy?: 'ANY' | 'TARGET_ONLY';
}

export async function joinMatchQueue(config: MatchConfig): Promise<{ inQueue: boolean }> {
  const payload = {
    game: config.game,
    tier: config.tierPolicy === 'TARGET_ONLY' ? config.targetTier : undefined,
    position: config.position,
  };
  const { ok, data } = await apiFetch<{ inQueue: boolean }>('/api/match/queue/join', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Content-Type': 'application/json' },
  });
  if (!ok) return { inQueue: false };
  return data ?? { inQueue: true };
}

export async function leaveMatchQueue(): Promise<void> {
  await apiFetch('/api/match/queue/leave', { method: 'DELETE' });
}

export async function getMatchQueueStatus(): Promise<boolean> {
  const { ok, data } = await apiFetch<{ inQueue: boolean }>('/api/match/queue/status');
  if (!ok || !data) return false;
  return data.inQueue;
}

export async function getMatchSession(sessionId: number): Promise<MatchSessionInfo | null> {
  const { ok, data } = await apiFetch<MatchSessionInfo>(`/api/match/sessions/${sessionId}`);
  if (!ok || !data) return null;
  return data;
}

export interface MatchSessionListItem {
  id: number;
  game: string;
  createdAt: string;
}

export async function getMyMatchSessions(): Promise<MatchSessionListItem[]> {
  const { ok, data } = await apiFetch<{ list: MatchSessionListItem[] }>('/api/match/sessions');
  if (!ok || !data) return [];
  return data.list ?? [];
}

export async function deleteMatchSession(sessionId: number): Promise<boolean> {
  const { ok } = await apiFetch(`/api/match/sessions/${sessionId}`, { method: 'DELETE' });
  return ok;
}

export interface MatchChatMessage {
  id: number;
  sessionId: number;
  fromUserId: number;
  fromNickname?: string;
  fromProfileImageUrl?: string;
  text: string;
  createdAt: string;
}

export async function getMatchChatMessages(sessionId: number, limit = 50): Promise<MatchChatMessage[]> {
  const { ok, data } = await apiFetch<{ list: MatchChatMessage[] }>(
    `/api/match/sessions/${sessionId}/messages?limit=${limit}`
  );
  if (!ok || !data) return [];
  return data.list ?? [];
}

export async function sendMatchChatMessage(sessionId: number, text: string): Promise<{ ok: boolean; message?: string }> {
  const { ok, message } = await apiFetch(`/api/match/sessions/${sessionId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ text }),
    headers: { 'Content-Type': 'application/json' },
  });
  return { ok, message };
}
