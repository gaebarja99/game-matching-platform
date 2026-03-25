import { apiFetch } from './client';

export async function fetchOnlineCount(): Promise<number> {
  const { ok, data } = await apiFetch<{ online?: number }>('/api/stats/online');
  if (!ok || data == null) return 0;
  return typeof (data as { online?: number }).online === 'number' ? (data as { online: number }).online : 0;
}
