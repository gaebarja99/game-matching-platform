/** 1:1 DM에서 본인(self) 제외·차단용 공통 유틸 */

export function isDmWithSelf(myId: number | undefined, otherUserId: number): boolean {
  return myId != null && otherUserId === myId;
}

export function filterFriendsExcludingSelf<T extends { id: number }>(friends: T[], myId: number | undefined): T[] {
  if (myId == null) return friends;
  return friends.filter((f) => f.id !== myId);
}

export function filterConversationsExcludingSelf<T extends { otherUserId: number }>(
  rows: T[],
  myId: number | undefined,
): T[] {
  if (myId == null) return rows;
  return rows.filter((c) => c.otherUserId !== myId);
}
