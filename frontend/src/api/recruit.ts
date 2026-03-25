import { apiFetch } from './client';

export interface RecruitPost {
  id: number;
  userId: number;
  game: string;
  summonerName: string;
  mainPosition?: string;
  findPosition?: string;
  tier?: string;
  region?: string;
  mode?: string;
  memo?: string;
  createdAt: string;
}

export interface RecruitFilters {
  game?: string;
  tier?: string;
  mainPosition?: string;
  findPosition?: string;
  region?: string;
  mode?: string;
}

export async function fetchRecruitList(game: string, filters?: RecruitFilters): Promise<RecruitPost[]> {
  const params = new URLSearchParams();
  params.set('game', game && game !== 'ALL' ? game : 'ALL');
  if (filters?.tier) params.set('tier', filters.tier);
  if (filters?.mainPosition) params.set('mainPosition', filters.mainPosition);
  if (filters?.findPosition) params.set('findPosition', filters.findPosition);
  if (filters?.region) params.set('region', filters.region);
  if (filters?.mode) params.set('mode', filters.mode);
  const { ok, data } = await apiFetch<{ list: RecruitPost[] }>(`/api/recruit?${params.toString()}`);
  if (!ok || !data) return [];
  return data.list ?? [];
}
