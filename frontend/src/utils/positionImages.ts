export const IMAGE_BASE = 'images';

// LoL 공식 포지션 아이콘 (Community Dragon 최신 자산)
// - role key: TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY
// - UI 텍스트(탑/정글/미드/원딜/서포터) ↔ role key 매핑도 함께 제공
export const LOL_ROLE_KEY_BY_KO: Record<string, 'TOP' | 'JUNGLE' | 'MIDDLE' | 'BOTTOM' | 'UTILITY'> = {
  탑: 'TOP',
  정글: 'JUNGLE',
  미드: 'MIDDLE',
  원딜: 'BOTTOM',
  서포터: 'UTILITY',
};

export const LOL_OFFICIAL_ROLE_ICON_URL_BY_KEY: Record<
  'TOP' | 'JUNGLE' | 'MIDDLE' | 'BOTTOM' | 'UTILITY',
  string
> = {
  // NOTE: static-assets/svg는 간헐적으로 500이 발생할 수 있어
  // 더 안정적인 position-selector PNG를 사용 (공식 RCP 자산)
  TOP: 'https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-clash/global/default/assets/images/position-selector/positions/icon-position-top.png',
  JUNGLE:
    'https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-clash/global/default/assets/images/position-selector/positions/icon-position-jungle.png',
  MIDDLE:
    'https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-clash/global/default/assets/images/position-selector/positions/icon-position-middle.png',
  BOTTOM:
    'https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-clash/global/default/assets/images/position-selector/positions/icon-position-bottom.png',
  UTILITY:
    'https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-clash/global/default/assets/images/position-selector/positions/icon-position-utility.png',
};

const LEAGUE_POSITION_IMAGES: Record<string, string> = {
  TOP: 'leagueoflegends/top.png',
  JUNGLE: 'leagueoflegends/jg.png',
  MID: 'leagueoflegends/mid.png',
  MIDDLE: 'leagueoflegends/mid.png',
  ADC: 'leagueoflegends/adc.png',
  BOTTOM: 'leagueoflegends/adc.png',
  SUPPORT: 'leagueoflegends/spt.png',
  SUP: 'leagueoflegends/spt.png',
};

const VALORANT_ROLE_IMAGES: Record<string, string> = {
  DUELIST: 'valorant-roles/duelist.png',
  SCOUT: 'valorant-roles/scout.png',
  STRATEGIST: 'valorant-roles/strategist.png',
  WATCHER: 'valorant-roles/watcher.png',
};

const OVERWATCH_ROLE_IMAGES: Record<string, string> = {
  TANK: 'overwatch-roles/role-tank.png',
  DAMAGE: 'overwatch-roles/role-damage.png',
  SUPPORT: 'overwatch-roles/role-support.png',
};

const PUBG_TEAM_IMAGES: Record<string, string> = {
  DUO: 'pubg-team/team-duo.png',
  SQUAD: 'pubg-team/team-squad.png',
  ALL: 'pubg-team/team-all.png',
};

const CS2_ROLE_IMAGES: Record<string, string> = {
  ALL: 'cs2-roles/role-all.png',
  ENTRY: 'cs2-roles/role-entry.png',
  SUPPORT: 'cs2-roles/role-support.png',
  IGL: 'cs2-roles/role-igl.png',
  AWPER: 'cs2-roles/role-awper.png',
  LURKER: 'cs2-roles/role-lurker.png',
};

const GAME_IMAGE_MAP: Record<string, Record<string, string>> = {
  LEAGUE_OF_LEGENDS: LEAGUE_POSITION_IMAGES,
  VALORANT: VALORANT_ROLE_IMAGES,
  OVERWATCH: OVERWATCH_ROLE_IMAGES,
  PUBG: PUBG_TEAM_IMAGES,
  COUNTER_STRIKE_2: CS2_ROLE_IMAGES,
};

function normalizeKey(pos: string | null | undefined): string | null {
  if (pos == null || pos === '') return null;
  return pos.trim().toUpperCase().replace(/\s+/g, '_');
}

export function getPositionImagePath(
  game: string | null | undefined,
  position: string | null | undefined,
): string | null {
  const gameKey = game?.trim().toUpperCase() || '';
  const posKey = normalizeKey(position);
  if (!posKey) return null;

  // LoL은 공식 포지션 아이콘(SVG)을 우선 사용
  // - 기존 로컬 PNG는 유지(만약 외부 리소스 실패 시, PositionIcon에서 자동 fallback 가능)
  if (gameKey === 'LEAGUE_OF_LEGENDS') {
    const k = posKey as keyof typeof LOL_OFFICIAL_ROLE_ICON_URL_BY_KEY;
    if (k in LOL_OFFICIAL_ROLE_ICON_URL_BY_KEY) return LOL_OFFICIAL_ROLE_ICON_URL_BY_KEY[k];
  }

  const map = GAME_IMAGE_MAP[gameKey];
  if (!map) return null;
  const path = map[posKey];
  return path ? `${IMAGE_BASE}/${path}` : null;
}
