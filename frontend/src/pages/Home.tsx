import { useEffect, useState, useCallback, useMemo } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import LiveThumb from '../components/LiveThumb';
import PositionPicker from '../components/PositionPicker';
import PositionIcon from '../components/PositionIcon';
import { useAuth } from '../contexts/AuthContext';
import { resolveProfileImageUrl } from '../api/client';
import {
  createGameRoom,
  fetchGameRoomList,
  joinGameRoom,
  getGameRoomChatRoomId,
  type GameRoomItem,
} from '../api/gameRooms';
import { fetchLiveStreams, type StreamItem } from '../api/streams';
import {
  joinMatchQueue,
  leaveMatchQueue,
  getMatchQueueStatusDetail,
  getLolMatchQueueStatus,
  leaveLolMatchQueue,
  getMyMatchSessions,
  deleteMatchSession,
  type MatchSessionListItem,
} from '../api/match';
import {
  TIER_OPTIONS,
  VALORANT_TIER_OPTIONS,
  RANK_OPTIONS,
  VALORANT_MODE_OPTIONS,
  OVERWATCH_MODE_OPTIONS,
  PUBG_MODE_OPTIONS,
  PUBG_PLATFORM_OPTIONS,
  GAME_OPTIONS,
  getMatchModeOptions,
  getControlledPartyOptions,
  tierOptionsForGame,
  createFormShowTier,
  MATCH_GAME_LABELS,
  tierLabel,
  rankLabel,
  modeLabel,
  modeHasNoTier,
  showPositionForRoom,
  partySizeLabel,
  isLolAram,
  isLolSoloRank,
  positionRequiredForRandomMatch,
} from '../utils/randomMatchHelpers';
import { saveRandomMatchPending, clearRandomMatchPending } from '../utils/randomMatchPendingStorage';
import { isHiddenGameRoomHost } from '../utils/gameRoomVisibility';
import './Home.css';

function parseGameOptions(s?: string | null): {
  tier?: string;
  mode?: string;
  position?: string;
  maxPlayers?: number;
  myPosition?: string;
  partnerPosition?: string;
  primaryRole?: string;
  secondaryRole?: string;
  findPosition?: string;
  preferredMap?: string;
  preferredMethod?: string;
  preferredLegend?: string;
  platform?: string;
  partySize?: string;
} {
  if (!s || !s.trim()) return {};
  try {
    const o = JSON.parse(s) as Record<string, unknown>;
    return {
      tier: typeof o.tier === 'string' ? o.tier : undefined,
      mode: typeof o.mode === 'string' ? o.mode : undefined,
      position: typeof o.position === 'string' ? o.position : undefined,
      maxPlayers: typeof o.maxPlayers === 'number' ? o.maxPlayers : undefined,
      myPosition: typeof o.myPosition === 'string' ? o.myPosition : undefined,
      partnerPosition: typeof o.partnerPosition === 'string' ? o.partnerPosition : undefined,
      primaryRole: typeof o.primaryRole === 'string' ? o.primaryRole : undefined,
      secondaryRole: typeof o.secondaryRole === 'string' ? o.secondaryRole : undefined,
      findPosition: typeof o.findPosition === 'string' ? o.findPosition : undefined,
      preferredMap: typeof o.preferredMap === 'string' ? o.preferredMap : undefined,
      preferredMethod: typeof o.preferredMethod === 'string' ? o.preferredMethod : undefined,
      preferredLegend: typeof o.preferredLegend === 'string' ? o.preferredLegend : undefined,
      platform: typeof o.platform === 'string' ? o.platform : undefined,
      partySize: typeof o.partySize === 'string' ? o.partySize : undefined,
    };
  } catch {
    return {};
  }
}

function formatDateForRoom(s: string) {
  try {
    return new Date(s).toLocaleString('ko-KR', {
      month: '2-digit',
      day: '2-digit',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return s;
  }
}

function extraColumnValue(op: ReturnType<typeof parseGameOptions>, roomGame: string) {
  if (roomGame === 'PUBG') return op.platform ? (PUBG_PLATFORM_OPTIONS.find((x) => x.value === op.platform)?.label ?? op.platform) : '-';
  if (roomGame === 'COUNTER_STRIKE_2') return op.preferredMethod || '-';
  // APEX 제거: 남겨진 표시 로직 없음
  return '-';
}

type SidebarTab = 'random' | 'history' | 'create';

const HOME_QUICK_LINKS = [
  { to: '/records', eyebrow: '전적 검색', title: '연동된 게임 전적 확인', body: '지원 게임 전적을 검색하고 최근 플레이 흐름을 한곳에서 비교할 수 있습니다.' },
  { to: '/community', eyebrow: '커뮤니티', title: '실시간 소통 바로가기', body: '게시글 확인, 글 작성, 팀 찾기 흐름을 한 화면 안에서 이어갈 수 있습니다.' },
  { to: '/chatbot', eyebrow: '도우미', title: '앱 내 챗봇 이용', body: '기능 위치나 사용 방법이 헷갈릴 때 바로 물어보고 도움을 받을 수 있습니다.' },
];

export default function Home() {
  const { user, loading: authLoading } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>('create');
  /** Random match / history / create-room panel (same toggle as former Team Searching header). */
  const [showMatchingSidebar, setShowMatchingSidebar] = useState(false);
  const [matchInQueue, setMatchInQueue] = useState(false);
  const [matchJoining, setMatchJoining] = useState(false);
  const [matchGame, setMatchGame] = useState('LEAGUE_OF_LEGENDS');
  const [matchMode, setMatchMode] = useState('');
  const [matchTier, setMatchTier] = useState('');
  const [matchPartySize, setMatchPartySize] = useState('');
  const [matchPosition, setMatchPosition] = useState<string | null>(null);
  const [isMatching, setIsMatching] = useState(false);
  const [participantCount, setParticipantCount] = useState(0);
  const [participantMax, setParticipantMax] = useState(0);

  const expectedMaxPlayers = useMemo(() => {
    if (matchGame === 'LEAGUE_OF_LEGENDS') return 5;
    if (matchGame === 'VALORANT') return 5;
    if (matchGame === 'OVERWATCH') return 5;
    if (matchGame === 'PUBG') return matchPartySize === 'SQUAD' ? 4 : 2;
    if (matchGame === 'COUNTER_STRIKE_2') return matchMode === 'WINGMAN' ? 2 : 5;
    // 기본 랜덤매칭(기존 로직): 2인 매칭
    return 2;
  }, [matchGame, matchMode, matchPartySize]);
  const [liveStreams, setLiveStreams] = useState<StreamItem[]>([]);
  const [loadingLive, setLoadingLive] = useState(false);
  const [matchHistoryList, setMatchHistoryList] = useState<MatchSessionListItem[]>([]);
  const [matchHistoryLoading, setMatchHistoryLoading] = useState(false);
  const [matchHistoryDeletingId, setMatchHistoryDeletingId] = useState<number | null>(null);
  const controlledPartyOptions = getControlledPartyOptions(matchGame, matchMode);
  const positionRequired = positionRequiredForRandomMatch(matchGame, matchMode);
  const positionDisabled = isLolAram(matchGame, matchMode);
  const partySizeDisabled = isLolSoloRank(matchGame, matchMode);

  const [createGame, setCreateGame] = useState('LEAGUE_OF_LEGENDS');
  const [createTitle, setCreateTitle] = useState('');
  const [createTier, setCreateTier] = useState('');
  const [createRank, setCreateRank] = useState('SOLO');
  const [createPosition, setCreatePosition] = useState<string | null>(null);
  const [createMyPosition, setCreateMyPosition] = useState<string | null>(null);
  const [createPartnerPosition, setCreatePartnerPosition] = useState<string | null>(null);
  const [createPrimaryRole, setCreatePrimaryRole] = useState<string | null>(null);
  const [createSecondaryRole, setCreateSecondaryRole] = useState<string | null>(null);
  const [createFindPosition, setCreateFindPosition] = useState<string | null>(null);
  const [createMode, setCreateMode] = useState('');
  const [createPreferredMap, setCreatePreferredMap] = useState('');
  const [createPreferredMethod, setCreatePreferredMethod] = useState('');
  const [createPreferredLegend, setCreatePreferredLegend] = useState('');
  const [createPlatform, setCreatePlatform] = useState('');
  const [createPartySize, setCreatePartySize] = useState('');
  const [createPassword, setCreatePassword] = useState('');
  const [createMemo, setCreateMemo] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [roomList, setRoomList] = useState<GameRoomItem[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [joinRoomId, setJoinRoomId] = useState<number | null>(null);

  const fetchRooms = useCallback(() => {
    setLoadingRooms(true);
    fetchGameRoomList(undefined, false)
      .then(setRoomList)
      .finally(() => setLoadingRooms(false));
  }, []);

  useEffect(() => {
    if (authLoading) return;
    fetchRooms();
  }, [authLoading, fetchRooms]);

  useEffect(() => {
    if (authLoading) return;
    // 방 목록 인원 현황을 자연스럽게 갱신 (예: 1/5 → 2/5)
    const timer = window.setInterval(() => {
      fetchRooms();
    }, 4000);
    return () => window.clearInterval(timer);
  }, [authLoading, fetchRooms]);

  const visibleRoomList = useMemo(
    () => roomList.filter((r) => !isHiddenGameRoomHost(r.hostNickname)),
    [roomList],
  );

  useEffect(() => {
    if (searchParams.get('oauth2_error') === 'not_configured') {
      setSearchParams({}, { replace: true });
      navigate('/login?error=oauth_not_configured', { replace: true });
    }
  }, [searchParams, setSearchParams, navigate]);

  const fetchMatchHistory = useCallback(() => {
    if (!user) return;
    setMatchHistoryLoading(true);
    getMyMatchSessions()
      .then(setMatchHistoryList)
      .finally(() => setMatchHistoryLoading(false));
  }, [user]);

  useEffect(() => {
    const open = sidebarTab === 'history';
    if (open && user) fetchMatchHistory();
  }, [sidebarTab, user, fetchMatchHistory]);

  const formatHistoryTime = (createdAt: string) => {
    try {
      return new Date(createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: true });
    } catch {
      return '';
    }
  };

  const handleDeleteMatchHistory = async (sessionId: number) => {
    if (matchHistoryDeletingId != null) return;
    setMatchHistoryDeletingId(sessionId);
    const ok = await deleteMatchSession(sessionId);
    setMatchHistoryDeletingId(null);
    if (ok) setMatchHistoryList((prev) => prev.filter((s) => s.id !== sessionId));
  };

  useEffect(() => {
    if (authLoading) return;
    setLoadingLive(true);
    fetchLiveStreams().then(setLiveStreams).finally(() => setLoadingLive(false));
    const t = setInterval(() => fetchLiveStreams().then(setLiveStreams), 5000);
    return () => clearInterval(t);
  }, [authLoading]);

  useEffect(() => {
    if (authLoading || !user) return;
    // 게임별 대기열 상태를 먼저 동기화 (이미 대기열 참가 중인데도 "매칭 시작"을 다시 눌러 중복 참가 오류가 나지 않게)
    if (matchGame === 'LEAGUE_OF_LEGENDS') {
      getLolMatchQueueStatus()
        .then((s) => {
          const inQ = Boolean(s.inQueue);
          setMatchInQueue(inQ);
          setIsMatching(inQ);
          setParticipantCount(Number(s.currentParticipants ?? 0));
          setParticipantMax(Number(s.maxParticipants ?? 5));
        })
        .catch(() => {
          setMatchInQueue(false);
          setIsMatching(false);
          setParticipantCount(0);
          setParticipantMax(0);
        });
      return;
    }
    getMatchQueueStatusDetail()
      .then((s) => {
        const inQ = Boolean(s.inQueue);
        setMatchInQueue(inQ);
        setIsMatching(inQ);
        setParticipantCount(Number(s.currentParticipants ?? 0));
        setParticipantMax(Number(s.maxParticipants ?? expectedMaxPlayers));
      })
      .catch(() => {
        setMatchInQueue(false);
        setIsMatching(false);
        setParticipantCount(0);
        setParticipantMax(0);
      });
  }, [authLoading, user, matchGame, expectedMaxPlayers]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    // LoL 방 만들기 기본 포지션 세팅
    if (createRank === 'QUICK') {
      if (!createPrimaryRole) setCreatePrimaryRole('TOP');
      if (!createSecondaryRole) setCreateSecondaryRole('JUNGLE');
      if (!createFindPosition) setCreateFindPosition('MID');
      return;
    }
    if (!createMyPosition) setCreateMyPosition('TOP');
    if (!createPartnerPosition) setCreatePartnerPosition('JUNGLE');
  }, [
    createGame,
    createRank,
    createMyPosition,
    createPartnerPosition,
    createPrimaryRole,
    createSecondaryRole,
    createFindPosition,
  ]);

  useEffect(() => {
    if (createGame !== 'PUBG') return;
    if (!createPlatform) setCreatePlatform('STEAM');
    if (!createPartySize) setCreatePartySize('DUO');
    if (!createMode) setCreateMode('NORMAL');
  }, [createGame, createPlatform, createPartySize, createMode]);

  useEffect(() => {
    if (createGame !== 'COUNTER_STRIKE_2') return;
    if (!createMode) setCreateMode('PREMIER');
  }, [createGame, createMode]);

  useEffect(() => {
    if (matchGame !== 'PUBG') return;
    if (!matchMode) setMatchMode('NORMAL');
    if (!matchPartySize) setMatchPartySize('DUO');
  }, [matchGame, matchMode, matchPartySize]);

  useEffect(() => {
    if (!user) return;
    if (!isMatching) return;
    const timer = window.setInterval(() => {
      if (matchGame === 'LEAGUE_OF_LEGENDS') {
        getLolMatchQueueStatus()
          .then((s) => {
            if (!s.inQueue) {
              setIsMatching(false);
              setParticipantCount(0);
              setParticipantMax(0);
              setMatchInQueue(false);
              return;
            }
            const cnt = Number(s.currentParticipants ?? 0);
            setParticipantCount(cnt);
            const mx = Number(s.maxParticipants ?? 5);
            setParticipantMax(mx);
            if (cnt >= mx) window.alert('매칭 성공!');
          })
          .catch(() => {});
        return;
      }
      getMatchQueueStatusDetail()
        .then((s) => {
          if (!s.inQueue) {
            setIsMatching(false);
            setParticipantCount(0);
            setParticipantMax(0);
            setMatchInQueue(false);
            return;
          }
          const cnt = Number(s.currentParticipants ?? 0);
          setParticipantCount(cnt);
          const mx = Number(s.maxParticipants ?? expectedMaxPlayers);
          setParticipantMax(mx);
          if (cnt >= mx) window.alert('매칭 성공!');
        })
        .catch(() => {});
    }, 1200);
    return () => window.clearInterval(timer);
  }, [isMatching, user, matchGame, expectedMaxPlayers]);

  useEffect(() => {
    const modeOptions = getMatchModeOptions(matchGame);
    if (modeOptions.length === 0) {
      if (matchMode !== '') setMatchMode('');
      return;
    }
    if (!modeOptions.some((o) => o.value === matchMode)) {
      setMatchMode(modeOptions[0]?.value ?? '');
    }
  }, [matchGame, matchMode]);

  useEffect(() => {
    if (controlledPartyOptions.length === 0) {
      if (matchPartySize !== '') setMatchPartySize('');
      return;
    }
    if (!controlledPartyOptions.some((o) => o.value === matchPartySize)) {
      setMatchPartySize(controlledPartyOptions[0]?.value ?? '');
    }
  }, [controlledPartyOptions, matchPartySize]);

  useEffect(() => {
    if (positionRequired) {
      if (!matchPosition) setMatchPosition('TOP');
      return;
    }
    if (matchPosition != null) setMatchPosition(null);
  }, [positionRequired, matchPosition]);

  useEffect(() => {
    // LoL 전용 매칭 API는 tier가 필수라 UI에서 숨겨도 기본값을 유지
    if (matchGame !== 'LEAGUE_OF_LEGENDS') return;
    if (!matchTier) setMatchTier('GOLD');
  }, [matchGame, matchTier]);

  const handleRandomMatch = async () => {
    if (!user) return;
    if (matchJoining) return;
    if (positionRequired && !matchPosition) {
      window.alert('포지션을 선택해 주세요');
      return;
    }

    const matchConfig = {
      game: matchGame,
      mode: matchMode || undefined,
      partySize: matchGame === 'COUNTER_STRIKE_2' ? undefined : (matchPartySize || undefined),
      position: positionRequired ? (matchPosition ?? undefined) : undefined,
      targetTier:
        matchGame === 'LEAGUE_OF_LEGENDS'
          ? (matchTier.trim() || 'GOLD')
          : createFormShowTier(matchGame, matchMode) ? (matchTier.trim() || undefined) : undefined,
      tierPolicy: matchGame === 'LEAGUE_OF_LEGENDS' ? ('TARGET_ONLY' as const) : ('ANY' as const),
      maxPlayers: expectedMaxPlayers,
    };

    setMatchJoining(true);
    try {
      const res = await joinMatchQueue(matchConfig);
      setMatchInQueue(res.inQueue);
      if (res.inQueue) {
        setIsMatching(true);
        setParticipantCount(1);
        setParticipantMax(expectedMaxPlayers);
        saveRandomMatchPending({
          game: matchConfig.game,
          mode: matchConfig.mode ?? '',
          tier: matchConfig.targetTier ?? '',
          partySize: matchConfig.partySize ?? '',
          position: matchConfig.position ?? null,
        });
      } else {
        window.alert(res.message || '매칭 시작에 실패했습니다. 잠시 후 다시 시도해 주세요.');
      }
    } catch {
      window.alert('매칭 시작에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setMatchJoining(false);
    }
  };

  const handleLeaveMatchQueue = async () => {
    if (matchGame === 'LEAGUE_OF_LEGENDS') await leaveLolMatchQueue();
    else await leaveMatchQueue();
    clearRandomMatchPending();
    setMatchInQueue(false);
    setIsMatching(false);
    setParticipantCount(0);
    setParticipantMax(0);
  };

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      navigate('/login');
      return;
    }
    const trimmedTitle = createTitle.trim();
    if (!trimmedTitle) {
      setCreateError('제목을 입력해 주세요.');
      return;
    }
    if (!createPassword.trim()) {
      setCreateError('방 삭제용 비밀번호를 입력해 주세요.');
      return;
    }
    const platformPrefix =
      createGame === 'PUBG' && createPlatform === 'STEAM' ? '[스팀] '
      : createGame === 'PUBG' && createPlatform === 'KAKAO' ? '[카카오] '
      : '';
    const title = (platformPrefix + trimmedTitle).slice(0, 200);
    setCreating(true);
    setCreateError('');
    const isLolCreate = createGame === 'LEAGUE_OF_LEGENDS';
    const isValorantCreate = createGame === 'VALORANT';
    const isOverwatchCreate = createGame === 'OVERWATCH';
    const isPubgCreate = createGame === 'PUBG';
    const isCs2Create = createGame === 'COUNTER_STRIKE_2';
    const lolMode = isLolCreate ? (createRank || undefined) : undefined;
    const isLolQuick = isLolCreate && lolMode === 'QUICK';
    const lolFind = (isLolQuick ? createFindPosition : createPartnerPosition) ?? undefined;
    const pubgMaxPlayers = isPubgCreate ? (createPartySize === 'SQUAD' ? 4 : 2) : undefined;
    const cs2MaxPlayers = isCs2Create ? (createMode === 'WINGMAN' ? 2 : 5) : undefined;
    const gameOptions = JSON.stringify({
      tier: isCs2Create ? undefined : (createTier || undefined),
      mode: (isLolCreate ? lolMode : (createMode || undefined)) ?? undefined,
      // 목록 표시/필터용 기본 position은 "찾는 포지션"으로 통일
      position: (isLolCreate ? lolFind : (createPosition || undefined)) ?? undefined,
      maxPlayers: (isLolCreate || isValorantCreate || isOverwatchCreate) ? 5 : (isCs2Create ? cs2MaxPlayers : pubgMaxPlayers),
      // LoL 전용 포지션 정보
      myPosition: isLolCreate && !isLolQuick ? (createMyPosition ?? undefined) : undefined,
      partnerPosition: isLolCreate && !isLolQuick ? (createPartnerPosition ?? undefined) : undefined,
      primaryRole: isLolCreate && isLolQuick ? (createPrimaryRole ?? undefined) : undefined,
      secondaryRole: isLolCreate && isLolQuick ? (createSecondaryRole ?? undefined) : undefined,
      findPosition: isLolCreate && isLolQuick ? (createFindPosition ?? undefined) : undefined,
      preferredMap: createPreferredMap.trim() || undefined,
      preferredMethod: isCs2Create ? undefined : (createPreferredMethod.trim() || undefined),
      preferredLegend: createPreferredLegend.trim() || undefined,
      platform: createPlatform || undefined,
      partySize: (isLolCreate || isValorantCreate || isOverwatchCreate || isCs2Create) ? undefined : (createPartySize || undefined),
    });
    const res = await createGameRoom({
      title,
      memo: createMemo.trim() || undefined,
      deletePassword: createPassword,
      game: createGame,
      gameOptions,
    });
    setCreating(false);
    if (res.ok) {
      setCreateTitle('');
      setCreateMemo('');
      setCreatePassword('');
      setCreatePosition(null);
      setCreateMyPosition(null);
      setCreatePartnerPosition(null);
      setCreatePrimaryRole(null);
      setCreateSecondaryRole(null);
      setCreateFindPosition(null);
      setCreateMode('');
      setCreatePreferredMap('');
      setCreatePreferredMethod('');
      setCreatePreferredLegend('');
      setCreatePlatform('');
      setCreatePartySize('');
      setShowMatchingSidebar(false);
      fetchRooms();
    } else {
      setCreateError(res.message || '방 만들기에 실패했습니다.');
    }
  };

  const goGameRoomChat = (r: GameRoomItem) => {
    if (r.groupChatRoomId == null) return;
    navigate(`/group-chat/room/${r.groupChatRoomId}`, { state: { fromGameRoom: true, gameRoomId: r.id } });
  };

  const handleApiRoomButton = async (r: GameRoomItem) => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (r.closed) return;
    setJoinRoomId(r.id);
    const ok = await joinGameRoom(r.id);
    setJoinRoomId(null);
    if (ok) {
      const chatRoomId = await getGameRoomChatRoomId(r.id);
      if (chatRoomId != null) {
        navigate(`/group-chat/room/${chatRoomId}`, { state: { fromGameRoom: true, gameRoomId: r.id } });
      }
      fetchRooms();
    }
  };

  /** Random-match sidebar panel (shared with fixed sidebar). */
  const randomPanelContent = (
    <div className="sidebar-panel random-panel">
      <h3 className="sidebar-panel-title">랜덤 매칭</h3>
      {user ? (
        matchInQueue ? (
          <>
            <p className="sidebar-panel-desc">매칭 참여 중입니다.</p>
            <p className="sidebar-panel-desc" style={{ marginTop: -6 }}>
              실시간 갱신: 주기적으로 상태를 조회해서 1/2, 2/5처럼 실시간으로 숫자가 업데이트됩니다
            </p>
            {isMatching && (
              <div className="sidebar-panel-desc" style={{ marginTop: -4, fontWeight: 700 }}>
                현재 인원: {participantCount}/{participantMax || expectedMaxPlayers}
              </div>
            )}
            <button type="button" className="sidebar-btn secondary" onClick={handleLeaveMatchQueue}>취소</button>
          </>
        ) : (
          <>
            <p className="sidebar-panel-desc">같은 조건의 유저와 매칭됩니다.</p>
            <div className="random-match-form">
              <label className="sidebar-form-label">게임</label>
              <select className="sidebar-form-input" value={matchGame} onChange={(e) => { setMatchGame(e.target.value); setMatchMode(''); setMatchPartySize(''); }}>
                {GAME_OPTIONS.map((o) => (
                  <option key={o.key} value={o.key}>{o.label}</option>
                ))}
              </select>
              {getMatchModeOptions(matchGame).length > 0 && (
                <>
                  <label className="sidebar-form-label">모드</label>
                  <select className="sidebar-form-input" value={matchMode} onChange={(e) => setMatchMode(e.target.value)}>
                    {getMatchModeOptions(matchGame).map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              {createFormShowTier(matchGame, matchMode) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={matchTier} onChange={(e) => setMatchTier(e.target.value)}>
                    {tierOptionsForGame(matchGame).map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              {matchGame !== 'LEAGUE_OF_LEGENDS' && matchGame !== 'VALORANT' && matchGame !== 'OVERWATCH' && matchGame !== 'COUNTER_STRIKE_2' && controlledPartyOptions.length > 0 && (
                <>
                  <label className="sidebar-form-label">인원</label>
                  <select
                    className="sidebar-form-input"
                    value={matchPartySize}
                    onChange={(e) => setMatchPartySize(e.target.value)}
                    disabled={partySizeDisabled}
                  >
                    {controlledPartyOptions.map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              {matchGame !== 'PUBG' && (
                <div className={positionDisabled ? 'random-match-position-block disabled' : 'random-match-position-block'}>
                  <label className="sidebar-form-label">포지션</label>
                  <PositionPicker
                    value={matchPosition}
                    onChange={setMatchPosition}
                    game={matchGame}
                    filterMode
                    includeAllOption={false}
                    disabled={positionDisabled}
                    className="random-match-position"
                  />
                </div>
              )}
              <button
                type="button"
                className="sidebar-btn primary"
                onClick={handleRandomMatch}
                aria-busy={matchJoining}
                style={{ marginTop: 16, opacity: matchJoining ? 0.7 : 1 }}
              >
                {matchJoining ? '참가 중...' : '매칭 시작'}
              </button>
              {matchInQueue && isMatching && (
                <div className="sidebar-panel-desc" style={{ marginTop: 10, fontWeight: 700 }}>
                  현재 인원: {participantCount}/{participantMax || expectedMaxPlayers}
                </div>
              )}
            </div>
          </>
        )
      ) : (
        <Link to="/login" className="sidebar-btn primary">로그인하고 매칭</Link>
      )}
    </div>
  );

  const historyPanelContent = (
    <div className="sidebar-panel history-panel">
      <h3 className="sidebar-panel-title">랜덤 매칭 내역</h3>
      <p className="sidebar-panel-desc">매칭 후 채팅방을 다시 볼 수 있습니다.</p>
      {!user ? (
        <Link to="/login" className="sidebar-btn primary">로그인하고 보기</Link>
      ) : matchHistoryLoading ? (
        <div className="duo-empty">로딩 중...</div>
      ) : matchHistoryList.length === 0 ? (
        <div className="duo-empty">아직 매칭 내역이 없습니다.</div>
      ) : (
        <ul className="match-history-list match-history-list-inline">
          {matchHistoryList.map((s) => (
            <li key={s.id} className="match-history-item">
              <Link to={`/match-chat/${s.id}`} className="match-history-link" onClick={() => setShowMatchingSidebar(false)}>
                <div className="match-history-avatar">
                  <span className="match-history-avatar-initial">{(MATCH_GAME_LABELS[s.game] ?? s.game)[0]}</span>
                </div>
                <div className="match-history-content">
                  <span className="match-history-name">{MATCH_GAME_LABELS[s.game] ?? s.game}</span>
                  <span className="match-history-preview">채팅 보기</span>
                </div>
                <span className="match-history-time">{formatHistoryTime(s.createdAt)}</span>
              </Link>
              <button
                type="button"
                className="match-history-delete-btn"
                onClick={(e) => { e.preventDefault(); handleDeleteMatchHistory(s.id); }}
                disabled={matchHistoryDeletingId === s.id}
                title="내역 삭제"
                aria-label="내역 삭제"
              >
                {matchHistoryDeletingId === s.id ? '삭제 중...' : '삭제'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );

  const createPanelContent = (
    <div className="sidebar-panel create-panel" role="region" aria-labelledby="create-room-heading">
      <h3 id="create-room-heading" className="sidebar-panel-title">방 만들기</h3>
      <form id="create-room-form" className="create-room-form" onSubmit={handleCreateRoom} aria-label="방 만들기">
        <fieldset className="create-room-fieldset">
          <legend className="create-room-legend">방 정보</legend>

          <label className="sidebar-form-label">게임</label>
          <select className="sidebar-form-input" value={createGame} onChange={(e) => setCreateGame(e.target.value)}>
            {GAME_OPTIONS.map((o) => (
              <option key={o.key} value={o.key}>{o.label}</option>
            ))}
          </select>

          <label className="sidebar-form-label">제목</label>
          <input
            type="text"
            className="sidebar-form-input"
            value={createTitle}
            onChange={(e) => setCreateTitle(e.target.value)}
            placeholder={createGame === 'PUBG' ? '예: 아이디X' : '방 제목을 입력해 주세요'}
            maxLength={200}
          />

          {createGame === 'LEAGUE_OF_LEGENDS' && (
            <>
              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createRank} onChange={(e) => setCreateRank(e.target.value)}>
                {RANK_OPTIONS.filter((o) => o.value).map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              {createFormShowTier('LEAGUE_OF_LEGENDS', createRank) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={createTier} onChange={(e) => setCreateTier(e.target.value)}>
                    {TIER_OPTIONS.map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              {createRank === 'QUICK' ? (
                <>
                  <label className="sidebar-form-label">주 역할군</label>
                  <PositionPicker value={createPrimaryRole} onChange={setCreatePrimaryRole} game={createGame} filterMode includeAllOption={false} />
                  <label className="sidebar-form-label">부 역할군</label>
                  <PositionPicker value={createSecondaryRole} onChange={setCreateSecondaryRole} game={createGame} filterMode includeAllOption={false} />
                  <label className="sidebar-form-label">찾는 포지션</label>
                  <PositionPicker value={createFindPosition} onChange={setCreateFindPosition} game={createGame} filterMode includeAllOption={false} />
                </>
              ) : (
                <>
                  <label className="sidebar-form-label">나의 포지션</label>
                  <PositionPicker value={createMyPosition} onChange={setCreateMyPosition} game={createGame} filterMode includeAllOption={false} />
                  <label className="sidebar-form-label">찾는 포지션</label>
                  <PositionPicker value={createPartnerPosition} onChange={setCreatePartnerPosition} game={createGame} filterMode includeAllOption={false} />
                </>
              )}
            </>
          )}

          {createGame === 'VALORANT' && (
            <>
              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createMode} onChange={(e) => setCreateMode(e.target.value)}>
                {VALORANT_MODE_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              {createFormShowTier('VALORANT', createMode) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={createTier} onChange={(e) => setCreateTier(e.target.value)}>
                    {VALORANT_TIER_OPTIONS.map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              <label className="sidebar-form-label">역할</label>
              <PositionPicker value={createPosition} onChange={setCreatePosition} game={createGame} />
            </>
          )}

          {createGame === 'OVERWATCH' && (
            <>
              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createMode} onChange={(e) => setCreateMode(e.target.value)}>
                {OVERWATCH_MODE_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              {createFormShowTier('OVERWATCH', createMode) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={createTier} onChange={(e) => setCreateTier(e.target.value)}>
                    {TIER_OPTIONS.map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              <label className="sidebar-form-label">역할</label>
              <PositionPicker value={createPosition} onChange={setCreatePosition} game={createGame} />
            </>
          )}

          {createGame === 'PUBG' && (
            <>
              <label className="sidebar-form-label">플랫폼</label>
              <div className="create-toggle-row" role="radiogroup" aria-label="플랫폼 선택">
                <button
                  type="button"
                  className={`create-toggle-btn ${createPlatform === 'STEAM' || !createPlatform ? 'active' : ''}`}
                  onClick={() => setCreatePlatform('STEAM')}
                >
                  스팀
                </button>
                <button
                  type="button"
                  className={`create-toggle-btn ${createPlatform === 'KAKAO' ? 'active' : ''}`}
                  onClick={() => setCreatePlatform('KAKAO')}
                >
                  카카오
                </button>
              </div>

              <label className="sidebar-form-label">파티</label>
              <div className="create-toggle-row" role="radiogroup" aria-label="파티 선택">
                <button
                  type="button"
                  className={`create-toggle-btn ${createPartySize === 'DUO' || !createPartySize ? 'active' : ''}`}
                  onClick={() => setCreatePartySize('DUO')}
                >
                  듀오(2인)
                </button>
                <button
                  type="button"
                  className={`create-toggle-btn ${createPartySize === 'SQUAD' ? 'active' : ''}`}
                  onClick={() => setCreatePartySize('SQUAD')}
                >
                  스쿼드(4인)
                </button>
              </div>

              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createMode} onChange={(e) => setCreateMode(e.target.value)}>
                {PUBG_MODE_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">선호 맵(경쟁전 제외 선택)</label>
              <input type="text" className="sidebar-form-input" value={createPreferredMap} onChange={(e) => setCreatePreferredMap(e.target.value)} placeholder="선호 맵(선택)" maxLength={100} />
            </>
          )}

          {createGame === 'COUNTER_STRIKE_2' && (
            <>
              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createMode} onChange={(e) => setCreateMode(e.target.value)}>
                <option value="PREMIER">프리미어</option>
                <option value="COMPETITIVE">경쟁</option>
                <option value="WINGMAN">윙맨</option>
              </select>
              <label className="sidebar-form-label">포지션</label>
              <PositionPicker value={createPosition} onChange={setCreatePosition} game={createGame} />
            </>
          )}


          <label className="sidebar-form-label">글 삭제용 비밀번호</label>
          <input type="password" className="sidebar-form-input" value={createPassword} onChange={(e) => setCreatePassword(e.target.value)} placeholder="삭제 시 입력할 비밀번호" />

          <label className="sidebar-form-label">{createGame === 'PUBG' ? '방에 대한 메모' : '짧은 메모'}</label>
          <textarea
            className="sidebar-form-input sidebar-form-textarea"
            value={createMemo}
            onChange={(e) => setCreateMemo(e.target.value)}
            placeholder={'찾는 조건이나 하고 싶은 말을 적어주세요'}
            rows={3}
          />

          {createError && <p className="sidebar-form-error">{createError}</p>}

          <div className="sidebar-form-actions">
            <button type="submit" className="sidebar-btn primary" disabled={creating}>{creating ? '만드는 중...' : '방 만들기'}</button>
            <button
              type="button"
              className="sidebar-btn secondary"
              onClick={() => {
                setCreateTitle('');
                setCreateMemo('');
                setCreatePassword('');
                setCreateError('');
                setCreateMode('');
                setCreatePreferredMap('');
                setCreatePreferredMethod('');
                setCreatePreferredLegend('');
                setCreatePlatform('');
                setCreatePartySize('');
                setCreatePosition(null);
                setShowMatchingSidebar(false);
              }}
            >
              취소
            </button>
          </div>
        </fieldset>
      </form>
    </div>
  );

  return (
    <Layout>
      <section className="home-hero" aria-label="GameMatcher overview">
        <div className="home-hero-copy">
          <span className="home-hero-eyebrow">메인 프론트</span>
          <h1 className="home-hero-title">매칭, 방송, 팀 관리까지 한곳에서 이어집니다.</h1>
          <p className="home-hero-body">
            이제 메인 `frontend` 기준으로 사용자 흐름이 정리되어, 실시간 방송 확인부터 방 생성, 매칭 기록, 보조 기능까지 하나의 동선으로 사용할 수 있습니다.
          </p>
          <div className="home-hero-actions">
            <button type="button" className="home-hero-primary" onClick={() => { setSidebarTab('create'); setShowMatchingSidebar(true); }}>
              방 만들기
            </button>
            <button type="button" className="home-hero-secondary" onClick={() => { setSidebarTab('random'); setShowMatchingSidebar(true); }}>
              매칭 시작
            </button>
          </div>
        </div>
        <div className="home-hero-grid">
          {HOME_QUICK_LINKS.map((item) => (
            <Link key={item.to} to={item.to} className="home-hero-card">
              <span className="home-hero-card-eyebrow">{item.eyebrow}</span>
              <strong className="home-hero-card-title">{item.title}</strong>
              <span className="home-hero-card-body">{item.body}</span>
            </Link>
          ))}
        </div>
      </section>
      <section className="live-section" aria-label="지금 라이브">
        <div className="live-section-header">
          <h2 className="live-section-title"><span className="icon">??</span> 지금 라이브</h2>
          <Link to="/streams" className="link-all">전체보기</Link>
        </div>
        {loadingLive ? (
          <div className="live-empty">로딩 중...</div>
        ) : liveStreams.length === 0 ? (
          <div className="live-empty">현재 라이브 방송이 없습니다. <Link to="/streams">방송 페이지</Link>에서 더 보기</div>
        ) : (
          <div className="live-cards">
            {liveStreams.slice(0, 6).map((s) => (
              <Link key={s.id} to={`/watch/${s.id}`} className="live-card">
                <div className="live-card-thumb">
                  <div className="live-card-thumb-inner">
                    {s.playbackUrl ? (
                      <LiveThumb playbackUrl={s.playbackUrl} className="card-thumb-preview" />
                    ) : (
                      <div className="thumb-placeholder">라이브</div>
                    )}
                  </div>
                  <span className="live-badge">생방송</span>
                  {s.viewerCount != null && <span className="watching">{s.viewerCount} 명 시청</span>}
                </div>
                <div className="live-card-info">
                  <div className="live-card-avatar">
                    {resolveProfileImageUrl(s.broadcasterProfileImageUrl) ? (
                      <img src={resolveProfileImageUrl(s.broadcasterProfileImageUrl)!} alt="" />
                    ) : (
                      <span>{(s.broadcasterNickname || '?')[0]}</span>
                    )}
                  </div>
                  <div className="live-card-meta">
                    <div className="live-card-name">{s.broadcasterNickname || '?'}</div>
                    <div className="live-card-game">{s.title || '방송 중'}</div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      <div className={`main-matching-layout ${showMatchingSidebar ? 'matching-sidebar-visible' : ''}`}>
        <div className="home-matching-main">
          <div className="section-card home-matching-toolbar">
            <div className="home-matching-toolbar-actions">
              <button type="button" className={`team-search-quick-btn ${showMatchingSidebar && sidebarTab === 'random' ? 'active' : ''}`} onClick={() => { if (showMatchingSidebar && sidebarTab === 'random') setShowMatchingSidebar(false); else { setSidebarTab('random'); setShowMatchingSidebar(true); } }}>
                랜덤 매칭
              </button>
              <button type="button" className={`team-search-quick-btn ${showMatchingSidebar && sidebarTab === 'history' ? 'active' : ''}`} onClick={() => { if (showMatchingSidebar && sidebarTab === 'history') setShowMatchingSidebar(false); else { setSidebarTab('history'); setShowMatchingSidebar(true); } }}>
                랜덤 매칭 내역
              </button>
              <button type="button" className={`team-search-quick-btn ${showMatchingSidebar && sidebarTab === 'create' ? 'active' : ''}`} onClick={() => { if (showMatchingSidebar && sidebarTab === 'create') setShowMatchingSidebar(false); else { setSidebarTab('create'); setShowMatchingSidebar(true); } }}>
                방 만들기
              </button>
            </div>
          </div>
          <section className="section-card home-demo-room-section" aria-labelledby="home-demo-room-heading">
            <div className="home-demo-room-header">
              <div>
                <h2 id="home-demo-room-heading" className="home-demo-room-title">팀 찾기</h2>
                <p className="home-demo-room-sub">팀 검색 조건 · 서버에 등록된 열린 방 목록</p>
              </div>
            </div>
            <div className="home-demo-room-table-wrap">
              <table className="home-demo-room-table">
                <thead>
                  <tr>
                    <th>제목</th>
                    <th>티어</th>
                    <th>랭크</th>
                    <th>포지션</th>
                    <th>비고</th>
                    <th>인원</th>
                    <th>방장</th>
                    <th>등록일</th>
                    <th>참가</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingRooms && roomList.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="home-demo-room-loading-cell">방 목록 불러오는 중…</td>
                    </tr>
                  ) : visibleRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="home-demo-room-loading-cell">등록된 방이 없습니다.</td>
                    </tr>
                  ) : null}
                  {visibleRoomList.map((r) => {
                    const op = parseGameOptions(r.gameOptions);
  const maxP =
    (r.game === 'LEAGUE_OF_LEGENDS' || r.game === 'VALORANT' || r.game === 'OVERWATCH')
      ? (op.maxPlayers ?? 5)
      : r.game === 'PUBG'
        ? (op.maxPlayers ?? (op.partySize === 'SQUAD' ? 4 : 2))
        : r.game === 'COUNTER_STRIKE_2'
          ? (op.maxPlayers ?? (op.mode === 'WINGMAN' ? 2 : 5))
          : null;
                    const tierCell = modeHasNoTier(r.game, op.mode)
                      ? '-'
                      : op.tier
                        ? tierLabel(op.tier, r.game === 'PUBG' || r.game === 'VALORANT' ? r.game : undefined)
                        : '-';
                    let rankCell = '-';
                    if (r.game === 'LEAGUE_OF_LEGENDS') rankCell = rankLabel(op.mode ?? '');
                    else if (['VALORANT', 'OVERWATCH', 'PUBG'].includes(r.game)) rankCell = modeLabel(r.game, op.mode ?? '');
                    else rankCell = op.mode || '-';
                    let noteCell = extraColumnValue(op, r.game);
                    if (r.game === 'PUBG' && op.preferredMap) {
                      noteCell = noteCell !== '-' ? `${noteCell} · ${op.preferredMap}` : op.preferredMap;
                    }
                    const partyCell = partySizeLabel(op.partySize, r.game);
                    const showPos = r.game !== 'PUBG' && showPositionForRoom(r.game, op.mode) && op.position;
                    return (
                      <tr key={`api-${r.id}`}>
                        <td>{r.title}</td>
                        <td>{tierCell}</td>
                        <td>{rankCell}</td>
                        <td>
                          {showPos ? (
                            <span className="home-demo-room-position">
                              <PositionIcon position={op.position} game={r.game} showLabel />
                            </span>
                          ) : (
                            '-'
                          )}
                        </td>
                        <td>{noteCell}</td>
                        <td>
                          {maxP != null ? (
                            <span className="room-capacity-badge" title="현재 인원 / 최대 인원">
                              <span className="room-capacity-icon" aria-hidden>
                                <svg viewBox="0 0 24 24" width="14" height="14" focusable="false">
                                  <path
                                    d="M16 11c1.66 0 3-1.34 3-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3ZM8 11c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3Zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13Zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5C23 14.17 18.33 13 16 13Z"
                                    fill="currentColor"
                                  />
                                </svg>
                              </span>
                              {r.memberCount}/{maxP}
                            </span>
                          ) : (
                            partyCell
                          )}
                        </td>
                        <td>{r.hostNickname ?? '-'}</td>
                        <td>{formatDateForRoom(r.createdAt)}</td>
                        <td>
                          {r.closed ? (
                            <span className="home-demo-room-closed-label">마감</span>
                          ) : r.isMember && r.groupChatRoomId ? (
                            <button
                              type="button"
                              className="home-demo-room-join-btn home-demo-room-join-btn--live"
                              title="방 채팅으로 이동"
                              onClick={() => goGameRoomChat(r)}
                            >
                              입장
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="home-demo-room-join-btn home-demo-room-join-btn--live"
                              title={user ? '참가' : '로그인 후 참가'}
                              disabled={joinRoomId === r.id}
                              onClick={() => handleApiRoomButton(r)}
                            >
                              {joinRoomId === r.id ? '참가 중…' : '참가'}
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        {showMatchingSidebar && (
          <aside className="main-sidebar section-card sidebar-card matching-sidebar-panel">
            <div className="sidebar-tabs">
              <button type="button" className={sidebarTab === 'random' ? 'active' : ''} onClick={() => setSidebarTab('random')}>랜덤 매칭</button>
              <button type="button" className={sidebarTab === 'history' ? 'active' : ''} onClick={() => setSidebarTab('history')}>랜덤 매칭 내역</button>
              <button type="button" className={sidebarTab === 'create' ? 'active' : ''} onClick={() => setSidebarTab('create')}>방 만들기</button>
            </div>
            <div className="sidebar-content">
              {sidebarTab === 'random' && randomPanelContent}
              {sidebarTab === 'history' && historyPanelContent}
              {sidebarTab === 'create' && createPanelContent}
            </div>
          </aside>
        )}
      </div>
    </Layout>
  );
}
