import { apiFetch } from './client';

export type AccountLinkProvider = 'DISCORD' | 'STEAM' | 'BLIZZARD' | 'RIOT';

export interface AccountConnectionStatus {
  provider: string;
  connected: boolean;
  displayName?: string | null;
  secondaryValue?: string | null;
  avatarUrl?: string | null;
  ownershipVerified: boolean;
  connectUrl?: string | null;
  note?: string | null;
  /** Riot: LoL 솔로/자유 랭크 요약 */
  lolRankSummary?: string | null;
  /** Riot: 발로란트 경쟁 티어 요약 */
  valorantRankSummary?: string | null;
  /** 공개 프로필 등에 이 연동 노출 여부 */
  publicProfileVisible?: boolean;
  /** Riot: LoL 랭크 요약 공개 */
  publicLolRankVisible?: boolean;
  /** Riot: 발로란트 티어 요약 공개 */
  publicValorantRankVisible?: boolean;
}

export interface AccountConnectionsResponse {
  userId: number;
  connections: AccountConnectionStatus[];
}

export interface OAuthStartResponse {
  provider: string;
  authorizationUrl: string;
}

export interface RiotManualLinkResponse {
  id: number;
  userId: number;
  puuid: string;
  gameName: string;
  tagLine: string;
  message?: string;
}

export async function fetchAccountConnections() {
  return apiFetch<AccountConnectionsResponse>('/api/account-links');
}

export async function unlinkAccount(provider: Lowercase<AccountLinkProvider>) {
  return apiFetch<void>(`/api/account-links/${provider}`, {
    method: 'DELETE',
  });
}

export async function startOAuthLink(provider: 'discord' | 'steam' | 'blizzard') {
  return apiFetch<OAuthStartResponse>(`/api/account-links/oauth/${provider}/start`);
}

export async function linkRiotAccount(gameName: string, tagLine: string) {
  return apiFetch<RiotManualLinkResponse>('/api/account-links/riot/manual', {
    method: 'POST',
    body: JSON.stringify({ gameName, tagLine }),
  });
}
