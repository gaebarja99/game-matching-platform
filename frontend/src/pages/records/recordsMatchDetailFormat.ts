/**
 * 전적 검색 매치 상세 API payload → 화면용 요약 (게임별)
 * 백엔드 RecordsMatchDetailService가 내려주는 구조(스네이크/카멜 혼용 가능)에 맞춤
 */

export type MatchDetailBlock =
  | { kind: 'kv'; title: string; items: [string, string][] }
  | {
      kind: 'table';
      title: string;
      headers: string[];
      rows: string[][];
      highlightRow?: number;
      /** 행마다 팀 색 (발로란트 등). 없으면 기본 배경만 사용 */
      rowTeamTint?: Array<'blue' | 'red' | undefined>;
    };

export type FormattedMatchDetail = {
  /** 맵·모드·라운드 결과 등 */
  matchBlocks: MatchDetailBlock[];
  /** 참가자 표 등 */
  playerBlocks: MatchDetailBlock[];
  /** AI 분석 (payload.records_ai_evaluation 등) */
  aiBlocks: MatchDetailBlock[];
};

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function num(v: unknown): number | undefined {
  if (typeof v === 'number' && !Number.isNaN(v)) return v;
  if (typeof v === 'string' && v.trim() !== '' && !Number.isNaN(Number(v))) return Number(v);
  return undefined;
}

function firstStr(obj: Record<string, unknown>, keys: string[]): string | undefined {
  for (const k of keys) {
    const v = obj[k];
    if (v != null && String(v).trim() !== '') return String(v);
  }
  return undefined;
}

function extractAiBlocks(payload: Record<string, unknown>): MatchDetailBlock[] {
  const aiKeys = ['records_ai_evaluation', 'recordsAiEvaluation', 'ai_evaluation', 'aiEvaluation'] as const;
  let raw: Record<string, unknown> | undefined;
  for (const k of aiKeys) {
    if (!Object.prototype.hasOwnProperty.call(payload, k)) continue;
    const v = payload[k];
    if (v === null || v === undefined) return [];
    if (isRecord(v)) {
      raw = v;
      break;
    }
  }
  if (!raw) return [];
  const items: [string, string][] = [];
  const model = firstStr(raw, ['llmModel', 'llm_model']);
  const status = firstStr(raw, ['status']);
  const grade = firstStr(raw, ['grade']);
  const scoreRaw = raw.score;
  if (model) items.push(['모델', model]);
  if (status) items.push(['상태', status]);
  if (grade) items.push(['등급', grade]);
  if (scoreRaw != null && String(scoreRaw).trim() !== '') items.push(['점수', String(scoreRaw)]);
  const summary = firstStr(raw, ['summary']);
  const detailed = firstStr(raw, ['detailed_comment', 'detailedComment']);
  if (summary) items.push(['요약', summary]);
  if (detailed) items.push(['상세', detailed]);
  if (!items.length) return [];
  return [{ kind: 'kv', title: '', items }];
}

function withAi(
  blocks: { matchBlocks: MatchDetailBlock[]; playerBlocks: MatchDetailBlock[] },
  payload: Record<string, unknown>,
): FormattedMatchDetail {
  return {
    matchBlocks: blocks.matchBlocks,
    playerBlocks: blocks.playerBlocks,
    aiBlocks: extractAiBlocks(payload),
  };
}

function formatValorant(payload: Record<string, unknown>, puuid?: string): FormattedMatchDetail {
  const matchBlocks: MatchDetailBlock[] = [];
  const playerBlocks: MatchDetailBlock[] = [];
  const meta = isRecord(payload.metadata) ? payload.metadata : {};
  const kv: [string, string][] = [];
  const add = (label: string, value: string | undefined) => {
    if (value != null && value !== '') kv.push([label, value]);
  };
  add('맵', firstStr(meta, ['map']));
  add('모드', firstStr(meta, ['mode', 'queue']));
  add('지역', firstStr(meta, ['region', 'cluster']));
  const rounds = num(meta.rounds_played);
  if (rounds != null) add('라운드 수', String(rounds));
  const gl = num(meta.game_length);
  if (gl != null) add('플레이 시간', `${Math.max(1, Math.round(gl / 60))}분`);
  const start = firstStr(meta, ['game_start_patched', 'game_start']);
  add('시작', start);
  if (kv.length) matchBlocks.push({ kind: 'kv', title: '매치 정보', items: kv });

  const teams = isRecord(payload.teams) ? payload.teams : {};
  const redT = isRecord(teams.red) ? (teams.red as Record<string, unknown>) : {};
  const blueT = isRecord(teams.blue) ? (teams.blue as Record<string, unknown>) : {};
  const redWon = num(redT.rounds_won);
  const blueWon = num(blueT.rounds_won);
  if (redWon != null && blueWon != null) {
    let line = `Red ${redWon} : Blue ${blueWon}`;
    if (redWon === blueWon) {
      const rWin = redT.has_won ?? redT.hasWon;
      const bWin = blueT.has_won ?? blueT.hasWon;
      if (rWin === true) line += ' (Red 승)';
      else if (bWin === true) line += ' (Blue 승)';
      else line += ' (동점)';
    }
    matchBlocks.push({ kind: 'kv', title: '라운드 결과', items: [['전적', line]] });
  }

  const playersRoot = isRecord(payload.players) ? payload.players : {};
  const allRaw = playersRoot.all_players ?? playersRoot.allPlayers;
  const list = Array.isArray(allRaw) ? allRaw : [];
  const roundsPlayed = num(meta.rounds_played);
  const headers = ['플레이어', '팀', '에이전트', 'K', 'D', 'A', 'ACS', '피해량'];
  const rows: string[][] = [];
  const rowTeamTint: Array<'blue' | 'red' | undefined> = [];
  let highlightRow: number | undefined;
  const sorted = [...list].sort((a, b) => {
    if (!isRecord(a) || !isRecord(b)) return 0;
    const sa = num(isRecord(a.stats) ? (a.stats as Record<string, unknown>).score : undefined) ?? 0;
    const sb = num(isRecord(b.stats) ? (b.stats as Record<string, unknown>).score : undefined) ?? 0;
    return sb - sa;
  });
  sorted.forEach((raw) => {
    if (!isRecord(raw)) return;
    const st = isRecord(raw.stats) ? raw.stats : {};
    const name = String(raw.name ?? '?');
    const tag = String(raw.tag ?? '');
    const display = tag ? `${name}#${tag}` : name;
    const agent = String(raw.character ?? raw.agent ?? '–');
    const team = String(raw.team ?? '–');
    const pid = String(raw.puuid ?? '');
    const teamKey = team.trim().toLowerCase();
    rowTeamTint.push(teamKey === 'blue' ? 'blue' : teamKey === 'red' ? 'red' : undefined);
    const totalScore = num(st.score);
    let acsCell = '–';
    if (totalScore != null && roundsPlayed != null && roundsPlayed > 0) {
      acsCell = (totalScore / roundsPlayed).toFixed(1);
    }
    rows.push([
      display,
      team,
      agent,
      String(st.kills ?? '–'),
      String(st.deaths ?? '–'),
      String(st.assists ?? '–'),
      acsCell,
      String(raw.damage_made ?? raw.damageMade ?? '–'),
    ]);
    if (puuid && pid && pid === puuid) highlightRow = rows.length - 1;
  });
  if (rows.length) {
    playerBlocks.push({
      kind: 'table',
      title: '',
      headers,
      rows,
      highlightRow,
      rowTeamTint,
    });
  }

  return withAi({ matchBlocks, playerBlocks }, payload);
}

function formatLol(payload: Record<string, unknown>, puuid?: string): FormattedMatchDetail {
  const matchBlocks: MatchDetailBlock[] = [];
  const playerBlocks: MatchDetailBlock[] = [];
  const info = isRecord(payload.info) ? payload.info : {};
  const kv: [string, string][] = [];
  const mode = firstStr(info, ['gameMode', 'game_mode']);
  if (mode) kv.push(['게임 모드', mode]);
  const dur = num(info.gameDuration);
  if (dur != null) kv.push(['플레이 시간', `${Math.max(1, Math.round(dur / 60))}분`]);
  if (kv.length) matchBlocks.push({ kind: 'kv', title: '매치 정보', items: kv });

  const parts = Array.isArray(info.participants) ? info.participants : [];
  const headers = ['소환사', '챔피언', 'K / D / A', '결과'];
  const rows: string[][] = [];
  let highlightRow: number | undefined;
  parts.forEach((p) => {
    if (!isRecord(p)) return;
    const pid = String(p.puuid ?? '');
    const name = String(p.riotIdGameName ?? p.summonerName ?? '?');
    const tag = String(p.riotIdTagline ?? '');
    const label = tag ? `${name}#${tag}` : name;
    const champ = String(p.championName ?? '–');
    const k = p.kills ?? 0;
    const d = p.deaths ?? 0;
    const a = p.assists ?? 0;
    const win = p.win === true ? '승' : p.win === false ? '패' : '–';
    rows.push([label, champ, `${k} / ${d} / ${a}`, win]);
    if (puuid && pid === puuid) highlightRow = rows.length - 1;
  });
  if (rows.length) playerBlocks.push({ kind: 'table', title: '', headers, rows, highlightRow });
  return withAi({ matchBlocks, playerBlocks }, payload);
}

function formatTft(payload: Record<string, unknown>, puuid?: string): FormattedMatchDetail {
  const matchBlocks: MatchDetailBlock[] = [];
  const playerBlocks: MatchDetailBlock[] = [];
  const info = isRecord(payload.info) ? payload.info : {};
  const kv: [string, string][] = [];
  const gl = num(info.game_length ?? info.gameLength);
  if (gl != null) kv.push(['길이', `${Math.round(gl)}초`]);
  if (kv.length) matchBlocks.push({ kind: 'kv', title: '매치 정보', items: kv });

  const parts = Array.isArray(info.participants) ? info.participants : [];
  const headers = ['순위', '플레이어', '레벨', '라운드', '딜량'];
  const rows: string[][] = [];
  let highlightRow: number | undefined;
  const sorted = [...parts]
    .filter(isRecord)
    .sort((a, b) => (num(a.placement) ?? 999) - (num(b.placement) ?? 999));
  sorted.forEach((p) => {
    const pid = String(p.puuid ?? '');
    const name = String(p.riotIdGameName ?? '?');
    const tag = String(p.riotIdTagline ?? '');
    const label = tag ? `${name}#${tag}` : name;
    rows.push([
      String(p.placement ?? '–'),
      label,
      String(p.level ?? '–'),
      String(p.last_round ?? p.lastRound ?? '–'),
      String(p.total_damage_to_players ?? p.totalDamageToPlayers ?? '–'),
    ]);
    if (puuid && pid === puuid) highlightRow = rows.length - 1;
  });
  if (rows.length) playerBlocks.push({ kind: 'table', title: '', headers, rows, highlightRow });
  return withAi({ matchBlocks, playerBlocks }, payload);
}

function formatPubg(payload: Record<string, unknown>): FormattedMatchDetail {
  const matchBlocks: MatchDetailBlock[] = [];
  const playerBlocks: MatchDetailBlock[] = [];
  const data = isRecord(payload.data) ? payload.data : {};
  const attr = isRecord(data.attributes) ? data.attributes : {};
  const kv: [string, string][] = [];
  const mapName = firstStr(attr, ['mapName']);
  if (mapName) kv.push(['맵', mapName]);
  const mode = firstStr(attr, ['gameMode', 'matchType']);
  if (mode) kv.push(['모드', mode]);
  const du = num(attr.duration);
  if (du != null) kv.push(['시간', `${Math.max(1, Math.round(du / 60))}분`]);
  const shard = firstStr(attr, ['shardId']);
  if (shard) kv.push(['샤드', shard]);
  if (kv.length) matchBlocks.push({ kind: 'kv', title: '매치 요약', items: kv });
  playerBlocks.push({
    kind: 'kv',
    title: '참가 로스터',
    items: [['안내', 'PUBG는 스쿼드·킬 등 세부가 많아 이 화면에서는 매치 요약만 표시합니다.']],
  });
  return withAi({ matchBlocks, playerBlocks }, payload);
}

/**
 * 전적 목록 행의 gameMode가 비어 있을 때, 상세 API payload로 표시 문자열 보강.
 */
export function extractValorantRowModeFromDetailPayload(
  payload: Record<string, unknown> | null | undefined,
): string | undefined {
  if (!payload || !isRecord(payload)) return undefined;
  const meta = payload.metadata;
  if (!isRecord(meta)) return undefined;
  const fromMeta = firstStr(meta, ['mode', 'queue']);
  if (fromMeta?.trim()) return fromMeta.trim();
  const mapName = firstStr(meta, ['map']);
  if (mapName) {
    const lower = mapName.trim().toLowerCase();
    if (lower.startsWith('skirmish')) return 'Skirmish';
    if (lower.includes('deathmatch')) return 'Deathmatch';
  }
  return undefined;
}

export function formatRecordsMatchDetail(
  gameId: string,
  payload: unknown,
  ctx: { puuid?: string },
): FormattedMatchDetail | null {
  if (!isRecord(payload)) return null;
  switch (gameId) {
    case 'valorant':
      return formatValorant(payload, ctx.puuid);
    case 'lol':
      return formatLol(payload, ctx.puuid);
    case 'tft':
      return formatTft(payload, ctx.puuid);
    case 'pubg':
      return formatPubg(payload);
    default:
      return withAi(
        {
          matchBlocks: [{ kind: 'kv', title: '상세', items: [['형식', '지원 게임이 아닙니다.']] }],
          playerBlocks: [],
        },
        payload,
      );
  }
}
