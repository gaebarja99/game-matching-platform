import { repairMojibakeText } from './auth';
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

function normalizeStreamItem(stream: StreamItem): StreamItem {
  return {
    ...stream,
    title: repairMojibakeText(stream.title) ?? stream.title,
    game: repairMojibakeText(stream.game) ?? stream.game,
    broadcasterNickname: repairMojibakeText(stream.broadcasterNickname) ?? stream.broadcasterNickname,
  };
}

export async function fetchLiveStreams(): Promise<StreamItem[]> {
  const { ok, data } = await apiFetch<StreamItem[]>('/api/streams/live');
  if (!ok || !Array.isArray(data)) return [];
  return data.map(normalizeStreamItem);
}

/** 최근 방송 목록 (?�트리밍 메인??. limit 기본 50. */
export async function fetchRecentStreams(limit = 50): Promise<StreamItem[]> {
  const { ok, data } = await apiFetch<StreamItem[]>(`/api/streams?limit=${Math.min(limit, 50)}`);
  if (!ok || !Array.isArray(data)) return [];
  return data.map(normalizeStreamItem);
}
