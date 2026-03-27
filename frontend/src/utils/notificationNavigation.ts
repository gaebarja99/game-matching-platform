export type NavigableNotificationItem = {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
  streamId?: number;
  actorUserId?: number;
  actorNickname?: string;
  targetPath?: string;
};

export function resolveNotificationTargetPath(n: Partial<NavigableNotificationItem>): string | null {
  const explicit = typeof n.targetPath === 'string' ? n.targetPath.trim() : '';
  if (explicit) return explicit;

  switch ((n.type ?? '').toUpperCase()) {
    case 'FOLLOWING_STARTED_STREAM':
      return n.streamId ? `/watch/${n.streamId}` : '/streams';
    case 'PAYMENT_COMPLETED':
    case 'PAYMENT_REFUNDED':
    case 'ADMIN_PANG_GIFT':
      return '/profile/pang';
    case 'FRIEND_REQUEST':
      return '/profile';
    case 'ADMIN_STREAM_NOTICE':
      return '/studio';
    case 'CHANNEL_PERMISSION_GRANTED':
      return '/studio/channel/manage';
    case 'CHANNEL_PERMISSION_REVOKED':
      return '/studio';
    case 'NEW_DM':
      return n.actorUserId ? `/dm?userId=${n.actorUserId}` : '/dm';
    case 'GROUP_CHAT_INVITE':
    case 'GROUP_CHAT_MENTION':
      return n.streamId ? `/group-chat/room/${n.streamId}` : '/group-chat';
    case 'MATCH_CHAT_MENTION':
      return n.streamId ? `/match-chat/${n.streamId}` : '/match-history';
    default:
      return null;
  }
}
