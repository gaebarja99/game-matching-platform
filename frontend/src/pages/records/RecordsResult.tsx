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
    const platQ = searchParams.get('platform');
    const meta = GAMES.find((g) => g.id === gameId);
    if (platQ && meta?.platformOptions?.some((o) => o.value === platQ)) {
      setPlatform(platQ);
    }
    const c = searchParams.get('count');
    if (c && [5, 10, 15, 20].includes(Number(c))) {
      setCount(Number(c));
    }
  }, [gameId, playerSlug, urlHash, searchParams]);

  useEffect(() => {
    if (!game.platformOptions?.length) {
      setPlatform('');
      return;
    }
    setPlatform((prev) => {
      const platQ = searchParams.get('platform');
      if (platQ && game.platformOptions!.some((o) => o.value === platQ)) {
        return platQ;
      }
      const valid = game.platformOptions!.some((opt) => opt.value === prev);
      return valid ? prev : game.platformOptions![0].value;
    });
  }, [game, searchParams]);

  const performSearch = useCallback(
    async (forceRefresh: boolean, overrides?: SearchFieldOverrides) => {
      const gid = overrides?.gameId ?? gameId;
      const nick = (overrides?.nickname ?? nickname).trim();
      if (!nick) return;
      const tag = overrides?.tagLine ?? tagLine;
      const plat = overrides?.platform ?? platform;
      const cnt = overrides?.count ?? count;
      const g = GAMES.find((item) => item.id === gid) || GAMES[0];
      setLoading(true);
      setError(null);
      if (!forceRefresh) setResult(null);
      try {
        const response = await fetchRecordsPlayerSearch(
          gid,
          nick,
          tag,
          g.fields.includes('pubg_platform') || g.fields.includes('apex_platform') ? plat : '',
          cnt,
          forceRefresh,
        );
        setResult(response);
        if (!response.success) {
          setError(response.errorMessage || '검색에 실패했습니다.');
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '서버 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    },
    [gameId, nickname, tagLine, platform, count],
  );

  useEffect(() => {
    const key = `${gameId}/${playerSlug}${urlHash}?${searchParams.toString()}`;
    if (autoSearchedUrlKey.current === key) return;
    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    if (!parsed.nickname.trim()) return;
    autoSearchedUrlKey.current = key;
    const platQ = searchParams.get('platform') ?? '';
    const cntQ = searchParams.get('count');
    const cnt = cntQ && [5, 10, 15, 20].includes(Number(cntQ)) ? Number(cntQ) : 5;
    const g = GAMES.find((item) => item.id === gameId)!;
    void performSearch(false, {
      gameId,
      nickname: parsed.nickname,
      tagLine: parsed.tagLine,
      platform:
        (g.fields.includes('pubg_platform') || g.fields.includes('apex_platform')) && platQ
          ? platQ
          : undefined,
      count: g.fields.includes('count') ? cnt : undefined,
    });
  }, [gameId, playerSlug, urlHash, searchParams, performSearch]);

  return (
    <Layout>
      <div className="records-search-page">
        <p className="records-back-to-search">
          <Link to={`/records?game=${encodeURIComponent(gameId)}`}>← 전적 검색으로</Link>
        </p>

        {error ? (
          <section className="records-error-box">
            <strong>검색 실패</strong>
            {error.split('\n').map((line, index) => (
              <p key={`${line}-${index}`}>{line}</p>
            ))}
          </section>
        ) : null}

        {loading && !result?.success ? (
          <section className="records-error-box">
            <p>전적을 불러오는 중…</p>
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

  if (!routeGame || !GAMES.some((g) => g.id === routeGame)) {
    return <Navigate to="/records" replace />;
  }
  if (!playerSlug?.trim()) {
    return <Navigate to={`/records?game=${encodeURIComponent(routeGame)}`} replace />;
  }

  return <RecordsResultContent gameId={routeGame} playerSlug={playerSlug} urlHash={urlHash} />;
}
