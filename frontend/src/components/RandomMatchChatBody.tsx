import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PositionPicker from './PositionPicker';
import { useAuth } from '../contexts/AuthContext';
import {
  joinMatchQueue,
  leaveMatchQueue,
  getMatchQueueStatus,
  getLolMatchQueueStatus,
  leaveLolMatchQueue,
  getMyMatchSessions,
  deleteMatchSession,
  type MatchSessionListItem,
} from '../api/match';
import {
  GAME_OPTIONS,
  getMatchModeOptions,
  tierOptionsForGame,
  createFormShowTier,
  MATCH_GAME_LABELS,
  describeRandomMatchSummary,
  isLolAram,
  positionRequiredForRandomMatch,
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
  const [matchJoining, setMatchJoining] = useState(false);
  const [matchGame, setMatchGame] = useState('LEAGUE_OF_LEGENDS');
  const [matchMode, setMatchMode] = useState('');
  const [matchTier, setMatchTier] = useState('');
  const [matchPosition, setMatchPosition] = useState<string | null>(null);
  const [myPosition, setMyPosition] = useState<string | null>(null);
  const [partnerPosition, setPartnerPosition] = useState<string | null>(null);
  const [primaryRole, setPrimaryRole] = useState<string | null>(null);
  const [secondaryRole, setSecondaryRole] = useState<string | null>(null);
  const [quickFindPosition, setQuickFindPosition] = useState<string | null>(null);

  const [isMatching, setIsMatching] = useState(false);
  const [currentParticipants, setCurrentParticipants] = useState(0);
  const maxParticipants = 5;

  const [sessions, setSessions] = useState<MatchSessionListItem[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const positionRequired = positionRequiredForRandomMatch(matchGame, matchMode);
  const positionDisabled = isLolAram(matchGame, matchMode);

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
    if (positionRequired) {
      if (!matchPosition) setMatchPosition('TOP');
      return;
    }
    if (matchPosition != null) setMatchPosition(null);
  }, [positionRequired, matchPosition]);

  useEffect(() => {
    // LoL 전용 포지션 UI 기본값
    if (matchGame !== 'LEAGUE_OF_LEGENDS') return;
    if (matchMode === 'QUICK') {
      if (!primaryRole) setPrimaryRole('TOP');
      if (!secondaryRole) setSecondaryRole('JUNGLE');
      if (!quickFindPosition) setQuickFindPosition('MID');
      return;
    }
    if (!myPosition) setMyPosition('TOP');
    if (!partnerPosition) setPartnerPosition('JUNGLE');
  }, [matchGame, matchMode, myPosition, partnerPosition, primaryRole, secondaryRole, quickFindPosition]);

  useEffect(() => {
    // LoL 매칭은 tier가 필수라 기본값 세팅
    if (matchGame !== 'LEAGUE_OF_LEGENDS') return;
    if (!matchTier) setMatchTier('GOLD');
  }, [matchGame, matchTier]);

  const refreshQueue = useCallback(() => {
    if (!user) return;
    if (matchGame === 'LEAGUE_OF_LEGENDS') {
      getLolMatchQueueStatus()
        .then((s) => {
          setMatchInQueue(Boolean(s.inQueue));
          setIsMatching(Boolean(s.inQueue));
          setCurrentParticipants(Number(s.currentParticipants ?? 0));
        })
        .catch(() => setMatchInQueue(false));
      return;
    }
    getMatchQueueStatus().then(setMatchInQueue);
  }, [user]);

  useEffect(() => {
    if (!user) return;
    if (!enabled) return;
    if (matchGame !== 'LEAGUE_OF_LEGENDS') return;
    if (!isMatching) return;

    const timer = window.setInterval(() => {
      getLolMatchQueueStatus()
        .then((s) => {
          setMatchInQueue(Boolean(s.inQueue));
          setIsMatching(Boolean(s.inQueue));
          const cnt = Number(s.currentParticipants ?? 0);
          setCurrentParticipants(cnt);
          if (cnt >= maxParticipants) {
            // 실제 매칭 완료는 서버가 WebSocket으로 통지하지만, UI상 즉시 성공 피드백 제공
            window.alert('매칭 성공!');
          }
        })
        .catch(() => {});
    }, 1200);
    return () => window.clearInterval(timer);
  }, [enabled, isMatching, matchGame, user]);

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
    if (!enabled) setOpenSessionId(null);
  }, [enabled]);

  const handleRandomMatch = async () => {
    if (!user) return;
    if (matchGame === 'LEAGUE_OF_LEGENDS') {
      if (!matchMode) {
        window.alert('큐 타입을 선택해 주세요');
        return;
      }
      if (!matchTier) {
        window.alert('티어를 선택해 주세요');
        return;
      }
      if (matchMode === 'QUICK') {
        if (!primaryRole || !secondaryRole || !quickFindPosition) {
          window.alert('포지션을 선택해 주세요');
          return;
        }
      } else {
        if (!myPosition || !partnerPosition) {
          window.alert('포지션을 선택해 주세요');
          return;
        }
      }
    } else if (positionRequired && !matchPosition) {
      window.alert('포지션을 선택해 주세요');
      return;
    }

    const lolPositionForQueue =
      matchGame === 'LEAGUE_OF_LEGENDS'
        ? (matchMode === 'QUICK' ? quickFindPosition : myPosition) ?? undefined
        : undefined;

    const matchConfig = {
      game: matchGame,
      mode: matchMode || undefined,
      position: matchGame === 'LEAGUE_OF_LEGENDS'
        ? lolPositionForQueue
        : positionRequired ? (matchPosition ?? undefined) : undefined,
      // 티어는 자유 매칭 기준에서 희망 타겟으로만 사용
      targetTier: matchGame === 'LEAGUE_OF_LEGENDS'
        ? (matchTier.trim() || undefined)
        : createFormShowTier(matchGame, matchMode) ? (matchTier.trim() || undefined) : undefined,
      tierPolicy: matchGame === 'LEAGUE_OF_LEGENDS' ? ('TARGET_ONLY' as const) : ('ANY' as const),
    };

    setMatchJoining(true);
    const res = await joinMatchQueue(matchConfig);
    setMatchInQueue(res.inQueue);
    if (res.inQueue) {
      setIsMatching(true);
      saveRandomMatchPending({
        game: matchConfig.game,
        mode: matchConfig.mode ?? '',
        tier: matchConfig.targetTier ?? '',
        partySize: '',
        position: matchConfig.position ?? null,
      });
    }
    setMatchJoining(false);
  };

  const handleLeaveMatchQueue = async () => {
    if (matchGame === 'LEAGUE_OF_LEGENDS') {
      await leaveLolMatchQueue();
    } else {
      await leaveMatchQueue();
    }
    clearRandomMatchPending();
    setMatchInQueue(false);
    setIsMatching(false);
    setCurrentParticipants(0);
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
          pendingSummary.partySize
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
        <p className="random-match-chat-waiting-desc">선택한 조건으로 상대를 찾고 있습니다.</p>
        <ul className="random-match-chat-conditions">
          {waitingRows.map((row) => (
            <li key={row.label}>
              <span className="random-match-chat-cond-label">{row.label}</span>
              <span className="random-match-chat-cond-value">{row.value}</span>
            </li>
          ))}
        </ul>
        {isMatching && (
          <div className="random-match-chat-waiting-desc" style={{ marginTop: 10, fontWeight: 700 }}>
            현재 인원: {currentParticipants}/{maxParticipants}
          </div>
        )}
        <button type="button" className="random-match-chat-cancel-queue" onClick={handleLeaveMatchQueue}>
          대기 취소
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
          }}
        >
          {GAME_OPTIONS.map((o) => (
            <option key={o.key} value={o.key}>
              {o.label}
            </option>
          ))}
        </select>
        {getMatchModeOptions(matchGame).length > 0 && (
          <>
            <label className="random-match-chat-label">큐 타입</label>
            <select className="random-match-chat-input" value={matchMode} onChange={(e) => setMatchMode(e.target.value)}>
              {getMatchModeOptions(matchGame).map((o) => (
                <option key={o.value || '_'} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </>
        )}
        {(matchGame === 'LEAGUE_OF_LEGENDS' || createFormShowTier(matchGame, matchMode)) && (
          <>
            <label className="random-match-chat-label">티어</label>
            <select className="random-match-chat-input" value={matchTier} onChange={(e) => setMatchTier(e.target.value)}>
              {tierOptionsForGame(matchGame)
                .filter((o) => matchGame !== 'LEAGUE_OF_LEGENDS' || o.value !== '')
                .map((o) => (
                <option key={o.value || '_'} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </>
        )}
        {matchGame === 'LEAGUE_OF_LEGENDS' ? (
          matchMode === 'QUICK' ? (
            <>
              <label className="random-match-chat-label">주 역할군</label>
              <PositionPicker value={primaryRole} onChange={setPrimaryRole} game={matchGame} filterMode includeAllOption={false} className="random-match-chat-position" />
              <label className="random-match-chat-label">부 역할군</label>
              <PositionPicker value={secondaryRole} onChange={setSecondaryRole} game={matchGame} filterMode includeAllOption={false} className="random-match-chat-position" />
              <label className="random-match-chat-label">찾는 포지션</label>
              <PositionPicker value={quickFindPosition} onChange={setQuickFindPosition} game={matchGame} filterMode includeAllOption={false} className="random-match-chat-position" />
            </>
          ) : (
            <>
              <label className="random-match-chat-label">나의 포지션</label>
              <PositionPicker value={myPosition} onChange={setMyPosition} game={matchGame} filterMode includeAllOption={false} className="random-match-chat-position" />
              <label className="random-match-chat-label">찾는 포지션</label>
              <PositionPicker value={partnerPosition} onChange={setPartnerPosition} game={matchGame} filterMode includeAllOption={false} className="random-match-chat-position" />
            </>
          )
        ) : matchGame !== 'PUBG' ? (
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
        ) : null}
        <button type="button" className="random-match-chat-start-btn" onClick={handleRandomMatch} disabled={matchJoining}>
          {matchJoining ? '참가 중...' : '매칭 시작'}
        </button>
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
