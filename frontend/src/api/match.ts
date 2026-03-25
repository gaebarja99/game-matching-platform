import { apiFetch } from './client';
import { encodePubgQueueMeta } from '../utils/randomMatchHelpers';

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
  /** 발로란트 단순 큐: 서버에 preferredRole로 전달(세션의 userId와 함께 저장) */
  preferredRole?: string;
  /** 오버워치2 단순 큐: 서버에 preferredPosition으로 전달 */
  preferredPosition?: string;
  // 티어는 매칭 제한값이 아닌 희망 타겟으로 관리
  targetTier?: string;
  tierPolicy?: 'ANY' | 'TARGET_ONLY';
  /** PUBG 랜덤 매칭 */
  pubgPlatform?: string;
  pubgPerspective?: string;
  /** PUBG 단순 큐: 플랫폼별 4인 FIFO (모드·인원 없음) */
  pubgSimpleFourPerson?: boolean;
  pubgPreferredMap?: string;
}

export interface JoinMatchQueueResult {
  inQueue: boolean;
  lobbyCount?: number;
  targetSize?: number;
}

export async function joinMatchQueue(config: MatchConfig): Promise<JoinMatchQueueResult> {
  if (config.game === 'PUBG' && config.pubgSimpleFourPerson) {
    const platform = config.pubgPlatform?.trim();
    const preferredMap = (config.pubgPreferredMap ?? 'ALL').trim() || 'ALL';
    const payload: Record<string, string | undefined> = {
      game: 'PUBG',
      platform,
      preferredMap,
    };
    const { ok, data } = await apiFetch<JoinMatchQueueResult>('/api/match/queue/join', {
      method: 'POST',
      body: JSON.stringify(payload),
      headers: { 'Content-Type': 'application/json' },
    });
    if (!ok) return { inQueue: false };
    return data ?? { inQueue: true };
  }

  let tier: string | undefined;
  if (config.game === 'VALORANT' || config.game === 'OVERWATCH' || config.game === 'COUNTER_STRIKE_2') {
    tier = undefined;
  } else if (config.game === 'PUBG' && config.mode === 'RANKED') {
    tier = config.targetTier?.trim() ? config.targetTier.trim() : undefined;
  } else if (config.tierPolicy === 'TARGET_ONLY') {
    tier = config.targetTier;
  }

  let position: string | undefined;
  if (config.game === 'PUBG') {
    position = encodePubgQueueMeta({
      platform: config.pubgPlatform ?? '',
      mode: config.mode ?? '',
      perspective: config.pubgPerspective,
      partySize: config.partySize,
    });
  } else {
    const pr = config.preferredRole?.trim();
    position = pr || config.position;
  }

  const payload: Record<string, string | undefined> = {
    game: config.game,
    tier,
    position,
  };
  if (config.game === 'VALORANT' && position) {
    payload.preferredRole = position;
  }
  if (config.game === 'OVERWATCH' && position) {
    payload.preferredPosition = position;
  }
  if (config.game === 'COUNTER_STRIKE_2' && position) {
    payload.selectedPosition = position;
  }
  const { ok, data } = await apiFetch<JoinMatchQueueResult>('/api/match/queue/join', {
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

export interface MatchQueueStatus {
  inQueue: boolean;
  lobbyCount?: number;
  targetSize?: number;
}

export async function getMatchQueueStatus(): Promise<MatchQueueStatus> {
  const { ok, data } = await apiFetch<MatchQueueStatus>('/api/match/queue/status');
  if (!ok || !data) return { inQueue: false };
  return data;
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

export async function sendMatchChatMessage(sessionId: number, text: string): Promise<boolean> {
  const { ok } = await apiFetch(`/api/match/sessions/${sessionId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ text }),
    headers: { 'Content-Type': 'application/json' },
  });
  return ok;
}
