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
