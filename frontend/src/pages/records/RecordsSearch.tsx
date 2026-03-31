import { useCallback, useEffect, useMemo, useState, type CSSProperties, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../../components/Layout';
import { decodeApiTextNewlines } from '../../api/client';
import { GAMES, buildRecordsProfileUrl, fetchRecordsPlayerSearch, getRecordsLandingCssVars } from './recordsShared';
import '../Records.css';

const FEATURED_GAMES = ['lol', 'tft', 'valorant', 'pubg', 'overwatch', 'cs2'] as const;

function gameCardClass(gameId: string) {
  return `records-landing-card records-landing-card--${gameId}`;
}

export default function RecordsSearch() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const urlGame = searchParams.get('game');
  const [gameId, setGameId] = useState(() =>
    urlGame && GAMES.some((g) => g.id === urlGame) ? urlGame : 'lol',
  );
  const [nickname, setNickname] = useState('');
  const [tagLine, setTagLine] = useState('KR1');
  const [platform, setPlatform] = useState('');
  const [count] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);
  const featuredGames = useMemo(
    () => FEATURED_GAMES.map((id) => GAMES.find((item) => item.id === id)).filter(Boolean) as typeof GAMES,
    [],
  );
  const isTwoFieldSearch = gameId === 'cs2';

  const recordsLandingFormClass = useMemo(
    () => ['records-landing-form', isTwoFieldSearch ? 'records-landing-form--two-field' : ''].filter(Boolean).join(' '),
    [isTwoFieldSearch],
  );

  useEffect(() => {
    const g = searchParams.get('game');
    if (g && GAMES.some((item) => item.id === g)) {
      setGameId(g);
    }
  }, [searchParams]);

  useEffect(() => {
    if (!game.platformOptions?.length) {
      setPlatform('');
      return;
    }
    setPlatform((prev) => {
      const valid = game.platformOptions!.some((opt) => opt.value === prev);
      return valid ? prev : game.platformOptions![0].value;
    });
  }, [game]);

  useEffect(() => {
    if (!game.fields.includes('tag')) {
      setTagLine('');
      return;
    }

    setTagLine((prev) => {
      if (prev.trim()) return prev;
      if (gameId === 'valorant') return '';
      return 'KR1';
    });
  }, [game.fields, gameId]);

  const selectGame = (id: string) => {
    setGameId(id);
    setError(null);
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('game', id);
      return next;
    });
  };

  const runSearch = useCallback(
    async (overrides?: { nickname?: string; tagLine?: string }) => {
      const nick = (overrides?.nickname ?? nickname).trim();
      const tag = (overrides?.tagLine ?? tagLine).trim();
      if (!nick) {
        setError('검색어를 입력해 주세요.');
        return;
      }

      setLoading(true);
      setError(null);

      try {
        if (gameId === 'valorant') {
          const probe = await fetchRecordsPlayerSearch(gameId, nick, tag, platform, count, false, undefined, {
            accountOnly: true,
          });
          if (!probe.success) {
            setError(probe.errorMessage || '검색에 실패했습니다.');
            return;
          }
          const puuid = probe.playerInfo?.puuid?.trim();
          if (!puuid) {
            setError('계정 정보를 확인했지만 puuid가 없습니다.');
            return;
          }
          const raw = probe.playerInfo?.rawData as Record<string, unknown> | undefined;
          const shard = typeof raw?.valorantRegion === 'string' ? raw.valorantRegion.trim() : undefined;
          navigate(buildRecordsProfileUrl(gameId, nick, tag, platform, count, undefined), {
            state: {
              valorantPrefetch: {
                puuid,
                gameName: nick,
                tagLine: tag,
                accountRegionRaw: shard,
                cardUrl: probe.playerInfo?.avatarUrl ?? null,
              },
            },
          });
          return;
        }

        const response = await fetchRecordsPlayerSearch(
          gameId,
          nick,
          tag,
          platform,
          count,
          false,
          undefined,
        );

        if (!response.success) {
          setError(response.errorMessage || '검색에 실패했습니다.');
          return;
        }

        navigate(buildRecordsProfileUrl(gameId, nick, tag, platform, count, undefined), {
          state: { recordsSearchResult: response },
        });
      } catch (err) {
        const raw = err instanceof Error ? err.message : '서버 오류가 발생했습니다.';
        setError(decodeApiTextNewlines(raw));
      } finally {
        setLoading(false);
      }
    },
    [count, gameId, navigate, nickname, platform, tagLine],
  );

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const fd = new FormData(event.currentTarget);
    const domNick = String(fd.get('nickname') ?? '').trim();
    const domTag = game.fields.includes('tag') ? String(fd.get('tagLine') ?? '').trim() : tagLine.trim();
    setNickname(domNick);
    if (game.fields.includes('tag')) {
      setTagLine(domTag);
    }
    void runSearch(
      game.fields.includes('tag') ? { nickname: domNick, tagLine: domTag } : { nickname: domNick },
    );
  };

  const landingCssVars = useMemo(() => getRecordsLandingCssVars(game.accent), [game.accent]);

  return (
    <Layout>
      <div className="records-search-page records-search-page--landing" style={landingCssVars as CSSProperties}>
        <section className="records-landing-hero">
          <span className="records-landing-badge">전적 검색</span>
          <span className="records-landing-game-chip">{game.label}</span>

          <div className="records-landing-copy">
            <p className="records-kicker">GAME MATCHER SEARCH</p>
            <h1 className="records-landing-title">
              전적을 빠르게 검색
              <br />
              해보세요
            </h1>
            <p className="records-landing-description">
              게임명과 태그를 입력하면 최근 {game.fields.includes('count') ? `${count}` : '10'}경기 전적을 확인할 수 있습니다.
            </p>
          </div>

          <div className="records-landing-tips">
            <span>게임명과 태그를 정확히 입력해 주세요.</span>
            <span>게임별 검색 조건이 조금씩 다를 수 있습니다.</span>
            <span>최근 10경기 기준으로 결과를 보여줍니다.</span>
          </div>

          <form className={recordsLandingFormClass} onSubmit={handleSearch}>
            <label className="records-landing-field records-landing-field--game">
              <span>게임</span>
              <select name="gameId" value={gameId} onChange={(event) => selectGame(event.target.value)}>
                {featuredGames.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="records-landing-field records-landing-field--nickname">
              <span>검색</span>
              <input
                type="text"
                name="nickname"
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                placeholder={game.placeholders.nickname}
                autoComplete="off"
                required
              />
            </label>

            {game.fields.includes('tag') ? (
              <label className="records-landing-field records-landing-field--tag">
                <span>{game.tagLabel || '태그'}</span>
                <input
                  type="text"
                  name="tagLine"
                  value={tagLine}
                  onChange={(event) => setTagLine(event.target.value)}
                  placeholder={game.placeholders.tag || 'KR1'}
                  autoComplete="off"
                />
              </label>
            ) : null}

            {game.fields.includes('pubg_platform') ? (
              <label className="records-landing-field records-landing-field--tag">
                <span>플랫폼</span>
                <select name="platform" value={platform} onChange={(event) => setPlatform(event.target.value)}>
                  {game.platformOptions?.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}

            <button type="submit" className="records-landing-submit" disabled={loading}>
              {loading ? (gameId === 'valorant' ? '계정 확인 중' : '검색 중') : '검색'}
            </button>
          </form>

          <p className="records-landing-footer">{game.hint}</p>
        </section>

        <section className="records-landing-cards">
          {featuredGames.map((item) => (
            <button
              key={item.id}
              type="button"
              className={[gameCardClass(item.id), item.id === gameId ? 'is-active' : ''].join(' ')}
              aria-label={item.label}
              style={{ ['--records-card-image' as string]: `url(${item.cardImage})` }}
              onClick={() => selectGame(item.id)}
            />
          ))}
        </section>

        {error ? (
          <section className="records-error-box">
            <strong>검색 실패</strong>
            {error.split('\n').map((line, index) => (
              <p key={`${line}-${index}`}>{line}</p>
            ))}
          </section>
        ) : null}
      </div>
    </Layout>
  );
}
