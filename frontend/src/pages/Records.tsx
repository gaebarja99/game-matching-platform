import { useEffect, useMemo, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import Layout from '../components/Layout';
import { searchPlayer, type PlayerSearchResponse } from '../api/search';
import './Records.css';

type GameOption = {
  id: string;
  label: string;
  short: string;
  accent: string;
  banner: string;
  eyebrow: string;
  summary: string;
  placeholders: {
    nickname: string;
    tag?: string;
  };
  tagLabel?: string;
  needsTag?: boolean;
  regionOptions?: Array<{ value: string; label: string }>;
  platformOptions?: Array<{ value: string; label: string }>;
};

const GAMES: GameOption[] = [
  {
    id: 'lol',
    label: '리그 오브 레전드',
    short: 'LoL',
    accent: '#5383e8',
    banner: 'banner-lol',
    eyebrow: 'Riot Games',
    summary: '게임명과 태그를 입력하면 최근 20경기 전적을 확인할 수 있습니다.',
    placeholders: { nickname: '게임명', tag: 'KR1' },
    tagLabel: '태그',
    needsTag: true,
  },
  {
    id: 'tft',
    label: '전략적 팀 전투',
    short: 'TFT',
    accent: '#8b5cf6',
    banner: 'banner-tft',
    eyebrow: 'Teamfight Tactics',
    summary: '게임명과 태그를 입력하면 최근 경기와 순위를 확인할 수 있습니다.',
    placeholders: { nickname: '게임명', tag: 'KR1' },
    tagLabel: '태그',
    needsTag: true,
  },
  {
    id: 'valorant',
    label: '발로란트',
    short: 'VAL',
    accent: '#ff4655',
    banner: 'banner-valorant',
    eyebrow: 'Valorant',
    summary: '플레이어명과 태그를 입력하면 최근 20경기 전적을 확인할 수 있습니다.',
    placeholders: { nickname: '플레이어명', tag: 'KR1' },
    tagLabel: '태그',
    needsTag: true,
    regionOptions: [
      { value: 'kr', label: 'Korea' },
      { value: 'ap', label: 'Asia Pacific' },
      { value: 'na', label: 'North America' },
      { value: 'eu', label: 'Europe' },
      { value: 'latam', label: 'LATAM' },
      { value: 'br', label: 'Brazil' },
    ],
  },
  {
    id: 'pubg',
    label: 'PUBG',
    short: 'PUBG',
    accent: '#f59e0b',
    banner: 'banner-pubg',
    eyebrow: 'Battlegrounds',
    summary: '닉네임과 플랫폼을 입력하면 최근 전적을 확인할 수 있습니다.',
    placeholders: { nickname: 'Steam 또는 Kakao 닉네임' },
    platformOptions: [
      { value: 'steam', label: 'Steam' },
      { value: 'kakao', label: 'Kakao' },
    ],
  },
  {
    id: 'overwatch',
    label: '오버워치 2',
    short: 'OW2',
    accent: '#f97316',
    banner: 'banner-overwatch',
    eyebrow: 'Overwatch 2',
    summary: '배틀태그 이름과 숫자 태그를 입력하면 최근 전적을 확인할 수 있습니다.',
    placeholders: { nickname: 'BattleTag 이름', tag: '1234' },
    tagLabel: '배틀태그',
    needsTag: true,
  },
  {
    id: 'cs2',
    label: '카운터 스트라이크 2',
    short: 'CS2',
    accent: '#22c55e',
    banner: 'banner-cs2',
    eyebrow: 'Counter-Strike 2',
    summary: 'Steam64 ID 또는 Vanity URL로 최근 전적을 확인할 수 있습니다.',
    placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' },
  },
];

const QUICK_HINTS = [
  '게임명과 태그를 정확히 입력해 주세요.',
  '게임별 검색 조건이 조금씩 다를 수 있습니다.',
  '최근 20경기 기준으로 결과를 보여줍니다.',
];

function formatSearchError(gameLabel: string, message: string) {
  const normalized = message.trim();

  if (normalized.includes('Account not found')) {
    return {
      title: `${gameLabel} 계정을 찾지 못했습니다`,
      summary: '입력한 게임명이나 태그가 실제 계정 정보와 일치하지 않습니다.',
      hints: ['게임명과 태그를 다시 확인해 주세요.', '공백과 숫자 태그를 정확히 입력해 주세요.'],
    };
  }

  if (normalized.includes('404')) {
    return {
      title: '검색 결과가 없습니다',
      summary: '입력한 계정을 현재 API에서 찾지 못했습니다.',
      hints: ['이름, 태그, 지역 또는 플랫폼을 다시 확인해 주세요.'],
    };
  }

  if (normalized.includes('403')) {
    return {
      title: '공개 설정 확인이 필요합니다',
      summary: '해당 계정의 전적 또는 프로필이 비공개일 수 있습니다.',
      hints: ['게임 내 공개 설정을 확인한 뒤 다시 시도해 주세요.'],
    };
  }

  if (normalized.includes('429')) {
    return {
      title: '요청이 잠시 많습니다',
      summary: '전적 API 요청 제한에 걸렸습니다.',
      hints: ['잠시 후 다시 검색해 주세요.'],
    };
  }

  return {
    title: '검색에 실패했습니다',
    summary: '전적 정보를 불러오는 중 오류가 발생했습니다.',
    hints: ['잠시 후 다시 시도해 주세요.', '같은 문제가 계속되면 입력값을 다시 확인해 주세요.'],
  };
}

function formatWinRate(value: number | null | undefined) {
  if (value == null) return '-';
  const normalized = value > 1 ? value : value * 100;
  return `${normalized.toFixed(0)}%`;
}

function formatPlayedAt(value?: string | null) {
  if (!value) return '';
  const parsed = new Date(value);
  if (!Number.isNaN(parsed.getTime())) {
    return parsed.toLocaleString('ko-KR', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
  return value;
}

function getQueueCategory(gameMode?: string | null) {
  const mode = (gameMode || '').toUpperCase();

  if (mode.includes('RANKED')) return { label: '랭크', tone: 'ranked' as const };
  if (mode.includes('NORMAL') || mode.includes('QUICKPLAY') || mode.includes('CLASSIC')) return { label: '일반', tone: 'normal' as const };
  if (mode.includes('ARAM')) return { label: '칼바람', tone: 'aram' as const };
  if (mode.includes('CLASH')) return { label: '클래시', tone: 'clash' as const };
  return { label: gameMode || '기타', tone: 'other' as const };
}

function getInitials(name?: string | null) {
  if (!name) return '?';
  return name.slice(0, 1).toUpperCase();
}

function SearchHitPanel({ result, gameLabel, accent }: { result: PlayerSearchResponse; gameLabel: string; accent: string }) {
  const info = result.playerInfo ?? {};
  const name = info.gameName || result.nickname || '플레이어';
  const subline = [
    info.tagLine ? `#${info.tagLine}` : null,
    info.tier ? `${info.tier} ${info.rank || ''}`.trim() : null,
    info.lp || null,
    info.summonerLevel ? `Lv.${info.summonerLevel}` : null,
  ].filter(Boolean).join(' • ');

  return (
    <section className="records-search-hit-panel" style={{ ['--records-accent' as string]: accent }}>
      <div className="records-search-hit-head">
        <strong>Player Profile</strong>
        <span>{gameLabel}</span>
      </div>
      <button type="button" className="records-search-hit-row">
        {info.avatarUrl ? (
          <img src={info.avatarUrl} alt="avatar" className="records-search-hit-avatar" />
        ) : (
          <div className="records-search-hit-avatar records-search-hit-avatar-fallback">{getInitials(name)}</div>
        )}
        <div className="records-search-hit-copy">
          <div className="records-search-hit-name">{name}</div>
          <div className="records-search-hit-subline">{subline || '최근 전적 보기'}</div>
        </div>
        <div className="records-search-hit-action">전적 보기</div>
      </button>
    </section>
  );
}

function MatchRow({ match }: { match: NonNullable<PlayerSearchResponse['matches']>[number] }) {
  const duration = match.playtime ? `${Math.floor(match.playtime / 60)}분` : null;
  const queueCategory = getQueueCategory(match.gameMode);
  const playedAt = formatPlayedAt(match.playedAt);
  const extras = Object.entries(match.extras || {}).slice(0, 2);

  return (
    <div className={`records-opgg-match ${match.win ? 'is-win' : 'is-loss'}`}>
      <div className="records-opgg-match-result">
        <span className={match.win ? 'records-win-badge' : 'records-loss-badge'}>{match.win ? '승리' : '패배'}</span>
        <span className={`records-queue-badge is-${queueCategory.tone}`}>{queueCategory.label}</span>
        <span className="records-opgg-match-time">{playedAt || '-'}</span>
      </div>

      <div className="records-opgg-match-champion">
        <div className="records-opgg-champion-thumb">{getInitials(match.champion || match.agent)}</div>
        <div className="records-opgg-champion-copy">
          <strong>{match.champion || match.agent || '전적 정보 없음'}</strong>
          <span>{match.gameMode || '일반 매치'}</span>
        </div>
      </div>

      <div className="records-opgg-match-score">
        <strong>{match.kills ?? 0} / {match.deaths ?? 0} / {match.assists ?? 0}</strong>
        <span>KDA {match.kda != null ? Number(match.kda).toFixed(2) : '-'}</span>
      </div>

      <div className="records-opgg-match-stats">
        {duration ? <span className="records-meta-chip">{duration}</span> : null}
        {match.cs != null ? <span className="records-meta-chip">CS {match.cs}</span> : null}
        {extras.map(([key, value]) => (
          <span key={key} className="records-meta-chip">{key}: {String(value)}</span>
        ))}
      </div>
    </div>
  );
}

function ResultPanel({ result, accent, title }: { result: PlayerSearchResponse; accent: string; title: string }) {
  const info = result.playerInfo ?? {};
  const stats = result.stats ?? {};
  const matches = result.matches ?? [];
  const hasMatches = matches.length > 0;
  const wins = matches.filter((match) => match.win).length;
  const losses = matches.length - wins;
  const topPicks = Object.entries(
    matches.reduce<Record<string, number>>((acc, match) => {
      const key = match.champion || match.agent;
      if (key) acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {}),
  ).sort((a, b) => b[1] - a[1]).slice(0, 3);

  return (
    <section className="records-result-panel records-opgg-result" style={{ ['--records-accent' as string]: accent }}>
      <div className="records-opgg-profile">
        <div className="records-opgg-profile-main">
          {info.avatarUrl ? (
            <img src={info.avatarUrl} alt="avatar" className="records-avatar" />
          ) : (
            <div className="records-avatar-fallback">{getInitials(info.gameName || result.nickname)}</div>
          )}
          <div className="records-opgg-profile-copy">
            <div className="records-result-label">{title}</div>
            <h2 className="records-profile-name">
              {info.gameName || result.nickname || '플레이어 정보 없음'}
              {info.tagLine ? <span className="records-tag-line">#{info.tagLine}</span> : null}
            </h2>
            <div className="records-rank-row">
              {info.tier ? <span className="records-rank-badge">{`${info.tier} ${info.rank || ''}`.trim()}</span> : null}
              {info.lp ? <span className="records-soft-badge">{info.lp}</span> : null}
              {info.summonerLevel ? <span className="records-soft-badge">Lv.{info.summonerLevel}</span> : null}
            </div>
          </div>
        </div>

        <div className="records-opgg-overview">
          <div className="records-opgg-overview-card">
            <span>최근 20경기</span>
            <strong>{hasMatches ? `${wins}승 ${losses}패` : '전적 없음'}</strong>
            <em>{hasMatches ? `승률 ${formatWinRate(wins / matches.length)}` : '매치 데이터를 불러오지 못했습니다.'}</em>
          </div>
          <div className="records-opgg-overview-card">
            <span>평균 KDA</span>
            <strong>{hasMatches && stats.avgKda != null ? Number(stats.avgKda).toFixed(2) : '-'}</strong>
            <em>
              {hasMatches && stats.avgKills != null ? Number(stats.avgKills).toFixed(1) : '-'} / {hasMatches && stats.avgDeaths != null ? Number(stats.avgDeaths).toFixed(1) : '-'} / {hasMatches && stats.avgAssists != null ? Number(stats.avgAssists).toFixed(1) : '-'}
            </em>
          </div>
          <div className="records-opgg-overview-card">
            <span>주요 픽</span>
            <strong>{hasMatches ? (stats.mostUsedChampionOrAgent || '-') : '전적 없음'}</strong>
            <em>{hasMatches ? (topPicks.map(([name, count]) => `${name} ${count}회`).join(' • ') || '데이터 없음') : '최근 매치 데이터가 없습니다.'}</em>
          </div>
        </div>
      </div>

      <div className="records-opgg-match-panel">
        <div className="records-section-head">
          <h3>최근 매치</h3>
          <span>{matches.length}게임</span>
        </div>
        {matches.length ? (
          <div className="records-opgg-match-list">
            {matches.map((match, index) => (
              <MatchRow key={match.matchId || `${index}-${match.gameMode || 'match'}`} match={match} />
            ))}
          </div>
        ) : (
          <p className="records-empty-matches">계정은 확인되었지만 최근 매치 데이터는 아직 조회되지 않았습니다.</p>
        )}
      </div>
    </section>
  );
}

export default function Records() {
  const resultRef = useRef<HTMLDivElement | null>(null);
  const [gameId, setGameId] = useState('lol');
  const [nickname, setNickname] = useState('');
  const [tagLine, setTagLine] = useState('');
  const [region, setRegion] = useState('kr');
  const [platform, setPlatform] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PlayerSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);
  const formattedError = useMemo(() => (error ? formatSearchError(game.label, error) : null), [error, game.label]);
  const searchbarClassName = useMemo(() => {
    const hasRegion = Boolean(game.regionOptions?.length);
    const hasPlatform = Boolean(game.platformOptions?.length);
    if (hasRegion) return 'records-opgg-searchbar fields-4';
    if (hasPlatform || game.needsTag) return 'records-opgg-searchbar fields-3';
    return 'records-opgg-searchbar fields-2';
  }, [game]);

  useEffect(() => {
    setRegion(game.regionOptions?.[0]?.value || 'kr');
    setPlatform(game.platformOptions?.[0]?.value || '');
    setTagLine('');
  }, [game]);

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!nickname.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await searchPlayer({
        game: gameId,
        gameName: nickname.trim(),
        tagLine: game.needsTag ? tagLine.trim() || undefined : undefined,
        region: game.regionOptions?.length ? region : 'kr',
        platform: game.platformOptions?.length ? platform : undefined,
        count: 20,
      });

      setResult(response);
      if (!response.success) {
        setError(response.errorMessage || '검색에 실패했습니다.');
      } else {
        window.setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 120);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="records-search-page">
        <section className="records-opgg-hero" style={{ ['--records-accent' as string]: game.accent }}>
          <div className="records-opgg-topline">
            <span className="records-opgg-brand">전적 검색</span>
            <span className="records-opgg-current-game">{game.label}</span>
          </div>

          <div className="records-opgg-body">
            <div className="records-opgg-copy">
              <p className="records-kicker">GAME MATCHER SEARCH</p>
              <h1 className="records-title">전적을 빠르게 검색해보세요</h1>
              <p className="records-description">{game.summary}</p>
              <div className="records-hint-list">
                {QUICK_HINTS.map((hint) => (
                  <span key={hint} className="records-hint-chip">{hint}</span>
                ))}
              </div>
            </div>

            <form className="records-opgg-search" onSubmit={handleSearch}>
              <div className={searchbarClassName}>
                <label className="records-opgg-field records-opgg-field-game">
                  <span>게임</span>
                  <select value={gameId} onChange={(event) => { setGameId(event.target.value); setResult(null); setError(null); }}>
                    {GAMES.map((item) => (
                      <option key={item.id} value={item.id}>{item.label}</option>
                    ))}
                  </select>
                </label>

                <label className="records-opgg-field records-opgg-field-name">
                  <span>검색</span>
                  <input type="text" value={nickname} onChange={(event) => setNickname(event.target.value)} placeholder={game.placeholders.nickname} autoComplete="off" required />
                </label>

                {game.needsTag ? (
                  <label className="records-opgg-field records-opgg-field-tag">
                    <span>{game.tagLabel || '태그'}</span>
                    <input type="text" value={tagLine} onChange={(event) => setTagLine(event.target.value)} placeholder={game.placeholders.tag || 'KR1'} autoComplete="off" />
                  </label>
                ) : null}

                {game.regionOptions ? (
                  <label className="records-opgg-field records-opgg-field-small">
                    <span>지역</span>
                    <select value={region} onChange={(event) => setRegion(event.target.value)}>
                      {game.regionOptions.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>
                ) : null}

                {game.platformOptions ? (
                  <label className="records-opgg-field records-opgg-field-small">
                    <span>플랫폼</span>
                    <select value={platform} onChange={(event) => setPlatform(event.target.value)}>
                      {game.platformOptions.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>
                ) : null}

                <button type="submit" className="records-opgg-submit" disabled={loading}>{loading ? '검색 중...' : '검색'}</button>
              </div>

              <div className="records-opgg-bottom">
                <p className="records-inline-hint">{game.summary}</p>
                {(nickname || tagLine || result || error) ? (
                  <button type="button" className="records-clear-button" onClick={() => { setNickname(''); setTagLine(''); setError(null); setResult(null); }}>
                    초기화
                  </button>
                ) : null}
              </div>
            </form>
          </div>
        </section>

        <section className="records-game-tabs" aria-label="지원 게임">
          {GAMES.map((item) => (
            <button key={item.id} type="button" onClick={() => { setGameId(item.id); setResult(null); setError(null); }} className={`records-game-pill ${item.id === gameId ? 'is-active' : ''}`}>
              <span className={`records-game-poster ${item.banner}`}>
                <span className="records-game-overlay" />
                <span className="records-game-short">{item.short}</span>
                <span className="records-game-eyebrow">{item.eyebrow}</span>
                <span className="records-game-label">{item.label}</span>
              </span>
            </button>
          ))}
        </section>

        {error ? (
          <section className="records-error-box">
            <strong className="records-error-title">{formattedError?.title || '검색 실패'}</strong>
            <p className="records-error-summary">{formattedError?.summary}</p>
            {formattedError?.hints.map((line) => (
              <p key={line} className="records-error-hint">{line}</p>
            ))}
          </section>
        ) : null}

        {result?.success ? (
          <div ref={resultRef}>
            <SearchHitPanel result={result} accent={game.accent} gameLabel={game.label} />
            <ResultPanel result={result} accent={game.accent} title={`${game.label} 전적`} />
          </div>
        ) : null}
      </div>
    </Layout>
  );
}
