import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useParams, useSearchParams } from 'react-router-dom';
import Layout from '../../components/Layout';
import type { PlayerSearchResponse } from '../../api/search';
import { GAMES, parseProfileSlug, fetchRecordsPlayerSearch, type SearchFieldOverrides } from './recordsShared';
import { ResultPanel } from './RecordsResultPanel';
import '../Records.css';

function RecordsResultContent({
  gameId,
  playerSlug,
  urlHash,
}: {
  gameId: string;
  playerSlug: string;
  urlHash: string;
}) {
  const autoSearchedUrlKey = useRef<string | null>(null);
  const [searchParams] = useSearchParams();
  const [nickname, setNickname] = useState('');
  const [tagLine, setTagLine] = useState('');
  const [platform, setPlatform] = useState('');
  const [count, setCount] = useState(5);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PlayerSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);

  useEffect(() => {
    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    setNickname(parsed.nickname);
    setTagLine(parsed.tagLine);

    const platformQuery = searchParams.get('platform');
    const meta = GAMES.find((item) => item.id === gameId);
    if (platformQuery && meta?.platformOptions?.some((option) => option.value === platformQuery)) {
      setPlatform(platformQuery);
    }

    const countQuery = searchParams.get('count');
    if (countQuery && [5, 10, 15, 20].includes(Number(countQuery))) {
      setCount(Number(countQuery));
    }
  }, [gameId, playerSlug, searchParams, urlHash]);

  useEffect(() => {
    if (!game.platformOptions?.length) {
      setPlatform('');
      return;
    }
    setPlatform((prev) => {
      const platformQuery = searchParams.get('platform');
      if (platformQuery && game.platformOptions!.some((option) => option.value === platformQuery)) {
        return platformQuery;
      }
      const valid = game.platformOptions!.some((option) => option.value === prev);
      return valid ? prev : game.platformOptions![0].value;
    });
  }, [game, searchParams]);

  const performSearch = useCallback(
    async (forceRefresh: boolean, overrides?: SearchFieldOverrides) => {
      const resolvedGameId = overrides?.gameId ?? gameId;
      const resolvedNickname = (overrides?.nickname ?? nickname).trim();
      if (!resolvedNickname) return;

      const resolvedTagLine = overrides?.tagLine ?? tagLine;
      const resolvedPlatform = overrides?.platform ?? platform;
      const resolvedCount = overrides?.count ?? count;
      const resolvedGame = GAMES.find((item) => item.id === resolvedGameId) || GAMES[0];

      setLoading(true);
      setError(null);
      if (!forceRefresh) setResult(null);

      try {
        const response = await fetchRecordsPlayerSearch(
          resolvedGameId,
          resolvedNickname,
          resolvedTagLine,
          resolvedGame.fields.includes('pubg_platform') ? resolvedPlatform : '',
          resolvedCount,
          forceRefresh,
        );
        setResult(response);
        if (!response.success) {
          setError(response.errorMessage || '전적 정보를 찾지 못했습니다.');
        }
      } catch (error) {
        setError(error instanceof Error ? error.message : '검색 중 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    },
    [count, gameId, nickname, platform, tagLine],
  );

  useEffect(() => {
    const key = `${gameId}/${playerSlug}${urlHash}?${searchParams.toString()}`;
    if (autoSearchedUrlKey.current === key) return;

    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    if (!parsed.nickname.trim()) return;

    autoSearchedUrlKey.current = key;
    const platformQuery = searchParams.get('platform') ?? '';
    const countQuery = searchParams.get('count');
    const resolvedCount = countQuery && [5, 10, 15, 20].includes(Number(countQuery)) ? Number(countQuery) : 5;
    const resolvedGame = GAMES.find((item) => item.id === gameId) || GAMES[0];

    void performSearch(false, {
      gameId,
      nickname: parsed.nickname,
      tagLine: parsed.tagLine,
      platform: resolvedGame.fields.includes('pubg_platform') && platformQuery ? platformQuery : undefined,
      count: resolvedGame.fields.includes('count') ? resolvedCount : undefined,
    });
  }, [gameId, performSearch, playerSlug, searchParams, urlHash]);

  return (
    <Layout>
      <div className="records-search-page">
        <p className="records-back-to-search">
          <Link to={`/records?game=${encodeURIComponent(gameId)}`}>전적 검색으로</Link>
        </p>

        {error ? (
          <section className="records-error-box">
            <strong>검색 오류</strong>
            {error.split('\n').map((line, index) => (
              <p key={`${line}-${index}`}>{line}</p>
            ))}
          </section>
        ) : null}

        {loading && !result?.success ? (
          <section className="records-error-box">
            <p>전적을 불러오는 중입니다.</p>
          </section>
        ) : null}

        {result?.success ? (
          <ResultPanel
            result={result}
            gameId={gameId}
            accent={game.accent}
            title={`${game.label} Result`}
            loading={loading}
            onRefresh={() => void performSearch(true)}
            detailContext={{
              puuid: result.playerInfo?.puuid,
              platform: game.fields.includes('pubg_platform') ? platform : undefined,
            }}
          />
        ) : null}
      </div>
    </Layout>
  );
}

export default function RecordsResult() {
  const { gameId: routeGame, playerSlug } = useParams<{ gameId?: string; playerSlug?: string }>();
  const { hash: urlHash } = useLocation();

  if (!routeGame || !GAMES.some((game) => game.id === routeGame)) {
    return <Navigate to="/records" replace />;
  }

  if (!playerSlug?.trim()) {
    return <Navigate to={`/records?game=${encodeURIComponent(routeGame)}`} replace />;
  }

  return <RecordsResultContent gameId={routeGame} playerSlug={playerSlug} urlHash={urlHash} />;
}
