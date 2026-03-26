import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useParams, useSearchParams } from 'react-router-dom';
import Layout from '../../components/Layout';
import type { PlayerSearchResponse } from '../../api/search';
import { GAMES, parseProfileSlug, fetchRecordsPlayerSearch, type SearchFieldOverrides } from './recordsShared';
import { ResultPanel } from './RecordsResultPanel';
import '../Records.css';

const VALID_RECORDS_COUNTS = [5, 10, 15, 20] as const;

function recordsIdentityWithoutCount(fullKey: string): string {
  const qIdx = fullKey.indexOf('?');
  if (qIdx < 0) return fullKey;
  const base = fullKey.slice(0, qIdx);
  const params = new URLSearchParams(fullKey.slice(qIdx + 1));
  params.delete('count');
  const rest = params.toString();
  return rest ? `${base}?${rest}` : base;
}

function recordsCountFromUrlKey(fullKey: string): number {
  const qIdx = fullKey.indexOf('?');
  if (qIdx < 0) return 5;
  const c = new URLSearchParams(fullKey.slice(qIdx + 1)).get('count');
  const n = c != null ? Number(c) : NaN;
  return VALID_RECORDS_COUNTS.includes(n as (typeof VALID_RECORDS_COUNTS)[number]) ? n : 5;
}

function isLoadMoreRecordsUrl(prevKey: string | null, newKey: string): boolean {
  if (!prevKey) return false;
  return (
    recordsIdentityWithoutCount(prevKey) === recordsIdentityWithoutCount(newKey) &&
    recordsCountFromUrlKey(newKey) > recordsCountFromUrlKey(prevKey)
  );
}

type PerformSearchOptions = { keepPreviousResult?: boolean };

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
  const [searchParams, setSearchParams] = useSearchParams();

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
    if (c && VALID_RECORDS_COUNTS.includes(Number(c) as (typeof VALID_RECORDS_COUNTS)[number])) {
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
    async (forceRefresh: boolean, overrides?: SearchFieldOverrides, options?: PerformSearchOptions) => {
      const gid = overrides?.gameId ?? gameId;
      const nick = (overrides?.nickname ?? nickname).trim();
      if (!nick) return;
      const tag = overrides?.tagLine ?? tagLine;
      const plat = overrides?.platform ?? platform;
      const cnt = overrides?.count ?? count;
      const g = GAMES.find((item) => item.id === gid) || GAMES[0];
      setLoading(true);
      setError(null);
      if (!forceRefresh && !options?.keepPreviousResult) setResult(null);
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

  const handleLoadMore = useCallback(() => {
    const g = GAMES.find((item) => item.id === gameId)!;
    if (!g.fields.includes('count')) return;
    const next = Math.min(count + 5, 20);
    if (next <= count) return;
    setSearchParams(
      (prev) => {
        const n = new URLSearchParams(prev);
        n.set('count', String(next));
        return n;
      },
      { replace: true },
    );
  }, [count, gameId, setSearchParams]);

  useEffect(() => {
    const key = `${gameId}/${playerSlug}${urlHash}?${searchParams.toString()}`;
    if (autoSearchedUrlKey.current === key) return;
    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    if (!parsed.nickname.trim()) return;
    const prevKey = autoSearchedUrlKey.current;
    const keepPrev = isLoadMoreRecordsUrl(prevKey, key);
    autoSearchedUrlKey.current = key;
    const platQ = searchParams.get('platform') ?? '';
    const cntQ = searchParams.get('count');
    const cnt =
      cntQ && VALID_RECORDS_COUNTS.includes(Number(cntQ) as (typeof VALID_RECORDS_COUNTS)[number])
        ? Number(cntQ)
        : 5;
    const g = GAMES.find((item) => item.id === gameId)!;
    void performSearch(
      false,
      {
        gameId,
        nickname: parsed.nickname,
        tagLine: parsed.tagLine,
        platform:
          (g.fields.includes('pubg_platform') || g.fields.includes('apex_platform')) && platQ
            ? platQ
            : undefined,
        count: g.fields.includes('count') ? cnt : undefined,
      },
      { keepPreviousResult: keepPrev },
    );
  }, [gameId, playerSlug, urlHash, searchParams, performSearch]);

  const supportsPaginatedMatches = game.fields.includes('count');
  const matchCount = result?.matches?.length ?? 0;
  const showLoadMore =
    Boolean(result?.success) && supportsPaginatedMatches && count < 20 && matchCount > 0 && matchCount === count;

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
            showLoadMore={showLoadMore}
            onLoadMore={handleLoadMore}
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
