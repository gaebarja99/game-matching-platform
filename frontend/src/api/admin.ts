import { apiFetch } from './client';

export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminStreamerRow {
  id: number;
  loginId: string;
  displayName: string;
  streamerTier: 'GENERAL' | 'PARTNER';
  totalReceivedPang: number;
  createdAt?: string;
}

export interface AdminMemberRow {
  id: number;
  loginId: string;
  username: string;
  nickname: string;
  email: string;
  role: 'USER' | 'ADMIN';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
  provider: string;
  pangBalance: number;
  streamerTier: 'GENERAL' | 'PARTNER';
  createdAt?: string;
  lastLoginAt?: string | null;
  suspendedUntil?: string | null;
  suspensionReason?: string | null;
}

export interface AdminSettlementRow {
  id: number;
  orderId: string;
  loginId: string;
  displayName: string;
  kind: string;
  pangAmount: number;
  amountWon: number;
  status: string;
  createdAt?: string;
  completedAt?: string | null;
  commissionPercent?: number;
  commissionPang?: number;
  netPang?: number;
}

export interface AdminReportRow {
  id: number;
  reporterId: number;
  reporterUsername: string;
  reportedUserId: number;
  reportedUsername: string;
  reportedUserStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
  reason: string;
  description: string;
  status: 'PENDING' | 'IN_REVIEW' | 'RESOLVED' | 'DISMISSED';
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string | null;
  adminNote?: string | null;
}

export interface AdminAuditLogRow {
  id: number;
  targetType: string;
  actionType: string;
  summary: string;
  detail?: string | null;
  adminUserId?: number | null;
  createdAt?: string;
}

export interface AdminRevenueRecentOrder {
  id: number;
  orderId: string;
  loginId: string;
  displayName: string;
  kind: string;
  amountWon: number;
  pangAmount: number;
  status: string;
  createdAt?: string;
}

export interface AdminRevenueSummary {
  totalRequestedWon: number;
  completedSalesWon: number;
  pendingSalesWon: number;
  failedSalesWon: number;
  cancelledSalesWon: number;
  completedPang: number;
  todayCompletedWon: number;
  monthCompletedWon: number;
  subscriptionSalesWon: number;
  adFreeSalesWon: number;
  platformRevenueWon: number;
  totalDonationPang: number;
  completedSettlementCommissionPang: number;
  completedSettlementPang: number;
  recentOrders: AdminRevenueRecentOrder[];
}

export interface AdminCommunityPostRow {
  id: number;
  title: string;
  category: string;
  categoryLabel: string;
  status: 'ACTIVE' | 'BLIND' | 'DELETED_BY_USER' | 'DELETED_BY_ADMIN';
  statusLabel: string;
  authorId?: number | null;
  authorLoginId: string;
  authorName: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  reportCount: number;
  isNotice: boolean;
  createdAt?: string;
}

export interface AdminMatchRoomRow {
  id: number;
  game: string;
  summonerName: string;
  mainPosition: string;
  findPosition: string;
  tier: string;
  region: string;
  mode: string;
  memo: string;
  createdAt?: string;
  ownerId?: number | null;
  ownerLoginId: string;
  ownerName: string;
}

export interface AdminBroadcastRow {
  id: number;
  title: string;
  game: string;
  status: 'CREATED' | 'LIVE' | 'ENDED';
  statusLabel: string;
  userId: number;
  loginId: string;
  displayName: string;
  createdAt?: string;
  startedAt?: string | null;
  endedAt?: string | null;
  visibleInRecent: boolean;
  warningCount: number;
  lastWarningAt?: string | null;
  lastWarningMessage?: string;
}

function qs(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

export async function fetchAdminStreamers(args: {
  query?: string;
  tier?: string;
  page?: number;
  size?: number;
}) {
  const { query, tier, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminStreamerRow>>(`/api/admin/streamers${qs({ query, tier, page, size })}`);
}

export async function updateAdminStreamerTier(userId: number, tier: 'GENERAL' | 'PARTNER') {
  return apiFetch<{ tier: string; message?: string }>(`/api/admin/streamers/${userId}/tier`, {
    method: 'PATCH',
    body: JSON.stringify({ tier }),
  });
}

export async function fetchAdminMembers(args: {
  query?: string;
  role?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  const { query, role, status, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminMemberRow>>(`/api/admin/users${qs({ query, role, status, page, size })}`);
}

export async function fetchAdminMemberHistory(userId: number) {
  return apiFetch<AdminAuditLogRow[]>(`/api/admin/users/${userId}/history`);
}

export async function updateAdminMember(
  userId: number,
  body: {
    status?: string;
    role?: string;
    nickname?: string;
    suspensionDays?: number;
    permanentSuspension?: boolean;
    suspensionReason?: string;
  }
) {
  return apiFetch<{ status: string; role: string; nickname?: string; message?: string; suspendedUntil?: string | null; suspensionReason?: string | null }>(
    `/api/admin/users/${userId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(body),
    }
  );
}

export async function giftAdminPang(body: { loginId: string; pangAmount: number; message?: string }) {
  return apiFetch<{ message?: string; loginId?: string; pangBalance?: number; recipientCount?: number }>(
    '/api/admin/pang/gift',
    {
      method: 'POST',
      body: JSON.stringify(body),
    }
  );
}

export async function fetchAdminRevenue() {
  return apiFetch<AdminRevenueSummary>('/api/admin/revenue');
}

export async function fetchAdminCommunityPosts(args: {
  query?: string;
  category?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  const { query, category, status, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminCommunityPostRow>>(`/api/admin/community/posts${qs({ query, category, status, page, size })}`);
}

export async function updateAdminCommunityPost(postId: number, action: 'blind' | 'restore' | 'delete') {
  return apiFetch<{ message?: string; status?: string; statusLabel?: string }>(`/api/admin/community/posts/${postId}`, {
    method: 'PATCH',
    body: JSON.stringify({ action }),
  });
}

export async function fetchAdminMatchRooms(args: {
  query?: string;
  game?: string;
  page?: number;
  size?: number;
}) {
  const { query, game, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminMatchRoomRow>>(`/api/admin/match-rooms${qs({ query, game, page, size })}`);
}

export async function fetchAdminBroadcasts(args: {
  query?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  const { query, status, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminBroadcastRow>>(`/api/admin/broadcasts${qs({ query, status, page, size })}`);
}

export async function updateAdminBroadcast(
  streamId: number,
  action: 'warn' | 'force_end' | 'hide_recent' | 'restore_recent',
  message?: string
) {
  return apiFetch<{ message?: string; status?: string; visibleInRecent?: boolean; warningCount?: number; lastWarningMessage?: string }>(
    `/api/admin/broadcasts/${streamId}`,
    {
      method: 'PATCH',
      body: JSON.stringify({ action, message }),
    }
  );
}

export async function deleteAdminMatchRoom(roomId: number) {
  return apiFetch<{ message?: string }>(`/api/admin/match-rooms/${roomId}`, {
    method: 'DELETE',
  });
}

export async function fetchAdminSettlements(args: {
  query?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  const { query, status, page = 0, size = 20 } = args;
  return apiFetch<PageResult<AdminSettlementRow>>(`/api/admin/settlements${qs({ query, status, page, size })}`);
}

export async function updateAdminSettlementStatus(settlementId: number, status: string) {
  return apiFetch<{ status: string; message?: string }>(`/api/admin/settlements/${settlementId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function fetchAdminReports(args: {
  status?: string;
  page?: number;
  size?: number;
}) {
  const { status, page = 0, size = 20 } = args;
  return apiFetch<AdminReportRow[]>(`/api/admin/reports${qs({ status, page, size })}`);
}

export async function updateAdminReportStatus(reportId: number, status: string, adminNote?: string) {
  return apiFetch<AdminReportRow>(
    `/api/admin/reports/${reportId}${qs({ status, adminNote })}`,
    { method: 'PATCH' }
  );
}

export async function banReportedUser(reportId: number, adminNote?: string) {
  return apiFetch<AdminReportRow>(`/api/admin/reports/${reportId}/ban${qs({ adminNote })}`, {
    method: 'POST',
  });
}

export async function unbanReportedUser(reportId: number, adminNote?: string) {
  return apiFetch<AdminReportRow>(`/api/admin/reports/${reportId}/unban${qs({ adminNote })}`, {
    method: 'POST',
  });
}
