import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import Layout from '../components/Layout';
import { searchPlayer, type PlayerSearchResponse } from '../api/search';
import './Records.css';

type GameOption = {
  id: string;
  label: string;
  short: string;
  accent: string;
  fields: string[];
  placeholders: {
    nickname: string;
    tag?: string;
  };
  hint: string;
  tagLabel?: string;
  regionOptions?: { value: string; label: string }[];
  platformOptions?: { value: string; label: string }[];
};

const GAMES: GameOption[] = [
  { id: 'lol', label: 'League of Legends', short: 'LoL', accent: '#2f80ed', fields: ['nickname', 'tag', 'count'], placeholders: { nickname: '소환사명', tag: 'KR1' }, hint: '라이엇 계정 기준으로 닉네임과 태그를 입력하면 최근 전적을 조회합니다.', tagLabel: '태그' },
  { id: 'tft', label: 'Teamfight Tactics', short: 'TFT', accent: '#7c5cff', fields: ['nickname', 'tag', 'count'], placeholders: { nickname: '닉네임', tag: 'KR1' }, hint: 'TFT도 닉네임과 태그 기준으로 최근 매치를 불러옵니다.', tagLabel: '태그' },
  { id: 'valorant', label: 'Valorant', short: 'VAL', accent: '#ff4d67', fields: ['nickname', 'tag', 'valorant_region', 'count'], placeholders: { nickname: '플레이어명', tag: 'KR1' }, hint: '닉네임, 태그, 지역을 선택해 발로란트 전적을 검색합니다.', tagLabel: '태그', regionOptions: [{ value: 'kr', label: 'Korea' }, { value: 'ap', label: 'Asia Pacific' }, { value: 'na', label: 'North America' }, { value: 'eu', label: 'Europe' }, { value: 'latam', label: 'LATAM' }, { value: 'br', label: 'Brazil' }] },
  { id: 'pubg', label: 'PUBG', short: 'PUBG', accent: '#f0b429', fields: ['nickname', 'pubg_platform'], placeholders: { nickname: 'Steam 또는 Kakao 닉네임' }, hint: 'PUBG는 플랫폼을 같이 선택해야 정확히 검색됩니다.', platformOptions: [{ value: 'steam', label: 'Steam' }, { value: 'kakao', label: 'Kakao' }] },
  { id: 'overwatch', label: 'Overwatch 2', short: 'OW2', accent: '#ff9b3d', fields: ['nickname', 'tag'], placeholders: { nickname: 'BattleTag 이름', tag: '1234' }, hint: '오버워치는 BattleTag 이름과 숫자 태그 조합으로 검색합니다.', tagLabel: '배틀태그' },
  { id: 'apex', label: 'Apex Legends', short: 'APEX', accent: '#ff6f61', fields: ['nickname', 'apex_platform'], placeholders: { nickname: 'EA 또는 Origin 닉네임' }, hint: '에이펙스는 플랫폼에 따라 검색 결과가 달라질 수 있습니다.', platformOptions: [{ value: 'PC', label: 'PC' }, { value: 'PS4', label: 'PlayStation' }, { value: 'X1', label: 'Xbox' }] },
  { id: 'cs2', label: 'Counter-Strike 2', short: 'CS2', accent: '#61b15a', fields: ['nickname'], placeholders: { nickname: 'Steam64 ID 또는 Vanity URL' }, hint: 'CS2는 Steam 식별자를 기준으로 검색합니다.' },
];

function formatWinRate(value: number | null | undefined) {
  if (value == null) return '-';
  const normalized = value > 1 ? value : value * 100;
  return `${normalized.toFixed(0)}%`;
}

function getInitials(name?: string | null) {
  if (!name) return '?';
  return name.slice(0, 1).toUpperCase();
}

function MatchRow({ match }: { match: NonNullable<PlayerSearchResponse['matches']>[number] }) {
  const duration = match.playtime ? `${Math.floor(match.playtime / 60)}m` : null;

  return (
    <div className={`records-match-row ${match.win ? 'is-win' : 'is-loss'}`}>
      <div className="records-match-status">
        <span className={match.win ? 'records-win-badge' : 'records-loss-badge'}>{match.win ? 'WIN' : 'LOSS'}</span>
        <span className="records-match-mode">{match.gameMode || 'Mode'}</span>
      </div>
      <div className="records-match-center">
        <strong className="records-match-title">{match.champion || match.agent || 'Unknown'}</strong>
        {match.kills != null ? <span className="records-match-kda">{match.kills} / {match.deaths ?? 0} / {match.assists ?? 0}</span> : null}
      </div>
      <div className="records-match-meta">
        {duration ? <span className="records-meta-chip">{duration}</span> : null}
        {match.cs != null ? <span className="records-meta-chip">CS {match.cs}</span> : null}
        {match.extras ? Object.entries(match.extras).slice(0, 2).map(([key, value]) => <span key={key} className="records-meta-chip">{key}: {String(value)}</span>) : null}
      </div>
    </div>
  );
}

function ResultPanel({ result, accent, title }: { result: PlayerSearchResponse; accent: string; title: string }) {
  const info = result.playerInfo ?? {};
  const stats = result.stats ?? {};
  const statCards = [
    { label: '총 게임', value: stats.totalGames ?? '-' },
    { label: '승률', value: formatWinRate(stats.winRate) },
    { label: '승리', value: stats.wins ?? '-' },
    { label: '평균 KDA', value: stats.avgKda != null ? Number(stats.avgKda).toFixed(2) : '-' },
    { label: '주력 픽', value: stats.mostUsedChampionOrAgent || '-' },
  ];

  return (
    <section className="records-result-panel" style={{ ['--records-accent' as string]: accent }}>
      <div className="records-result-header">
        <div className="records-profile-block">
          {info.avatarUrl ? <img src={info.avatarUrl} alt="avatar" className="records-avatar" /> : <div className="records-avatar-fallback">{getInitials(info.gameName || result.nickname)}</div>}
          <div className="records-profile-text">
            <div className="records-result-label">{title}</div>
            <h2 className="records-profile-name">
              {info.gameName || result.nickname || 'Unknown Player'}
              {info.tagLine ? <span className="records-tag-line">#{info.tagLine}</span> : null}
            </h2>
            <div className="records-rank-row">
              {info.tier ? <span className="records-rank-badge">{info.tier} {info.rank || ''}</span> : null}
              {info.lp ? <span className="records-soft-badge">{info.lp}</span> : null}
              {info.summonerLevel ? <span className="records-soft-badge">Lv.{info.summonerLevel}</span> : null}
            </div>
          </div>
        </div>
        <div className="records-result-stamp">{result.game?.toUpperCase()}</div>
      </div>

      <div className="records-stat-grid">
        {statCards.map((item) => (
          <div key={item.label} className="records-stat-card">
            <span className="records-stat-label">{item.label}</span>
            <strong className="records-stat-value">{item.value}</strong>
          </div>
        ))}
      </div>

      <div className="records-result-body">
        <div className="records-section-card">
          <div className="records-section-head"><h3>플레이어 정보</h3></div>
          <dl className="records-info-list">
            <div><dt>게임명</dt><dd>{info.gameName || '-'}</dd></div>
            <div><dt>태그</dt><dd>{info.tagLine || '-'}</dd></div>
            <div><dt>티어</dt><dd>{info.tier ? `${info.tier} ${info.rank || ''}`.trim() : '-'}</dd></div>
            <div><dt>레벨</dt><dd>{info.summonerLevel || '-'}</dd></div>
          </dl>
        </div>

        <div className="records-section-card">
          <div className="records-section-head">
            <h3>최근 매치</h3>
            <span>{result.matches?.length || 0} games</span>
          </div>
          {result.matches?.length ? <div className="records-match-list">{result.matches.map((match, index) => <MatchRow key={match.matchId || `${index}-${match.gameMode || 'match'}`} match={match} />)}</div> : <p className="records-empty-matches">표시할 최근 전적이 없습니다.</p>}
        </div>
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
  const [count, setCount] = useState(5);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PlayerSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);

  useEffect(() => {
    if (game.platformOptions?.length) setPlatform(game.platformOptions[0].value);
    else setPlatform('');
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
        tagLine: game.fields.includes('tag') ? tagLine.trim() || undefined : undefined,
        region: game.fields.includes('valorant_region') ? region : 'kr',
        platform: game.fields.includes('pubg_platform') || game.fields.includes('apex_platform') ? platform : undefined,
        count: game.fields.includes('count') ? count : undefined,
      });
      setResult(response);
      if (!response.success) setError(response.errorMessage || '검색에 실패했습니다.');
      else window.setTimeout(() => resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 120);
    } catch (err) {
      setError(err instanceof Error ? err.message : '서버 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="records-search-page">
        <section className="records-hero">
          <div className="records-hero-copy">
            <p className="records-kicker">DUO-STYLE SEARCH</p>
            <h1 className="records-title">게임 전적 검색을 더 빠르고 선명하게</h1>
            <p className="records-description">게임을 선택하고 닉네임을 입력하면 최근 매치 기록과 요약 통계를 한 화면에서 확인할 수 있습니다.</p>
          </div>
          <div className="records-hero-card">
            <div className="records-hero-meta-top">
              <span className="records-hero-badge">Live Search</span>
              <span className="records-hero-subtle">{game.label}</span>
            </div>
            <strong className="records-hero-highlight">{game.hint}</strong>
          </div>
        </section>

        <section className="records-search-shell" style={{ ['--records-accent' as string]: game.accent }}>
          <div className="records-game-rail">
            {GAMES.map((item) => (
              <button key={item.id} type="button" onClick={() => { setGameId(item.id); setResult(null); setError(null); }} className={`records-game-pill ${item.id === gameId ? 'is-active' : ''}`}>
                <span className="records-game-short">{item.short}</span>
                <span className="records-game-label">{item.label}</span>
              </button>
            ))}
          </div>

          <form className="records-search-card" onSubmit={handleSearch}>
            <div className="records-card-header">
              <div>
                <p className="records-card-kicker">Search Center</p>
                <h2 className="records-card-title">{game.label}</h2>
              </div>
              <span className="records-card-accent-chip">{game.short}</span>
            </div>

            <div className="records-search-grid">
              <label className="records-primary-field">
                <span>닉네임</span>
                <input type="text" value={nickname} onChange={(event) => setNickname(event.target.value)} placeholder={game.placeholders.nickname} autoComplete="off" required />
              </label>

              {game.fields.includes('tag') ? (
                <label className="records-compact-field">
                  <span>{game.tagLabel || '태그'}</span>
                  <input type="text" value={tagLine} onChange={(event) => setTagLine(event.target.value)} placeholder={game.placeholders.tag || 'Tag'} autoComplete="off" />
                </label>
              ) : null}

              {game.fields.includes('valorant_region') && game.regionOptions ? (
                <label className="records-compact-field">
                  <span>지역</span>
                  <select value={region} onChange={(event) => setRegion(event.target.value)}>
                    {game.regionOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
              ) : null}

              {(game.fields.includes('pubg_platform') || game.fields.includes('apex_platform')) && game.platformOptions ? (
                <label className="records-compact-field">
                  <span>플랫폼</span>
                  <select value={platform} onChange={(event) => setPlatform(event.target.value)}>
                    {game.platformOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
              ) : null}

              {game.fields.includes('count') ? (
                <label className="records-compact-field">
                  <span>매치 수</span>
                  <select value={count} onChange={(event) => setCount(Number(event.target.value))}>
                    {[5, 10, 15, 20].map((option) => <option key={option} value={option}>{option}게임</option>)}
                  </select>
                </label>
              ) : null}
            </div>

            <div className="records-search-footer">
              <p className="records-inline-hint">{game.hint}</p>
              <div className="records-actions">
                {nickname || tagLine ? <button type="button" className="records-clear-button" onClick={() => { setNickname(''); setTagLine(''); setError(null); setResult(null); }}>초기화</button> : null}
                <button type="submit" className="records-search-button" disabled={loading}>{loading ? '검색 중...' : '검색'}</button>
              </div>
            </div>
          </form>
        </section>

        {error ? <section className="records-error-box"><strong>검색 실패</strong>{error.split('\n').map((line, index) => <p key={`${line}-${index}`}>{line}</p>)}</section> : null}
        {result?.success ? <div ref={resultRef}><ResultPanel result={result} accent={game.accent} title={`${game.label} Result`} /></div> : null}
      </div>
    </Layout>
  );
}
