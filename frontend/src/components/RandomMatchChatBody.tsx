import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PositionPicker from './PositionPicker';
import { useAuth } from '../contexts/AuthContext';
import {
  joinMatchQueue,
  leaveMatchQueue,
  getMatchQueueStatus,
  getMyMatchSessions,
  deleteMatchSession,
  type MatchSessionListItem,
} from '../api/match';
import {
  RANDOM_MATCH_AND_ROOM_GAME_OPTIONS,
  PUBG_PLATFORM_RADIO,
  PUBG_RANDOM_MAP_CHIPS,
  getMatchModeOptions,
  getControlledPartyOptions,
  tierOptionsForGame,
  createFormShowTier,
  MATCH_GAME_LABELS,
  describeRandomMatchSummary,
  isLolAram,
  isLolSoloRank,
  positionRequiredForRandomMatch,
  getPubgRandomMatchRuleError,
  isSimpleFivePersonRandomMatch,
  isSimplePubgRandomMatch,
} from '../utils/randomMatchHelpers';
import {
  saveRandomMatchPending,
  loadRandomMatchPending,
  clearRandomMatchPending,
} from '../utils/randomMatchPendingStorage';
import MatchChatPanel from './MatchChatPanel';

type Props = {
  enabled: boolean;
  onClose?: () => void;
  /** true: 세션 링크는 SPA 이동만 (위젯은 열린 채) */
  navigateInPlace?: boolean;
  /** true: 플로팅 위젯 등에서 페이지 이동 없이 MatchChatPanel로 이어 채팅 */
  embedMatchChat?: boolean;
};

function IconListPeople() {
  return (
    <svg viewBox="0 0 24 24" width="40" height="40" aria-hidden className="random-match-chat-modal-icon">
      <circle cx="9" cy="8" r="3.5" fill="none" stroke="#7c4dff" strokeWidth="1.8" />
      <path
        fill="none"
        stroke="#7c4dff"
        strokeWidth="1.8"
        strokeLinecap="round"
        d="M4 20v-1a5 5 0 0 1 5-5h0a5 5 0 0 1 5 5v1"
      />
      <circle cx="17" cy="9" r="2.8" fill="none" stroke="#9575cd" strokeWidth="1.6" />
      <path fill="none" stroke="#9575cd" strokeWidth="1.6" strokeLinecap="round" d="M21 20v-0.8a3.5 3.5 0 0 0-2.5-3.3" />
    </svg>
  );
}

export default function RandomMatchChatBody({ enabled, onClose, navigateInPlace, embedMatchChat }: Props) {
  const { user, loading: authLoading } = useAuth();
  const navigate = useNavigate();

  const [openSessionId, setOpenSessionId] = useState<number | null>(null);
  const [matchInQueue, setMatchInQueue] = useState(false);
  const [matchLobbyCount, setMatchLobbyCount] = useState<number | null>(null);
  const [matchTargetSize, setMatchTargetSize] = useState<number | null>(null);
  const [matchJoining, setMatchJoining] = useState(false);
  const [matchGame, setMatchGame] = useState('LEAGUE_OF_LEGENDS');
  const [matchMode, setMatchMode] = useState('');
  const [matchTier, setMatchTier] = useState('');
  const [matchPartySize, setMatchPartySize] = useState('');
  const [matchPosition, setMatchPosition] = useState<string | null>(null);
  const [matchPlatform, setMatchPlatform] = useState<'STEAM' | 'KAKAO' | ''>('');
  const [matchPerspective, setMatchPerspective] = useState<'TPP' | 'FPP' | ''>('');
  const [pubgPreferredMap, setPubgPreferredMap] = useState('ALL');

  const [sessions, setSessions] = useState<MatchSessionListItem[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const controlledPartyOptions = getControlledPartyOptions(matchGame, matchMode, matchTier);
  const positionRequired = positionRequiredForRandomMatch(matchGame, matchMode);
  const positionDisabled =
    isLolAram(matchGame, matchMode) || (matchGame === 'OVERWATCH' && matchMode === 'OPEN_QUEUE');
  const partySizeDisabled = isLolSoloRank(matchGame, matchMode);
  const overwatchPositionRequired = matchGame === 'OVERWATCH' && (matchMode === 'ROLE_QUEUE_COMP' || matchMode === 'QUICK_PLAY');

  useEffect(() => {
    const modeOptions = getMatchModeOptions(matchGame);
    if (modeOptions.length === 0) {
      if (matchMode !== '') setMatchMode('');
      return;
    }
    const hasMode = modeOptions.some((o) => o.value === matchMode);
    if (!hasMode) setMatchMode(modeOptions[0]?.value ?? '');
  }, [matchGame, matchMode]);

  useEffect(() => {
    const options = getControlledPartyOptions(matchGame, matchMode, matchTier);
    if (options.length === 0) {
      if (matchPartySize !== '') setMatchPartySize('');
      return;
    }
    const enabledOptions = options.filter((o) => !o.disabled);
    if (enabledOptions.length === 0) {
      if (matchPartySize !== '') setMatchPartySize('');
      return;
    }

    // 오버워치는 "규칙 위반" 조합을 선택한 상태에서 매칭 시작 버튼을 막고 안내 문구를 보여줘야 하므로,
    // 현재 선택값이 disabled여도 자동으로 교정하지 않는다.
    if (matchGame === 'OVERWATCH') {
      const current = options.find((o) => o.value === matchPartySize);
      if (current && current.disabled) return;
    }
    if (matchGame === 'PUBG') {
      const current = options.find((o) => o.value === matchPartySize);
      if (current && current.disabled) return;
    }
    if (matchGame === 'COUNTER_STRIKE_2') {
      const current = options.find((o) => o.value === matchPartySize);
      if (current && current.disabled) return;
    }

    const hasEnabledParty = enabledOptions.some((o) => o.value === matchPartySize);
    if (!hasEnabledParty) setMatchPartySize(enabledOptions[0]?.value ?? '');
  }, [matchGame, matchMode, matchPartySize, matchTier]);

  useEffect(() => {
    if (!createFormShowTier(matchGame, matchMode) && matchTier !== '') setMatchTier('');
  }, [matchGame, matchMode, matchTier]);

  useEffect(() => {
    if (isSimpleFivePersonRandomMatch(matchGame)) {
      if (matchGame === 'LEAGUE_OF_LEGENDS') {
        const valid = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'];
        if (!matchPosition || !valid.includes(matchPosition)) setMatchPosition('TOP');
        return;
      }
      if (matchGame === 'VALORANT') {
        const valid = ['DUELIST', 'SCOUT', 'STRATEGIST', 'WATCHER'];
        if (!matchPosition || !valid.includes(matchPosition)) setMatchPosition('DUELIST');
        return;
      }
      if (matchGame === 'OVERWATCH') {
        const valid = ['TANK', 'DAMAGE', 'SUPPORT'];
        if (!matchPosition || !valid.includes(matchPosition)) setMatchPosition('TANK');
        return;
      }
      if (matchGame === 'COUNTER_STRIKE_2') {
        const valid = ['ENTRY', 'SUPPORT', 'IGL', 'AWPER', 'LURKER'];
        if (!matchPosition || !valid.includes(matchPosition)) setMatchPosition('ENTRY');
        return;
      }
    }
    if (positionRequired) {
      if (!matchPosition && matchGame === 'LEAGUE_OF_LEGENDS') setMatchPosition('TOP');
      return;
    }
    if (matchPosition != null) setMatchPosition(null);
  }, [positionRequired, matchPosition, matchGame]);

  // 오버워치: 6인(고정)은 자유 모드(오픈 큐)에서만 이용하도록 모드 자동 보정
  useEffect(() => {
    if (matchGame !== 'OVERWATCH') return;
    if (matchPartySize !== '6') return;
    if (matchMode !== 'OPEN_QUEUE') setMatchMode('OPEN_QUEUE');
  }, [matchGame, matchPartySize, matchMode]);

  useEffect(() => {
    if (matchGame !== 'PUBG') return;
    if (matchMode === 'RANKED') setMatchPerspective('');
    if (matchMode === 'NORMAL' && !matchPerspective) setMatchPerspective('TPP');
  }, [matchGame, matchMode, matchPerspective]);

  useEffect(() => {
    if (matchGame !== 'PUBG' || matchMode !== 'RANKED') return;
    if (matchPartySize === 'DUO' || matchPartySize === 'ONE_MAN_SQUAD') setMatchPartySize('SOLO');
  }, [matchGame, matchMode, matchPartySize]);

  const valorantRuleWarning =
    matchGame === 'VALORANT' && matchMode === 'COMPETITIVE' && (matchTier === 'IMMORTAL' || matchTier === 'RADIANT')
      ? matchTier === 'RADIANT'
        ? '레디언트는 2인 이하만 가능합니다.'
        : '불멸 이상은 1~2인만 가능합니다.'
      : '';

  const valorantRuleError = (() => {
    if (matchGame !== 'VALORANT' || matchMode !== 'COMPETITIVE') return '';
    if (matchPartySize === '4') return '경쟁전은 4인 큐가 불가능합니다.';
    const restricted = matchTier === 'IMMORTAL' || matchTier === 'RADIANT';
    if (restricted && matchPartySize && !['1', '2'].includes(matchPartySize)) {
      return matchTier === 'RADIANT' ? '레디언트는 2인 이하만 가능합니다.' : '불멸 이상은 1~2인만 가능합니다.';
    }
    return '';
  })();

  const overwatchRuleError = (() => {
    if (matchGame !== 'OVERWATCH') return '';

    if (overwatchPositionRequired && !matchPosition) {
      return '역할 고정 모드에서는 탱커/딜러/힐러 중 1개를 선택해야 매칭할 수 있습니다.';
    }

    if (matchMode !== 'ROLE_QUEUE_COMP') return '';

    const gmPlus = matchTier === 'GRANDMASTER' || matchTier === 'TOP500';
    if (gmPlus && matchPartySize && !['', '1', '2'].includes(matchPartySize)) {
      return '그랜드마스터 등급은 최대 2인까지만 그룹이 가능합니다.';
    }

    // 방어적: 규칙 위반이 이미 선택되어 있어도 비활성화/안내를 위해 잡아냄
    if (matchPartySize === '6' && matchMode !== 'OPEN_QUEUE') {
      return '특수 인원(6인)은 자유 모드에서만 이용할 수 있습니다.';
    }

    return '';
  })();

  const pubgPartyOption = matchGame === 'PUBG' ? controlledPartyOptions.find((o) => o.value === matchPartySize) : undefined;
  const pubgRuleError =
    matchGame === 'PUBG'
      ? getPubgRandomMatchRuleError({
          mode: matchMode,
          platform: matchPlatform,
          perspective: matchPerspective,
          partySize: matchPartySize,
          partyOptionDisabled: !!pubgPartyOption?.disabled,
        })
      : '';

  const applyQueueStatus = useCallback((s: Awaited<ReturnType<typeof getMatchQueueStatus>>) => {
    setMatchInQueue(s.inQueue);
    if (s.inQueue && s.lobbyCount != null && s.targetSize != null) {
      setMatchLobbyCount(s.lobbyCount);
      setMatchTargetSize(s.targetSize);
    } else {
      setMatchLobbyCount(null);
      setMatchTargetSize(null);
    }
  }, []);

  const refreshQueue = useCallback(() => {
    if (!user) return;
    getMatchQueueStatus().then(applyQueueStatus);
  }, [user, applyQueueStatus]);

  const refreshSessions = useCallback(() => {
    if (!user) return;
    setSessionsLoading(true);
    getMyMatchSessions()
      .then(setSessions)
      .finally(() => setSessionsLoading(false));
  }, [user]);

  useEffect(() => {
    if (!enabled || authLoading) return;
    refreshQueue();
    if (user) refreshSessions();
  }, [enabled, authLoading, user, refreshQueue, refreshSessions]);

  useEffect(() => {
    if (!enabled || authLoading || !user || !matchInQueue) return;
    const t = setInterval(() => {
      getMatchQueueStatus().then(applyQueueStatus);
    }, 1500);
    return () => clearInterval(t);
  }, [enabled, authLoading, user, matchInQueue, applyQueueStatus]);

  useEffect(() => {
    if (!enabled) setOpenSessionId(null);
  }, [enabled]);

  const handleRandomMatch = async () => {
    if (!user) return;

    if (isSimpleFivePersonRandomMatch(matchGame)) {
      if (!matchPosition) {
        window.alert(matchGame === 'VALORANT' ? '역할군을 선택해 주세요' : '포지션을 선택해 주세요');
        return;
      }
      setMatchJoining(true);
      const res = await joinMatchQueue({
        game: matchGame,
        position: matchPosition,
        preferredRole: matchGame === 'VALORANT' ? matchPosition : undefined,
        preferredPosition: matchGame === 'OVERWATCH' ? matchPosition : undefined,
        tierPolicy: 'ANY',
      });
      applyQueueStatus({
        inQueue: res.inQueue,
        lobbyCount: res.lobbyCount,
        targetSize: res.targetSize,
      });
      if (res.inQueue) {
        saveRandomMatchPending({
          game: matchGame,
          mode: '',
          tier: '',
          partySize: '',
          position: matchPosition,
        });
      }
      setMatchJoining(false);
      return;
    }

    if (isSimplePubgRandomMatch(matchGame)) {
      if (matchPlatform !== 'STEAM' && matchPlatform !== 'KAKAO') {
        window.alert('플랫폼(스팀 또는 카카오)을 선택해 주세요');
        return;
      }
      setMatchJoining(true);
      const res = await joinMatchQueue({
        game: 'PUBG',
        pubgSimpleFourPerson: true,
        pubgPlatform: matchPlatform,
        pubgPreferredMap: pubgPreferredMap,
      });
      applyQueueStatus({
        inQueue: res.inQueue,
        lobbyCount: res.lobbyCount,
        targetSize: res.targetSize,
      });
      if (res.inQueue) {
        saveRandomMatchPending({
          game: 'PUBG',
          mode: '',
          tier: '',
          partySize: '',
          position: pubgPreferredMap,
          platform: matchPlatform,
        });
      }
      setMatchJoining(false);
      return;
    }

    if (valorantRuleError || overwatchRuleError || pubgRuleError) return;
    if (overwatchPositionRequired && !matchPosition) {
      window.alert('포지션을 선택해 주세요');
      return;
    }

    const matchConfig = {
      game: matchGame,
      mode: matchMode || undefined,
      partySize: matchPartySize || undefined,
      position: positionRequired ? (matchPosition ?? undefined) : undefined,
      targetTier: createFormShowTier(matchGame, matchMode) ? (matchTier.trim() || undefined) : undefined,
      tierPolicy: 'ANY' as const,
    };

    setMatchJoining(true);
    const res = await joinMatchQueue(matchConfig);
    applyQueueStatus({
      inQueue: res.inQueue,
      lobbyCount: res.lobbyCount,
      targetSize: res.targetSize,
    });
    if (res.inQueue) {
      saveRandomMatchPending({
        game: matchConfig.game,
        mode: matchConfig.mode ?? '',
        tier: matchTier ?? '',
        partySize: matchConfig.partySize ?? '',
        position: matchConfig.position ?? null,
      });
    }
    setMatchJoining(false);
  };

  const handleLeaveMatchQueue = async () => {
    await leaveMatchQueue();
    clearRandomMatchPending();
    setMatchInQueue(false);
    setMatchLobbyCount(null);
    setMatchTargetSize(null);
  };

  const handleDeleteSession = async (sessionId: number) => {
    if (deletingId != null) return;
    setDeletingId(sessionId);
    const ok = await deleteMatchSession(sessionId);
    setDeletingId(null);
    if (ok) setSessions((prev) => prev.filter((s) => s.id !== sessionId));
  };

  const formatSessionDate = (s: string) => {
    try {
      return new Date(s).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
    } catch {
      return s;
    }
  };

  const goMatchChat = (sessionId: number) => {
    if (embedMatchChat) {
      setOpenSessionId(sessionId);
      return;
    }
    if (navigateInPlace) {
      navigate(`/match-chat/${sessionId}`);
    }
  };

  const handleBackFromEmbeddedChat = () => {
    setOpenSessionId(null);
    refreshSessions();
  };

  const pendingSummary = loadRandomMatchPending();
  const waitingRows =
    matchInQueue && pendingSummary
      ? describeRandomMatchSummary(
          pendingSummary.game,
          pendingSummary.mode,
          pendingSummary.tier,
          pendingSummary.position,
          pendingSummary.partySize,
          pendingSummary.game === 'PUBG'
            ? { platform: pendingSummary.platform, perspective: pendingSummary.perspective }
            : undefined
        )
      : matchInQueue
        ? [{ label: '안내', value: '이 브라우저에서 매칭 시작 시 저장한 조건만 아래에 표시됩니다.' }]
        : [];

  if (!enabled) return null;

  if (user && embedMatchChat && openSessionId != null) {
    return (
      <div className="random-match-chat-modal-body random-match-chat-embed-session">
        <MatchChatPanel sessionId={openSessionId} embedded onBack={handleBackFromEmbeddedChat} />
      </div>
    );
  }

  const waitingBlock =
    matchInQueue ? (
      <div className="random-match-chat-waiting" role="status">
        <div className="random-match-chat-waiting-title">매칭 대기 중</div>
        <div className="random-match-chat-waiting-spinner" aria-hidden />
        <p className="random-match-chat-waiting-desc">
          {matchLobbyCount != null && matchTargetSize != null
            ? `매칭 중... (현재 ${matchLobbyCount} / ${matchTargetSize}명)`
            : '선택한 조건으로 상대를 찾고 있습니다.'}
        </p>
        <ul className="random-match-chat-conditions">
          {waitingRows.map((row) => (
            <li key={row.label}>
              <span className="random-match-chat-cond-label">{row.label}</span>
              <span className="random-match-chat-cond-value">{row.value}</span>
            </li>
          ))}
        </ul>
        <button type="button" className="random-match-chat-cancel-queue" onClick={handleLeaveMatchQueue}>
          매칭 취소
        </button>
      </div>
    ) : null;

  const formSection = !matchInQueue ? (
    <div className="random-match-chat-form-section">
      <h3 className="random-match-chat-form-heading">매칭 조건</h3>
      <p className="random-match-chat-form-note">아래 조건을 확인한 뒤 매칭을 시작하면 같은 조건이 팝업에 표시됩니다.</p>
      <div className="random-match-chat-form">
        <label className="random-match-chat-label">게임</label>
        <select
          className="random-match-chat-input"
          value={matchGame}
          onChange={(e) => {
            setMatchGame(e.target.value);
            setMatchMode('');
            setMatchPartySize('');
            setMatchPosition(null);
            setMatchPlatform('');
            setMatchPerspective('');
            setPubgPreferredMap('ALL');
          }}
        >
          {RANDOM_MATCH_AND_ROOM_GAME_OPTIONS.map((o) => (
            <option key={o.key} value={o.key}>
              {o.label}
            </option>
          ))}
        </select>
        {isSimpleFivePersonRandomMatch(matchGame) ? (
          <>
            <p className="random-match-chat-hint" style={{ marginTop: 4 }}>
              {matchGame === 'VALORANT'
                ? '역할군을 선택한 뒤 매칭을 시작하세요. 선착순으로 5명이 모이면 매칭됩니다.'
                : matchGame === 'OVERWATCH'
                  ? '포지션을 선택한 뒤 매칭을 시작하세요. 포지션이 겹쳐도 선착순 5명으로 매칭됩니다.'
                  : matchGame === 'COUNTER_STRIKE_2'
                    ? '포지션을 선택한 뒤 매칭을 시작하세요. 선착순 5명이 모이면 매칭됩니다.'
                : '포지션을 선택한 뒤 매칭을 시작하세요. 선착순으로 5명이 모이면 매칭됩니다.'}
            </p>
            <div className="random-match-position-block">
              <label className="random-match-chat-label">{matchGame === 'VALORANT' ? '역할군' : '포지션'}</label>
              <PositionPicker
                value={matchPosition}
                onChange={setMatchPosition}
                game={matchGame}
                filterMode
                includeAllOption={false}
                className="random-match-chat-position"
              />
            </div>
            <button
              type="button"
              className="random-match-chat-start-btn"
              onClick={handleRandomMatch}
              disabled={matchJoining}
            >
              {matchJoining ? '참가 중...' : '매칭 시작'}
            </button>
          </>
        ) : isSimplePubgRandomMatch(matchGame) ? (
          <>
            <label className="random-match-chat-label">플랫폼</label>
            <div className="pubg-radio-group" role="radiogroup" aria-label="PUBG 플랫폼">
              {PUBG_PLATFORM_RADIO.map((o) => (
                <label key={o.value} className={`pubg-radio-pill ${matchPlatform === o.value ? 'active' : ''}`}>
                  <input
                    type="radio"
                    name="rmc-pubg-platform"
                    value={o.value}
                    checked={matchPlatform === o.value}
                    onChange={() => setMatchPlatform(o.value)}
                  />
                  <span>{o.label}</span>
                </label>
              ))}
            </div>
            <p className="random-match-chat-hint">스팀과 카카오는 서로 다른 매칭 큐입니다. 플랫폼을 선택해 주세요.</p>
            <label className="random-match-chat-label">선호 맵</label>
            <div className="random-match-map-chips" role="group" aria-label="선호 맵">
              {PUBG_RANDOM_MAP_CHIPS.map((c) => (
                <button
                  key={c.value}
                  type="button"
                  className={`map-chip ${pubgPreferredMap === c.value ? 'active' : ''}`}
                  onClick={() => setPubgPreferredMap(c.value)}
                >
                  {c.label}
                </button>
              ))}
            </div>
            <button
              type="button"
              className="random-match-chat-start-btn"
              onClick={handleRandomMatch}
              disabled={matchJoining}
            >
              {matchJoining ? '참가 중...' : '매칭 시작'}
            </button>
          </>
        ) : (
          <>
        {getMatchModeOptions(matchGame).length > 0 && (
          <>
            <label className="random-match-chat-label">모드</label>
            <select className="random-match-chat-input" value={matchMode} onChange={(e) => setMatchMode(e.target.value)}>
              {getMatchModeOptions(matchGame).map((o) => (
                <option
                  key={o.value || '_'}
                  value={o.value}
                  disabled={
                    matchGame === 'OVERWATCH' &&
                    matchPartySize === '6' &&
                    (o.value === 'ROLE_QUEUE_COMP' || o.value === 'QUICK_PLAY' || o.value === '')
                  }
                >
                  {o.label}
                </option>
              ))}
            </select>
          </>
        )}
        {createFormShowTier(matchGame, matchMode) && (
          <>
            <label className="random-match-chat-label">티어</label>
            <select className="random-match-chat-input" value={matchTier} onChange={(e) => setMatchTier(e.target.value)}>
              {tierOptionsForGame(matchGame, matchMode).map((o) => (
                <option key={o.value || '_'} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </>
        )}
        {controlledPartyOptions.length > 0 && (
          <>
            <label className="random-match-chat-label">인원</label>
            <select
              className="random-match-chat-input"
              value={matchPartySize}
              onChange={(e) => setMatchPartySize(e.target.value)}
              disabled={partySizeDisabled}
            >
              {controlledPartyOptions.map((o) => (
                      <option key={o.value || '_'} value={o.value} disabled={o.disabled}>
                  {o.label}
                </option>
              ))}
            </select>
          </>
        )}
        <div className={positionDisabled ? 'random-match-position-block disabled' : 'random-match-position-block'}>
            <label className="random-match-chat-label">포지션</label>
            <PositionPicker
              value={matchPosition}
              onChange={setMatchPosition}
              game={matchGame}
              filterMode
              includeAllOption={false}
              disabled={positionDisabled}
              className="random-match-chat-position"
            />
        </div>
        <button
          type="button"
          className="random-match-chat-start-btn"
          onClick={handleRandomMatch}
          disabled={matchJoining || !!valorantRuleError || !!overwatchRuleError || !!pubgRuleError}
        >
          {matchJoining ? '참가 중...' : '매칭 시작'}
        </button>

        {overwatchRuleError ? (
          <div className="random-match-chat-form-error" role="alert">
            {overwatchRuleError}
          </div>
        ) : valorantRuleError ? (
          <div className="random-match-chat-form-error" role="alert">
            {valorantRuleError}
          </div>
        ) : pubgRuleError ? (
          <div className="random-match-chat-form-error" role="alert">
            {pubgRuleError}
          </div>
        ) : valorantRuleWarning ? (
          <div className="random-match-chat-form-warn">{valorantRuleWarning}</div>
        ) : null}
          </>
        )}
      </div>
    </div>
  ) : null;

  const sessionList = (
    <ul className="random-match-chat-session-list">
      {sessionsLoading ? (
        <li className="random-match-chat-session-empty">불러오는 중...</li>
      ) : sessions.length === 0 ? (
        <li className="random-match-chat-session-empty">아직 랜덤 매칭 채팅이 없습니다.</li>
      ) : (
        sessions.map((s) => (
          <li key={s.id} className="random-match-chat-session-list-item">
            <div className="random-match-chat-session-row">
              {embedMatchChat || navigateInPlace ? (
                <button
                  type="button"
                  className="random-match-chat-session-open"
                  onClick={() => goMatchChat(s.id)}
                  title="채팅 열기"
                >
                  <div className="random-match-chat-session-icon" aria-hidden>
                    <IconListPeople />
                  </div>
                  <span className="random-match-chat-session-link">
                    <span className="random-match-chat-session-name">{MATCH_GAME_LABELS[s.game] ?? s.game}</span>
                    <span className="random-match-chat-session-date">{formatSessionDate(s.createdAt)}</span>
                  </span>
                </button>
              ) : (
                <Link to={`/match-chat/${s.id}`} className="random-match-chat-session-open" onClick={onClose} title="채팅 열기">
                  <div className="random-match-chat-session-icon" aria-hidden>
                    <IconListPeople />
                  </div>
                  <span className="random-match-chat-session-link">
                    <span className="random-match-chat-session-name">{MATCH_GAME_LABELS[s.game] ?? s.game}</span>
                    <span className="random-match-chat-session-date">{formatSessionDate(s.createdAt)}</span>
                  </span>
                </Link>
              )}
              <button
                type="button"
                className="random-match-chat-session-delete"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  void handleDeleteSession(s.id);
                }}
                disabled={deletingId === s.id}
                title="목록에서 삭제"
              >
                {deletingId === s.id ? '…' : '삭제'}
              </button>
            </div>
          </li>
        ))
      )}
    </ul>
  );

  const historyToolbar = (
    <div className="random-match-chat-modal-toolbar">
      <span className="random-match-chat-modal-subtitle">채팅방 목록</span>
      <span className="random-match-chat-modal-toolbar-hint">
        {embedMatchChat
          ? '이전 채팅을 누르면 대화 내역을 보고 이어서 채팅할 수 있습니다.'
          : '매칭된 세션을 눌러 채팅으로 이동합니다.'}
      </span>
    </div>
  );

  return (
    <div className={embedMatchChat ? 'random-match-chat-modal-body random-match-chat-embed-tabbed' : 'random-match-chat-modal-body'}>
      {!user ? (
        <p className="random-match-chat-modal-login">
          <Link to="/login" onClick={onClose}>
            로그인
          </Link>
          후 랜덤 매칭과 채팅을 이용할 수 있습니다.
        </p>
      ) : embedMatchChat ? (
        <>
          <div className="floating-chat-widget-dm-scroll chat-popup-scroll">
            {historyToolbar}
            {sessionList}
          </div>
        </>
      ) : (
        <>
          {historyToolbar}
          {waitingBlock}
          {formSection}
          {sessionList}
        </>
      )}
    </div>
  );
}
