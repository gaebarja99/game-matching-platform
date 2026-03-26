import { apiFetch } from './client';

export type AccountLinkProvider = 'DISCORD' | 'STEAM' | 'BLIZZARD' | 'RIOT';
export type RiotGameType = 'lol' | 'valorant';
export type LolPlatform = 'kr' | 'jp1' | 'na1' | 'euw1' | 'eun1';
export type ValorantRegion = 'ap' | 'kr' | 'jp' | 'na' | 'eu';

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
  gameType?: string;
  verificationMethod?: string;
  ownershipVerified?: boolean;
  message?: string;
}

export interface RiotVerificationStartResponse {
  verificationId: string;
  gameType: RiotGameType;
  platform?: LolPlatform | string | null;
  gameName: string;
  tagLine: string;
  verificationMethod: string;
  verificationCode?: string | null;
  currentCardId?: string | null;
  currentCardImageUrl?: string | null;
  instructionTitle?: string | null;
  instructionBody?: string | null;
}

export async function fetchAccountConnections() {
  return apiFetch<AccountConnectionsResponse>('/api/account-links');
}

export async function fetchAccountConnectionsForUser(userId: number) {
  return apiFetch<AccountConnectionsResponse>(`/api/account-links/user/${userId}`);
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

export async function startRiotVerification(
  gameType: RiotGameType,
  gameName: string,
  tagLine: string,
  platform?: LolPlatform | ValorantRegion,
) {
  return apiFetch<RiotVerificationStartResponse>('/api/account-links/riot/verification/start', {
    method: 'POST',
    body: JSON.stringify({ gameType, platform, gameName, tagLine }),
  });
}

export async function confirmRiotVerification(verificationId: string) {
  return apiFetch<RiotManualLinkResponse>('/api/account-links/riot/verification/confirm', {
    method: 'POST',
    body: JSON.stringify({ verificationId }),
  });
}
