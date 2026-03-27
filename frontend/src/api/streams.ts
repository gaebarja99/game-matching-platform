import { apiFetch } from './client';

export interface StreamItem {
  id: number;
  title?: string;
  game?: string;
  status?: string;
  playbackUrl?: string;
  externalUrl?: string;
  userId: number;
  broadcasterNickname?: string;
  broadcasterProfileImageUrl?: string;
  followerCount?: number;
  viewerCount?: number;
  startedAt?: string;
  endedAt?: string | null;
  createdAt?: string;
}

export async function fetchLiveStreams(): Promise<StreamItem[]> {
  const { ok, data } = await apiFetch<StreamItem[]>('/api/streams/live');
  if (!ok || !Array.isArray(data)) return [];
  return data;
}

/** 최근 방송 목록 (스트리밍 메인용). limit 기본 50. */
export async function fetchRecentStreams(limit = 50): Promise<StreamItem[]> {
  const { ok, data } = await apiFetch<StreamItem[]>(`/api/streams?limit=${Math.min(limit, 50)}`);
  if (!ok || !Array.isArray(data)) return [];
  return data;
}
