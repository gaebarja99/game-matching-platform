import { apiUrl } from './client';

export type UserSummaryDto = {
  userId: number;
  username: string;
  nickname: string | null;
  profileImageUrl: string | null;
  createdAt: unknown;
  level: number;
  matchCount: number;
};

export async function fetchUserSummary(userId: number): Promise<UserSummaryDto> {
  const res = await fetch(apiUrl(`api/users/${userId}/summary`), { credentials: 'include' });
  if (res.status === 404) throw new Error('NOT_FOUND');
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

