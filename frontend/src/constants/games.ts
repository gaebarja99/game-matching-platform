/**
 * 선호 게임 선택용 목록 (실제 게임만; {@code GameList.OTHERS} 등 비게임 항목 제외).
 * {@code preferredGames} 필드에는 {@code value}를 쉼표+공백으로 이어 저장합니다.
 */
export const PREFERRED_GAME_OPTIONS = [
  { value: '리그 오브 레전드' },
  { value: '발로란트' },
  { value: '오버워치' },
  { value: 'PUBG' },
  { value: '카운터 스트라이크 2' },
] as const;

const VALID_VALUES = new Set<string>(
  PREFERRED_GAME_OPTIONS.map((o) => o.value)
);

/** API 문자열 → 체크박스용 값 배열 (목록에 없는 토큰은 제외) */
export function parsePreferredGamesToSelected(raw: string | null | undefined): string[] {
  if (!raw?.trim()) return [];
  const valid = VALID_VALUES as ReadonlySet<string>;
  return raw
    .split(',')
    .map((t) => t.trim())
    .filter((t) => valid.has(t));
}

export function serializePreferredGames(selected: string[]): string | null {
  const ordered = PREFERRED_GAME_OPTIONS.map((o) => o.value).filter((v) => selected.includes(v));
  return ordered.length > 0 ? ordered.join(', ') : null;
}
