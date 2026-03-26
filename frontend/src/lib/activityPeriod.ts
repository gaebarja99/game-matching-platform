/**
 * 가입일(서버에서 내려오는 값)을 Date로 변환.
 * ISO 문자열·타임스탬프 배열(Jackson) 등을 허용.
 */
function parseJoinDate(createdAt: unknown): Date | null {
  if (createdAt == null) return null;
  if (typeof createdAt === 'string') {
    const s = createdAt.trim();
    if (!s) return null;
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  if (Array.isArray(createdAt) && createdAt.length >= 3) {
    const y = Number(createdAt[0]);
    const monthRaw = Number(createdAt[1]);
    const day = Number(createdAt[2]);
    if (!Number.isFinite(y) || !Number.isFinite(monthRaw) || !Number.isFinite(day)) return null;
    const monthIndex = monthRaw >= 1 && monthRaw <= 12 ? monthRaw - 1 : monthRaw;
    const d = new Date(y, monthIndex, day);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  return null;
}

/**
 * 가입일 기준 활동 기간 표시 (프로필 카드용).
 * - 1년 이상: "N년 M개월 +" / "N년 +"
 * - 1개월 이상 1년 미만: "N개월 +"
 * - 그 외: 달력 기준 경과 일수(가입 당일 = 1일)
 */
export function formatActivityPeriod(createdAt: unknown): string {
  const d = parseJoinDate(createdAt);
  if (!d) return '—';
  const now = new Date();
  if (d.getTime() > now.getTime()) return '—';

  const totalMonths =
    (now.getFullYear() - d.getFullYear()) * 12 + (now.getMonth() - d.getMonth());
  const dayAdj = now.getDate() < d.getDate() ? -1 : 0;
  const tm = totalMonths + dayAdj;

  if (tm >= 12) {
    const y = Math.floor(tm / 12);
    const m = tm % 12;
    return m > 0 ? `${y}년 ${m}개월 +` : `${y}년 +`;
  }
  if (tm >= 1) return `${tm}개월 +`;

  const startJoin = Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
  const startToday = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  const diffDays = Math.floor((startToday - startJoin) / 86400000);
  const inclusiveDays = diffDays + 1;
  return `${Math.max(1, inclusiveDays)}일`;
}
