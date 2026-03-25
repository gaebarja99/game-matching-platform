/** 목록에서 제외할 방장 닉네임 (요청에 따라 확장 가능) */
export function isHiddenGameRoomHost(nickname?: string | null): boolean {
  return (nickname ?? '').trim() === '재혁';
}
