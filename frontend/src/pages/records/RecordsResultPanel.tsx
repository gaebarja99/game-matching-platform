import { useEffect, useMemo, useRef, useState } from 'react';
import {
  fetchLolSavedAiEvaluation,
  fetchMatchDetail,
  fetchPubgSavedAiEvaluation,
  fetchValorantSavedAiEvaluation,
  runLolMatchAiEvaluation,
  runPubgMatchAiEvaluation,
  runValorantMatchAiEvaluation,
  type PlayerSearchResponse,
} from '../../api/search';
import {
  formatWinRate,
  getInitials,
  GAMES_WITH_MATCH_DETAIL,
  RECORDS_AI_MODEL_OPTIONS,
} from './recordsShared';
import {
  extractValorantRowModeFromDetailPayload,
  formatRecordsMatchDetail,
  type FormattedMatchDetail,
  type MatchDetailBlock,
} from './recordsMatchDetailFormat';

type ParsedAiSavedBlocks = {
  model?: string;
  status?: string;
  grade?: string;
  score?: string;
  summary?: string;
  detailed?: string;
};

/** 저장된 AI KV 블록 → 화면용 구조 */
function parseAiSavedBlocks(blocks: MatchDetailBlock[]): ParsedAiSavedBlocks {
  const out: ParsedAiSavedBlocks = {};
  for (const block of blocks) {
    if (block.kind !== 'kv') continue;
    for (const [k, v] of block.items) {
      if (k === '모델') out.model = v;
      else if (k === '상태') out.status = v;
      else if (k === '등급') out.grade = v;
      else if (k === '점수') out.score = v;
      else if (k === '요약') out.summary = v;
      else if (k === '상세') out.detailed = v;
    }
  }
  return out;
}

function gradeModifierClass(grade: string | undefined): string | undefined {
  if (!grade) return undefined;
  const g = grade.trim().toUpperCase().charAt(0);
  if (['S', 'A', 'B', 'C', 'D'].includes(g)) return `is-grade-${g}`;
  return undefined;
}

function MatchDetailBlocksList({ blocks }: { blocks: MatchDetailBlock[] }) {
  if (!blocks.length) {
    return <p className="records-match-detail-status">표시할 내용이 없습니다.</p>;
  }
  return (
    <div className="records-match-detail-formatted">
      {blocks.map((block, i) => {
        if (block.kind === 'kv') {
          return (
            <div key={`kv-${i}`} className="records-match-detail-section">
              {block.title.trim() ? (
                <h4 className="records-match-detail-section-title">{block.title}</h4>
              ) : null}
              <dl className="records-match-detail-kv">
                {block.items.map(([k, v], ki) => (
                  <div key={ki} className="records-match-detail-kv-row">
                    <dt>{k}</dt>
                    <dd>{v}</dd>
                  </div>
                ))}
              </dl>
            </div>
          );
        }
        const valorantTable = Boolean(block.rowTeamTint && block.rowTeamTint.length > 0);
        return (
          <div key={`table-${i}`} className="records-match-detail-section">
            {block.title.trim() ? (
              <h4 className="records-match-detail-section-title">{block.title}</h4>
            ) : null}
            <div
              className={[
                'records-match-detail-table-wrap',
                valorantTable ? 'records-match-detail-table-wrap--valorant-inset' : '',
              ]
                .filter(Boolean)
                .join(' ')}
            >
              <table
                className={[
                  'records-match-detail-table',
                  valorantTable ? 'records-match-detail-table--valorant' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
              >
                <thead>
                  <tr>
                    {block.headers.map((h, hi) => (
                      <th
                        key={h}
                        className={valorantTable && hi === 0 ? 'records-match-detail-col-player' : undefined}
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {block.rows.map((row, ri) => {
                    const tint = block.rowTeamTint?.[ri];
                    const teamClass =
                      tint === 'blue' ? 'is-team-blue' : tint === 'red' ? 'is-team-red' : undefined;
                    const rowClass = [teamClass, block.highlightRow === ri ? 'is-highlight' : '']
                      .filter(Boolean)
                      .join(' ');
                    return (
                      <tr key={ri} className={rowClass || undefined}>
                        {row.map((cell, ci) => (
                          <td
                            key={ci}
                            className={
                              valorantTable && ci === 0 ? 'records-match-detail-col-player' : undefined
                            }
                            title={valorantTable && ci === 0 ? String(cell) : undefined}
                          >
                            {cell}
                          </td>
                        ))}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function RecordsMatchAiTab({
  gameId,
  matchId,
  puuid,
  savedBlocks,
  onMergeDetailPayload,
  aiModel,
  onAiModelChange,
  aiModelSwitchLoading,
}: {
  gameId: string;
  matchId: string;
  puuid?: string;
  savedBlocks: MatchDetailBlock[];
  onMergeDetailPayload: (patch: Record<string, unknown>) => void;
  aiModel: string;
  onAiModelChange: (model: string) => void;
  /** 모델만 바꿀 때 저장분만 조회 중(전체 상세 로딩과 구분) */
  aiModelSwitchLoading?: boolean;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isValorantAi = gameId === 'valorant';
  const isPubgAi = gameId === 'pubg';
  const isLolAi = gameId === 'lol';
  const supported = isValorantAi || isPubgAi || isLolAi;
  const modelBusy = Boolean(aiModelSwitchLoading);
  const parsed = useMemo(() => parseAiSavedBlocks(savedBlocks), [savedBlocks]);
  const hasResultContent = Boolean(
    parsed.model || parsed.status || parsed.grade || parsed.score || parsed.summary || parsed.detailed,
  );

  const run = async () => {
    if (!supported || !puuid) return;
    setError(null);
    setLoading(true);
    try {
      if (isValorantAi) {
        const rows = await runValorantMatchAiEvaluation({
          matchId,
          puuid,
          model: aiModel,
        });
        const pid = puuid.trim().toLowerCase();
        const row =
          rows.find((r) => (r.playerPuuid ?? '').trim().toLowerCase() === pid) ?? rows[0];
        if (!row) {
          setError(
            '분석 결과가 없습니다. 매치가 DB에 없거나 대상 플레이어를 찾지 못했습니다. 상세 전적을 한 번 연 뒤(매치 저장) 다시 시도해 주세요.',
          );
          onMergeDetailPayload({ records_ai_evaluation: null });
          return;
        }
        onMergeDetailPayload({
          records_ai_evaluation: {
            llmModel: row.llmModel ?? aiModel,
            status: row.status ?? undefined,
            grade: row.grade ?? undefined,
            score: row.score ?? undefined,
            summary: row.summary ?? undefined,
            detailedComment: row.detailedComment ?? undefined,
          },
        });
        return;
      }

      if (isLolAi) {
        const rows = await runLolMatchAiEvaluation({
          matchId,
          puuid,
          model: aiModel,
        });
        const pid = puuid.trim().toLowerCase();
        const row =
          rows.find((r) => (r.playerPuuid ?? '').trim().toLowerCase() === pid) ?? rows[0];
        if (!row) {
          setError(
            '분석 결과가 없습니다. 매치가 DB에 없거나 대상 소환사를 찾지 못했습니다. 상세 전적을 한 번 연 뒤(매치 저장) 다시 시도해 주세요.',
          );
          onMergeDetailPayload({ records_ai_evaluation: null });
          return;
        }
        onMergeDetailPayload({
          records_ai_evaluation: {
            llmModel: row.llmModel ?? aiModel,
            status: row.status ?? undefined,
            grade: row.grade ?? undefined,
            score: row.score ?? undefined,
            summary: row.summary ?? undefined,
            detailedComment: row.detailedComment ?? undefined,
          },
        });
        return;
      }

      if (isPubgAi) {
        const rows = await runPubgMatchAiEvaluation({
          matchId,
          accountId: puuid,
          model: aiModel,
        });
        const aid = puuid.trim();
        const row = rows.find((r) => (r.accountId ?? '').trim() === aid) ?? rows[0];
        if (!row) {
          setError(
            '분석 결과가 없습니다. 매치가 DB에 없거나 해당 계정의 참가 기록을 찾지 못했습니다. 상세 전적을 연 뒤(매치 저장) 다시 시도해 주세요.',
          );
          onMergeDetailPayload({ records_ai_evaluation: null });
          return;
        }
        onMergeDetailPayload({
          records_ai_evaluation: {
            llmModel: row.llmModel ?? aiModel,
            status: row.status ?? undefined,
            grade: row.grade ?? undefined,
            score: row.score ?? undefined,
            summary: row.summary ?? undefined,
            detailedComment: row.detailedComment ?? undefined,
          },
        });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '분석 실패');
      onMergeDetailPayload({ records_ai_evaluation: null });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="records-match-detail-ai-run">
      {supported ? (
        <>
          <div className="records-match-detail-ai-controls">
            {isValorantAi || isLolAi || isPubgAi ? (
              <label className="records-match-detail-ai-model-field">
                <span className="records-match-detail-ai-model-caption">모델</span>
                <select
                  className="records-match-detail-ai-model-select"
                  value={aiModel}
                  onChange={(e) => onAiModelChange(e.target.value)}
                  disabled={loading || modelBusy || !puuid}
                >
                  {RECORDS_AI_MODEL_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            <button
              type="button"
              className="records-match-detail-ai-run-btn"
              disabled={loading || modelBusy || !puuid}
              onClick={() => void run()}
            >
              {loading ? '분석 중…' : '분석'}
            </button>
          </div>
          {!puuid ? (
            <p className="records-match-detail-ai-hint">
              {isPubgAi
                ? 'PUBG 계정 ID를 알 수 없어 분석을 실행할 수 없습니다. 전적 검색이 정상인지 확인해 주세요.'
                : '플레이어 식별 정보(puuid)가 없어 분석을 실행할 수 없습니다.'}
            </p>
          ) : null}
          {puuid && modelBusy ? (
            <p className="records-match-detail-ai-hint">선택한 모델의 저장된 분석을 불러오는 중…</p>
          ) : null}
          {error ? <p className="records-match-detail-error">{error}</p> : null}
          <div
            className={[
              'records-ai-result-card',
              loading ? 'is-loading' : '',
              hasResultContent ? 'has-content' : '',
            ]
              .filter(Boolean)
              .join(' ')}
            aria-busy={loading}
          >
            <div className="records-ai-result-card-head">
              <div className="records-ai-result-card-head-text">
                <span className="records-ai-result-eyebrow">AI evaluation</span>
                <h4 className="records-ai-result-title">분석 결과</h4>
              </div>
              {loading ? (
                <span className="records-ai-result-pulse" aria-hidden>
                  <span className="records-ai-result-pulse-dot" />
                  생성 중
                </span>
              ) : hasResultContent ? (
                <span className="records-ai-result-done">완료</span>
              ) : null}
            </div>

            {loading && !hasResultContent ? (
              <div className="records-ai-result-skeleton" aria-hidden>
                <div className="records-ai-skeleton-line records-ai-skeleton-line--long" />
                <div className="records-ai-skeleton-line records-ai-skeleton-line--med" />
                <div className="records-ai-skeleton-line records-ai-skeleton-line--short" />
              </div>
            ) : null}

            {(parsed.model || parsed.status || parsed.grade || parsed.score) && (!loading || hasResultContent) ? (
              <div className="records-ai-result-meta">
                {parsed.model ? (
                  <span className="records-ai-meta-chip is-model">
                    <span className="records-ai-meta-label">모델</span>
                    <span className="records-ai-meta-value">{parsed.model}</span>
                  </span>
                ) : null}
                {parsed.status ? (
                  <span
                    className={[
                      'records-ai-meta-chip',
                      parsed.status.toUpperCase().includes('COMPLET')
                        ? 'is-status-done'
                        : 'is-status-pending',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                  >
                    <span className="records-ai-meta-label">상태</span>
                    <span className="records-ai-meta-value">{parsed.status}</span>
                  </span>
                ) : null}
                {parsed.grade ? (
                  <span
                    className={[
                      'records-ai-meta-chip',
                      'records-ai-meta-chip--grade',
                      gradeModifierClass(parsed.grade) ?? '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                  >
                    <span className="records-ai-meta-label">등급</span>
                    <span className="records-ai-meta-value">{parsed.grade}</span>
                  </span>
                ) : null}
                {parsed.score ? (
                  <span className="records-ai-meta-chip">
                    <span className="records-ai-meta-label">점수</span>
                    <span className="records-ai-meta-value">{parsed.score}</span>
                  </span>
                ) : null}
              </div>
            ) : null}

            {parsed.summary ? (
              <section className="records-ai-result-section">
                <h5 className="records-ai-section-label">요약</h5>
                <div className="records-ai-section-panel records-ai-section-panel--summary">
                  <p className="records-ai-section-text">{parsed.summary}</p>
                </div>
              </section>
            ) : null}

            {parsed.detailed ? (
              <section className="records-ai-result-section">
                <h5 className="records-ai-section-label">상세 코멘트</h5>
                <div className="records-ai-section-panel records-ai-section-panel--detail">
                  <p className="records-ai-section-text">{parsed.detailed}</p>
                </div>
              </section>
            ) : null}

            {!loading && !hasResultContent ? (
              <div className="records-ai-result-empty">
                <p className="records-ai-result-empty-title">아직 결과가 없습니다</p>
                <p className="records-ai-result-empty-hint">
                  모델을 고른 뒤 <strong>분석</strong>을 누르면 이 영역에 요약과 상세가 표시됩니다.
                </p>
              </div>
            ) : null}

            {!loading && hasResultContent && !parsed.summary && !parsed.detailed ? (
              <p className="records-ai-result-prose-missing">
                요약·상세 텍스트가 비어 있습니다. API 키와 모델 응답(JSON)을 확인해 주세요.
              </p>
            ) : null}
          </div>
        </>
      ) : (
        <p className="records-match-detail-ai-placeholder">
          이 게임은 전적 화면에서 AI 분석 실행을 아직 지원하지 않습니다.
        </p>
      )}
    </div>
  );
}

function MatchDetailFormattedView({
  detail,
  gameId,
  matchId,
  puuid,
  onMergeDetailPayload,
  aiModel,
  onAiModelChange,
  aiModelSwitchLoading,
}: {
  detail: FormattedMatchDetail;
  gameId: string;
  matchId: string;
  puuid?: string;
  onMergeDetailPayload: (patch: Record<string, unknown>) => void;
  aiModel: string;
  onAiModelChange: (model: string) => void;
  aiModelSwitchLoading?: boolean;
}) {
  const [tab, setTab] = useState<'match' | 'players' | 'ai'>('match');

  useEffect(() => {
    setTab((current) => {
      if (current === 'ai') return 'ai';
      if (detail.matchBlocks.length) return 'match';
      if (detail.playerBlocks.length) return 'players';
      return 'match';
    });
  }, [detail]);

  const hasMatch = detail.matchBlocks.length > 0;
  const hasPlayers = detail.playerBlocks.length > 0;
  if (!hasMatch && !hasPlayers) {
    return <p className="records-match-detail-status">표시할 요약이 없습니다.</p>;
  }

  return (
    <div className="records-match-detail-tabbed">
      <div className="records-match-detail-tabs" role="tablist" aria-label="상세 구분">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'match'}
          className={`records-match-detail-tab${tab === 'match' ? ' is-active' : ''}`}
          onClick={() => setTab('match')}
        >
          매치 정보
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'players'}
          className={`records-match-detail-tab${tab === 'players' ? ' is-active' : ''}`}
          onClick={() => setTab('players')}
        >
          플레이어 통계
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'ai'}
          className={`records-match-detail-tab${tab === 'ai' ? ' is-active' : ''}`}
          onClick={() => setTab('ai')}
        >
          AI 분석
        </button>
      </div>
      <div className="records-match-detail-tab-panel" role="tabpanel">
        {tab === 'match' ? (
          <MatchDetailBlocksList blocks={detail.matchBlocks} />
        ) : tab === 'players' ? (
          <MatchDetailBlocksList blocks={detail.playerBlocks} />
        ) : (
          <RecordsMatchAiTab
            gameId={gameId}
            matchId={matchId}
            puuid={puuid}
            savedBlocks={detail.aiBlocks ?? []}
            onMergeDetailPayload={onMergeDetailPayload}
            aiModel={aiModel}
            onAiModelChange={onAiModelChange}
            aiModelSwitchLoading={aiModelSwitchLoading}
          />
        )}
      </div>
    </div>
  );
}

export function MatchRow({
  gameId,
  match,
  detailContext,
}: {
  gameId: string;
  match: NonNullable<PlayerSearchResponse['matches']>[number];
  detailContext: { puuid?: string; platform?: string };
}) {
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailPayload, setDetailPayload] = useState<Record<string, unknown> | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detailAiModel, setDetailAiModel] = useState(RECORDS_AI_MODEL_OPTIONS[0]?.value ?? 'gpt-5-mini');
  const [detailAiSlotLoading, setDetailAiSlotLoading] = useState(false);
  const lastDetailFetchKey = useRef<string | null>(null);

  const buildDetailFetchKey = (model: string) =>
    `${match.matchId ?? ''}|${gameId}|${gameId === 'valorant' || gameId === 'lol' || gameId === 'pubg' ? model : '-'}|${detailContext.puuid ?? ''}`;

  const duration = match.playtime ? `${Math.floor(match.playtime / 60)}m` : null;
  const canDetail = GAMES_WITH_MATCH_DETAIL.has(gameId) && Boolean(match.matchId);
  const listOnlyRow = Boolean(match.extras && (match.extras as { listOnly?: boolean }).listOnly);

  const formattedDetail = useMemo(
    () =>
      detailPayload
        ? formatRecordsMatchDetail(gameId, detailPayload, { puuid: detailContext.puuid })
        : null,
    [gameId, detailPayload, detailContext.puuid],
  );

  const summaryGameMode = useMemo(() => {
    const fromList = match.gameMode?.trim();
    if (fromList) return fromList;
    if (gameId === 'valorant' && detailPayload) {
      const fromDetail = extractValorantRowModeFromDetailPayload(detailPayload);
      if (fromDetail) return fromDetail;
    }
    return '';
  }, [match.gameMode, gameId, detailPayload]);

  const toggleDetail = async () => {
    if (!canDetail) return;
    if (detailOpen) {
      setDetailOpen(false);
      return;
    }
    setDetailOpen(true);
    const fetchKey = buildDetailFetchKey(detailAiModel);
    if (detailPayload != null && lastDetailFetchKey.current === fetchKey) return;
    setDetailLoading(true);
    setDetailError(null);
    try {
      const res = await fetchMatchDetail({
        game: gameId,
        matchId: match.matchId!,
        puuid: detailContext.puuid,
        platform: detailContext.platform,
        llmModel:
          gameId === 'valorant' || gameId === 'lol' || gameId === 'pubg' ? detailAiModel : undefined,
      });
      if (!res.success) {
        setDetailError(res.errorMessage || '상세를 불러오지 못했습니다.');
        return;
      }
      lastDetailFetchKey.current = fetchKey;
      setDetailPayload((res.payload ?? {}) as Record<string, unknown>);
    } catch (e) {
      setDetailError(e instanceof Error ? e.message : '상세 요청 오류');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleAiModelChange = async (model: string) => {
    setDetailAiModel(model);
    if (
      !detailOpen ||
      !match.matchId ||
      (gameId !== 'valorant' && gameId !== 'lol' && gameId !== 'pubg')
    ) {
      return;
    }

    const fetchKey = buildDetailFetchKey(model);
    lastDetailFetchKey.current = fetchKey;

    const puuid = detailContext.puuid;
    if (!puuid) {
      setDetailPayload((prev) => (prev ? { ...prev, records_ai_evaluation: null } : prev));
      return;
    }

    setDetailAiSlotLoading(true);
    setDetailError(null);
    try {
      let row:
        | Awaited<ReturnType<typeof fetchValorantSavedAiEvaluation>>
        | Awaited<ReturnType<typeof fetchPubgSavedAiEvaluation>> = null;
      if (gameId === 'valorant') {
        row = await fetchValorantSavedAiEvaluation({
          matchId: match.matchId,
          puuid,
          model,
        });
      } else if (gameId === 'lol') {
        row = await fetchLolSavedAiEvaluation({
          matchId: match.matchId,
          puuid,
          model,
        });
      } else if (gameId === 'pubg') {
        row = await fetchPubgSavedAiEvaluation({
          matchId: match.matchId,
          accountId: puuid,
          model,
        });
      }
      setDetailPayload((prev) => {
        if (!prev) return prev;
        if (!row) {
          return { ...prev, records_ai_evaluation: null };
        }
        return {
          ...prev,
          records_ai_evaluation: {
            llmModel: row.llmModel ?? model,
            status: row.status ?? undefined,
            grade: row.grade ?? undefined,
            score: row.score ?? undefined,
            summary: row.summary ?? undefined,
            detailedComment: row.detailedComment ?? undefined,
          },
        };
      });
    } catch (e) {
      setDetailError(e instanceof Error ? e.message : '저장된 분석 불러오기 실패');
    } finally {
      setDetailAiSlotLoading(false);
    }
  };

  return (
    <div className="records-match-block">
      <div
        className={[
          'records-match-row',
          listOnlyRow ? 'is-list-only' : match.win ? 'is-win' : 'is-loss',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <div className="records-match-status">
          {listOnlyRow ? (
            <span className="records-list-only-badge">요약 없음</span>
          ) : (
            <span className={match.win ? 'records-win-badge' : 'records-loss-badge'}>
              {match.win ? 'WIN' : 'LOSS'}
            </span>
          )}
          <span className="records-match-mode">{summaryGameMode || 'Unknown mode'}</span>
        </div>
        <div
          className={
            gameId === 'valorant'
              ? 'records-match-center records-match-center--valorant'
              : 'records-match-center'
          }
        >
          <div className="records-match-stat-block records-match-stat-block--agent">
            <span className="records-match-stat-label">{gameId === 'valorant' ? '에이전트' : '픽'}</span>
            <strong className="records-match-title">
              {listOnlyRow
                ? '상세 전적에서 확인'
                : match.champion || match.agent || 'Unknown'}
            </strong>
          </div>
          {match.kills != null ? (
            <div className="records-match-stat-block">
              <span className="records-match-stat-label">K / D / A</span>
              <span className="records-match-kda">
                {match.kills} / {match.deaths ?? 0} / {match.assists ?? 0}
              </span>
            </div>
          ) : null}
          {gameId === 'valorant' && match.extras && typeof match.extras.valorantRoundScore === 'string' ? (
            <div className="records-match-stat-block">
              <span className="records-match-stat-label">라운드</span>
              <div className="records-valorant-roundline">
                <span className="records-valorant-round-score">{match.extras.valorantRoundScore}</span>
              </div>
            </div>
          ) : null}
          {gameId === 'valorant' && match.extras?.avgCombatScorePerRound != null ? (
            <div className="records-match-stat-block">
              <span className="records-match-stat-label records-match-stat-label--asis">acs</span>
              <div className="records-valorant-roundline">
                <span className="records-valorant-avg-score">
                  {typeof match.extras.avgCombatScorePerRound === 'number'
                    ? match.extras.avgCombatScorePerRound.toFixed(1)
                    : String(match.extras.avgCombatScorePerRound)}
                </span>
              </div>
            </div>
          ) : null}
        </div>
        <div className="records-match-meta-chips">
          {listOnlyRow && match.matchId ? (
            <span className="records-meta-chip records-meta-chip--id" title={match.matchId}>
              {match.matchId.length > 14 ? `${match.matchId.slice(0, 12)}…` : match.matchId}
            </span>
          ) : null}
          {duration ? <span className="records-meta-chip">{duration}</span> : null}
          {match.cs != null ? <span className="records-meta-chip">CS {match.cs}</span> : null}
          {match.extras && gameId !== 'valorant' && !listOnlyRow
            ? Object.entries(match.extras)
                .filter(([key]) => key !== 'listOnly')
                .slice(0, 2)
                .map(([key, value]) => (
                  <span key={key} className="records-meta-chip">
                    {key}: {String(value)}
                  </span>
                ))
            : null}
        </div>
        <div className="records-match-actions">
          {canDetail ? (
            <button type="button" className="records-match-detail-toggle" onClick={() => void toggleDetail()}>
              {detailOpen ? '상세 닫기' : '상세 전적'}
            </button>
          ) : null}
        </div>
      </div>
      {detailOpen ? (
        <div className="records-match-detail-panel">
          {detailLoading ? <p className="records-match-detail-status">상세 불러오는 중…</p> : null}
          {detailError ? <p className="records-match-detail-error">{detailError}</p> : null}
          {!detailLoading && !detailError && detailPayload && formattedDetail ? (
            <MatchDetailFormattedView
              detail={formattedDetail}
              gameId={gameId}
              matchId={match.matchId!}
              puuid={detailContext.puuid}
              onMergeDetailPayload={(patch) =>
                setDetailPayload((prev) => (prev ? { ...prev, ...patch } : prev))
              }
              aiModel={detailAiModel}
              onAiModelChange={(m) => void handleAiModelChange(m)}
              aiModelSwitchLoading={detailAiSlotLoading}
            />
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

export function ResultPanel({
  result,
  gameId,
  accent,
  title,
  loading,
  onRefresh,
  showLoadMore,
  onLoadMore,
  detailContext,
  valorantMmrPending,
}: {
  result: PlayerSearchResponse;
  gameId: string;
  accent: string;
  title: string;
  loading: boolean;
  onRefresh: () => void;
  showLoadMore?: boolean;
  onLoadMore?: () => void;
  detailContext: { puuid?: string; platform?: string };
  valorantMmrPending?: boolean;
}) {
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
          {info.avatarUrl ? (
            <img src={info.avatarUrl} alt="avatar" className="records-avatar" />
          ) : (
            <div className="records-avatar-fallback">{getInitials(info.gameName || result.nickname)}</div>
          )}
          <div className="records-profile-text">
            <div className="records-result-label">{title}</div>
            <h2 className="records-profile-name">
              {info.gameName || result.nickname || 'Unknown Player'}
              {info.tagLine ? <span className="records-tag-line">#{info.tagLine}</span> : null}
            </h2>
            <div className="records-rank-row">
              {valorantMmrPending && gameId === 'valorant' ? (
                <span className="records-soft-badge records-tier-loading">티어 불러오는 중…</span>
              ) : null}
              {!valorantMmrPending && info.tier && info.tier !== '…' ? (
                <span className="records-rank-badge">
                  {info.tier} {info.rank || ''}
                </span>
              ) : null}
              {info.lp ? <span className="records-soft-badge">{info.lp}</span> : null}
              {info.summonerLevel ? <span className="records-soft-badge">Lv.{info.summonerLevel}</span> : null}
            </div>
          </div>
        </div>
        <button
          type="button"
          className="records-result-refresh"
          disabled={loading}
          onClick={onRefresh}
          title="DB 캐시를 건너뛰고 API에서 최신 전적을 다시 받습니다"
        >
          {loading ? '갱신 중…' : '전적 갱신'}
        </button>
      </div>

      {result.matchListOnly ? (
        <p className="records-match-list-hint">
          최근 매치는 ID만 불러왔습니다. 각 행에서 「상세 전적」을 누르면 그때 매치 상세 API를 호출합니다.
        </p>
      ) : (
        <div className="records-stat-grid">
          {statCards.map((item) => (
            <div key={item.label} className="records-stat-card">
              <span className="records-stat-label">{item.label}</span>
              <strong className="records-stat-value">{item.value}</strong>
            </div>
          ))}
        </div>
      )}

      <div className="records-result-body">
        <div className="records-section-card">
          <div className="records-section-head">
            <h3>최근 매치</h3>
            <span>{result.matches?.length || 0} games</span>
          </div>
          {result.matches?.length ? (
            <>
              <div className="records-match-list">
                {result.matches.map((match, index) => (
                  <MatchRow
                    key={match.matchId || `${index}-${match.gameMode || 'match'}`}
                    gameId={gameId}
                    match={match}
                    detailContext={detailContext}
                  />
                ))}
              </div>
              {showLoadMore && onLoadMore ? (
                <div className="records-load-more-wrap">
                  <button
                    type="button"
                    className="records-load-more-btn"
                    disabled={loading}
                    onClick={onLoadMore}
                  >
                    {loading ? '불러오는 중…' : '더보기'}
                  </button>
                </div>
              ) : null}
            </>
          ) : (
            <p className="records-empty-matches">표시할 최근 전적이 없습니다.</p>
          )}
        </div>
      </div>
    </section>
  );
}
