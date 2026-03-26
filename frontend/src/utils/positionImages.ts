export const IMAGE_BASE = 'images';

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
  const map = GAME_IMAGE_MAP[gameKey];
  if (!map) return null;
  const path = map[posKey];
  return path ? `${IMAGE_BASE}/${path}` : null;
}
