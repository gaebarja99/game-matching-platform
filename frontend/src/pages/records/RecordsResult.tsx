import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import Layout from '../../components/Layout';
import { decodeApiTextNewlines } from '../../api/client';
import { fetchValorantSearchMmr, type PlayerSearchResponse } from '../../api/search';
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
  if (qIdx < 0) return 10;
  const c = new URLSearchParams(fullKey.slice(qIdx + 1)).get('count');
  const n = c != null ? Number(c) : NaN;
  return VALID_RECORDS_COUNTS.includes(n as (typeof VALID_RECORDS_COUNTS)[number]) ? n : 10;
}

function isLoadMoreRecordsUrl(prevKey: string | null, newKey: string): boolean {
  if (!prevKey) return false;
  return (
    recordsIdentityWithoutCount(prevKey) === recordsIdentityWithoutCount(newKey) &&
    recordsCountFromUrlKey(newKey) > recordsCountFromUrlKey(prevKey)
  );
}

/** 랜딩에서 navigate state로 넘긴 응답이 현재 URL 프로필과 같은지 (중복 API 방지) */
function isPrefetchForCurrentProfile(
  r: PlayerSearchResponse,
  gameId: string,
  playerSlug: string,
  urlHash: string,
): boolean {
  if (!r.success) return false;
  if (r.game && r.game !== gameId) return false;
  try {
    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    const nick = parsed.nickname.trim().toLowerCase();
    const tag = (parsed.tagLine ?? '').trim().toLowerCase();
    const meta = GAMES.find((g) => g.id === gameId);
    if (meta?.fields.includes('tag')) {
      const gn = (r.playerInfo?.gameName ?? '').trim().toLowerCase();
      const tl = (r.playerInfo?.tagLine ?? '').trim().toLowerCase();
      return gn === nick && tl === tag;
    }
    const fromResult = (r.playerInfo?.gameName ?? r.nickname ?? '').trim().toLowerCase();
    return fromResult === nick;
  } catch {
    return false;
  }
}

type ValorantAccountPrefetchState = {
  puuid: string;
  gameName: string;
  tagLine: string;
  accountRegionRaw?: string;
  cardUrl?: string | null;
};

type RecordsResultLocationState = {
  recordsSearchResult?: PlayerSearchResponse;
  valorantPrefetch?: ValorantAccountPrefetchState;
};

function isValorantPrefetchForUrl(
  vf: ValorantAccountPrefetchState,
  gameId: string,
  playerSlug: string,
  urlHash: string,
): boolean {
  if (gameId !== 'valorant') return false;
  const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
  return (
    parsed.nickname.trim().toLowerCase() === vf.gameName.trim().toLowerCase() &&
    (parsed.tagLine ?? '').trim().toLowerCase() === (vf.tagLine ?? '').trim().toLowerCase()
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
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [nickname, setNickname] = useState('');
  const [tagLine, setTagLine] = useState('');
  const [platform, setPlatform] = useState('');
  const [region, setRegion] = useState('');
  const [count, setCount] = useState(10);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PlayerSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const game = useMemo(() => GAMES.find((item) => item.id === gameId) || GAMES[0], [gameId]);

  useEffect(() => {
    if (result?.success !== true || result.game !== 'valorant' || !result.valorantMmrPending) return;
    const puuid = result.playerInfo?.puuid?.trim();
    const raw = result.playerInfo?.rawData as Record<string, unknown> | undefined;
    const mmrRegion = typeof raw?.valorantRegion === 'string' ? raw.valorantRegion.trim() : '';
    if (!puuid || !mmrRegion) return;

    let cancelled = false;
    void (async () => {
      try {
        const mmr = await fetchValorantSearchMmr({ puuid, region: mmrRegion });
        if (cancelled) return;
        setResult((prev) => {
          if (!prev?.success || prev.game !== 'valorant' || !prev.playerInfo) return prev;
          return {
            ...prev,
            valorantMmrPending: false,
            playerInfo: {
              ...prev.playerInfo,
              tier:
                mmr.success && mmr.tierDisplay != null && mmr.tierDisplay !== ''
                  ? mmr.tierDisplay
                  : prev.playerInfo.tier,
            },
          };
        });
      } catch {
        if (cancelled) return;
        setResult((prev) => (prev?.success ? { ...prev, valorantMmrPending: false } : prev));
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [result?.success, result?.game, result?.valorantMmrPending, result?.playerInfo?.puuid, result?.playerInfo?.rawData]);

  useEffect(() => {
    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    setNickname(parsed.nickname);
    setTagLine(parsed.tagLine);

    const platQ = searchParams.get('platform');
    const regionQ = searchParams.get('region');
    const meta = GAMES.find((g) => g.id === gameId);

    if (platQ && meta?.platformOptions?.some((o) => o.value === platQ)) {
      setPlatform(platQ);
    } else {
      setPlatform('');
    }

    if (regionQ) {
      setRegion(regionQ);
    } else {
      setRegion('');
    }

    const c = searchParams.get('count');
    if (c && VALID_RECORDS_COUNTS.includes(Number(c) as (typeof VALID_RECORDS_COUNTS)[number])) {
      setCount(Number(c));
    } else {
      setCount(10);
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
      const searchRegion = overrides?.region ?? region;
      const g = GAMES.find((item) => item.id === gid) || GAMES[0];

      setLoading(true);
      setError(null);
      if (!forceRefresh && !options?.keepPreviousResult) setResult(null);

      try {
        const response = await fetchRecordsPlayerSearch(
          gid,
          nick,
          tag,
          g.fields.includes('pubg_platform') ? plat : '',
          cnt,
          forceRefresh,
          searchRegion,
          overrides?.valorantPrefetch
            ? { valorantPrefetch: overrides.valorantPrefetch }
            : undefined,
        );
        setResult(response);
        if (!response.success) {
          setError(response.errorMessage || '검색에 실패했습니다.');
        }
      } catch (err) {
        const raw = err instanceof Error ? err.message : '서버 오류가 발생했습니다.';
        setError(decodeApiTextNewlines(raw));
      } finally {
        setLoading(false);
      }
    },
    [count, gameId, nickname, platform, region, tagLine],
  );

  const handleLoadMore = useCallback(() => {
    const g = GAMES.find((item) => item.id === gameId);
    if (!g?.fields.includes('count')) return;

    const next = Math.min(count + 5, 20);
    if (next <= count) return;

    const params = new URLSearchParams(location.search);
    params.set('count', String(next));
    const qs = params.toString();

    navigate(
      {
        pathname: location.pathname,
        search: qs ? `?${qs}` : '',
        hash: location.hash,
      },
      { replace: true },
    );
  }, [count, gameId, navigate, location.pathname, location.search, location.hash]);

  useEffect(() => {
    const key = `${gameId}/${playerSlug}${urlHash}?${searchParams.toString()}`;
    if (autoSearchedUrlKey.current === key) return;

    const parsed = parseProfileSlug(gameId, playerSlug, urlHash);
    if (!parsed.nickname.trim()) return;

    const locState = location.state as RecordsResultLocationState | null;
    const vf = locState?.valorantPrefetch;
    if (vf && isValorantPrefetchForUrl(vf, gameId, playerSlug, urlHash)) {
      autoSearchedUrlKey.current = key;
      navigate(
        { pathname: location.pathname, search: location.search, hash: location.hash },
        { replace: true, state: {} },
      );
      void performSearch(false, {
        nickname: vf.gameName,
        tagLine: vf.tagLine,
        valorantPrefetch: {
          puuid: vf.puuid,
          accountRegionRaw: vf.accountRegionRaw,
          cardUrl: vf.cardUrl ?? undefined,
        },
      });
      return;
    }

    const prefetch = locState?.recordsSearchResult;
    if (prefetch && isPrefetchForCurrentProfile(prefetch, gameId, playerSlug, urlHash)) {
      autoSearchedUrlKey.current = key;
      setResult(prefetch);
      setLoading(false);
      setError(null);
      navigate(
        { pathname: location.pathname, search: location.search, hash: location.hash },
        { replace: true, state: {} },
      );
      return;
    }

    const prevKey = autoSearchedUrlKey.current;
    const keepPrev = isLoadMoreRecordsUrl(prevKey, key);
    autoSearchedUrlKey.current = key;

    const platQ = searchParams.get('platform') ?? '';
    const regionQ = searchParams.get('region') ?? '';
    const cntQ = searchParams.get('count');
    const cnt =
      cntQ && VALID_RECORDS_COUNTS.includes(Number(cntQ) as (typeof VALID_RECORDS_COUNTS)[number])
        ? Number(cntQ)
        : 10;
    const g = GAMES.find((item) => item.id === gameId) || GAMES[0];

    void performSearch(
      false,
      {
        gameId,
        nickname: parsed.nickname,
        tagLine: parsed.tagLine,
        platform: g.fields.includes('pubg_platform') && platQ ? platQ : undefined,
        region: regionQ || undefined,
        count: g.fields.includes('count') ? cnt : undefined,
      },
      { keepPreviousResult: keepPrev },
    );
  }, [gameId, playerSlug, urlHash, searchParams, performSearch, location.pathname, location.search, location.hash, location.state, navigate]);

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
            <p>전적을 불러오는 중입니다.</p>
          </section>
        ) : null}

        {result?.success ? (
          <ResultPanel
            result={result}
            gameId={gameId}
            accent={game.accent}
            title={`${game.label} Result`}
            valorantMmrPending={Boolean(result.valorantMmrPending)}
            loading={loading}
            onRefresh={() => void performSearch(true)}
            showLoadMore={showLoadMore}
            onLoadMore={handleLoadMore}
            detailContext={{
              puuid: result.playerInfo?.puuid,
              platform: game.fields.includes('pubg_platform') ? platform : undefined,
              playerName:
                gameId === 'pubg'
                  ? result.nickname || nickname
                  : result.playerInfo?.gameName
                    ? `${result.playerInfo.gameName}${result.playerInfo.tagLine ? `#${result.playerInfo.tagLine}` : ''}`
                    : result.nickname || nickname,
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
