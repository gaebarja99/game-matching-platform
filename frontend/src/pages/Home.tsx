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
} from '../api/match';
import {
  TIER_OPTIONS,
  VALORANT_TIER_OPTIONS,
  RANK_OPTIONS,
  VALORANT_MODE_OPTIONS,
  OVERWATCH_MODE_OPTIONS,
  PUBG_MODE_OPTIONS,
  GAME_OPTIONS,
  getMatchModeOptions,
  getControlledPartyOptions,
  tierOptionsForGame,
  createFormShowTier,
  rankLabel,
  modeLabel,
  showPositionForRoom,
  partySizeLabel,
  isLolAram,
  isLolSoloRank,
  positionRequiredForRandomMatch,
} from '../utils/randomMatchHelpers';
import { saveRandomMatchPending, clearRandomMatchPending } from '../utils/randomMatchPendingStorage';
import { isHiddenGameRoomHost } from '../utils/gameRoomVisibility';
import { getRoomCapacityMeta } from '../utils/gameRoomCapacity';
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

function formatRelativeCreatedAt(s: string) {
  try {
    const d = new Date(s);
    const now = new Date();
    if (Number.isNaN(d.getTime())) return s;
    const isSameDay =
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate();
    if (!isSameDay) return formatDateForRoom(s);

    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin <= 0) return '방금 전';
    if (diffMin < 60) return `${diffMin}분 전`;
    const diffHr = Math.floor(diffMin / 60);
    return `${diffHr}시간 전`;
  } catch {
    return s;
  }
}

type TeamPanelType = 'match' | 'create';

/** 팀 찾기 테이블 게임 필터 (방 `game` 필드와 동일) */
type TeamSearchGameId =
  | 'ALL'
  | 'LEAGUE_OF_LEGENDS'
  | 'VALORANT'
  | 'OVERWATCH'
  | 'PUBG'
  | 'COUNTER_STRIKE_2';

const TEAM_SEARCH_GAME_TABS: { id: TeamSearchGameId; label: string }[] = [
  { id: 'ALL', label: '전체' },
  { id: 'LEAGUE_OF_LEGENDS', label: '리그오브레전드' },
  { id: 'VALORANT', label: '발로란트' },
  { id: 'OVERWATCH', label: '오버워치2' },
  { id: 'PUBG', label: 'PUBG' },
  { id: 'COUNTER_STRIKE_2', label: 'CS2' },
];

function TeamSearchGameTabIcon({ game }: { game: TeamSearchGameId }) {
  const svgProps = { width: 22, height: 22, viewBox: '0 0 24 24' as const, 'aria-hidden': true as const };
  switch (game) {
    case 'ALL':
      return (
        <svg {...svgProps}>
          <circle cx="12" cy="12" r="9.5" fill="#9CA3AF" opacity="0.25" />
          <path d="M6.8 12h10.4" stroke="#9CA3AF" strokeWidth="2" strokeLinecap="round" />
          <path d="M12 6.8v10.4" stroke="#9CA3AF" strokeWidth="2" strokeLinecap="round" opacity="0.85" />
        </svg>
      );
    case 'LEAGUE_OF_LEGENDS':
      return (
        <svg {...svgProps}>
          <circle cx="12" cy="12" r="10" fill="#C8AA6E" />
          <path fill="#010A13" d="M8 7.5h3.2v8.4h4.8V17H8V7.5z" />
        </svg>
      );
    case 'VALORANT':
      return (
        <svg {...svgProps}>
          <path fill="#FF4655" d="M6 18 12 6h2.2L18 18h-2.6l-1.2-3.2H10.8L9.6 18H6zm5.7-5.5h2.6L13 9.8 11.7 12.5z" />
        </svg>
      );
    case 'OVERWATCH':
      return (
        <svg {...svgProps}>
          <circle cx="12" cy="12" r="9.5" fill="#FF9C23" opacity="0.95" />
          <circle cx="12" cy="12" r="6" fill="#1a1a1d" />
          <circle cx="12" cy="12" r="3" fill="#FF9C23" />
        </svg>
      );
    case 'PUBG':
      return (
        <svg {...svgProps}>
          <rect x="3" y="3" width="18" height="18" rx="3" fill="#E0BC5B" />
          <circle cx="12" cy="11" r="2.4" fill="none" stroke="#2a1f0f" strokeWidth="1.8" />
          <path stroke="#2a1f0f" strokeWidth="1.4" d="M12 8v6M9 11h6" strokeLinecap="round" />
        </svg>
      );
    case 'COUNTER_STRIKE_2':
      return (
        <svg {...svgProps}>
          <path fill="#4A90D9" d="M12 3 20 8v8l-8 5-8-5V8l8-5zm0 2.5L6 9v6l6 3.8L18 15V9l-6-3.5z" />
          <path fill="#1e3a5f" d="m12 8.5 4 2.3V15l-4 2.5-4-2.5v-4.2l4-2.3z" />
        </svg>
      );
  }
}

export default function Home() {
  const { user, loading: authLoading } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  /** 우측 팀 찾기 패널: 랜덤 매칭 / 방 만들기 */
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [panelType, setPanelType] = useState<TeamPanelType>('match');
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
  const [selectedGame, setSelectedGame] = useState<TeamSearchGameId>('ALL');

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

  const filteredRoomList = useMemo(
    () => (selectedGame === 'ALL' ? visibleRoomList : visibleRoomList.filter((r) => r.game === selectedGame)),
    [visibleRoomList, selectedGame],
  );

  useEffect(() => {
    if (searchParams.get('oauth2_error') === 'not_configured') {
      setSearchParams({}, { replace: true });
      navigate('/login?error=oauth_not_configured', { replace: true });
    }
  }, [searchParams, setSearchParams, navigate]);

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
      setIsPanelOpen(false);
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
    const { isFull } = getRoomCapacityMeta(r);
    if (isFull) {
      window.alert('이미 정원이 가득 찬 방입니다.');
      return;
    }
    setJoinRoomId(r.id);
    const { ok, message } = await joinGameRoom(r.id);
    setJoinRoomId(null);
    if (ok) {
      const chatRoomId = await getGameRoomChatRoomId(r.id);
      if (chatRoomId != null) {
        navigate(`/group-chat/room/${chatRoomId}`, { state: { fromGameRoom: true, gameRoomId: r.id } });
      }
    } else if (message) {
      window.alert(message);
    }
    fetchRooms();
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
                setIsPanelOpen(false);
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

      <div className={`main-matching-layout ${isPanelOpen ? 'matching-sidebar-visible' : ''}`}>
        <div className="home-matching-main">
          <section className="section-card home-demo-room-section" aria-labelledby="home-demo-room-heading">
            <div className="home-demo-room-header">
              <div>
                <h2 id="home-demo-room-heading" className="home-demo-room-title">팀 찾기</h2>
                <p className="home-demo-room-sub">팀 검색 조건 · 서버에 등록된 열린 방 목록</p>
              </div>
              <div className="home-demo-room-header-actions">
                <button
                  type="button"
                  className={`home-team-panel-btn ${isPanelOpen && panelType === 'match' ? 'home-team-panel-btn--open' : ''}`}
                  onClick={() => {
                    if (isPanelOpen && panelType === 'match') setIsPanelOpen(false);
                    else {
                      setPanelType('match');
                      setIsPanelOpen(true);
                    }
                  }}
                >
                  랜덤 매칭
                </button>
                <button
                  type="button"
                  className={`home-team-panel-btn ${isPanelOpen && panelType === 'create' ? 'home-team-panel-btn--open' : ''}`}
                  onClick={() => {
                    if (isPanelOpen && panelType === 'create') setIsPanelOpen(false);
                    else {
                      setPanelType('create');
                      setIsPanelOpen(true);
                    }
                  }}
                >
                  방 만들기
                </button>
              </div>
            </div>
            <nav className="home-team-game-tabs" aria-label="게임별 방 목록 필터">
              {TEAM_SEARCH_GAME_TABS.map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  className={`home-team-game-tab ${selectedGame === tab.id ? 'home-team-game-tab--active' : ''}`}
                  onClick={() => setSelectedGame(tab.id)}
                >
                  <span className="home-team-game-tab-icon">
                    <TeamSearchGameTabIcon game={tab.id} />
                  </span>
                  <span className="home-team-game-tab-label">{tab.label}</span>
                </button>
              ))}
            </nav>
            <div className="home-demo-room-table-wrap">
              <table className="home-demo-room-table">
                <thead>
                  <tr>
                    <th>제목</th>
                    <th>랭크</th>
                    <th>포지션</th>
                    <th>메모</th>
                    <th>인원</th>
                    <th>방장</th>
                    <th>등록일</th>
                    <th>참가</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingRooms && roomList.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="home-demo-room-loading-cell">방 목록 불러오는 중…</td>
                    </tr>
                  ) : visibleRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="home-demo-room-loading-cell">등록된 방이 없습니다.</td>
                    </tr>
                  ) : filteredRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="home-demo-room-loading-cell">선택한 게임에 등록된 방이 없습니다.</td>
                    </tr>
                  ) : null}
                  {filteredRoomList.map((r) => {
                    const op = parseGameOptions(r.gameOptions);
                    const { maxPlayers: maxP, isFull } = getRoomCapacityMeta(r);
                    let rankCell = '-';
                    if (r.game === 'LEAGUE_OF_LEGENDS') rankCell = rankLabel(op.mode ?? '');
                    else if (['VALORANT', 'OVERWATCH', 'PUBG'].includes(r.game)) rankCell = modeLabel(r.game, op.mode ?? '');
                    else rankCell = op.mode || '-';
                    const partyCell = partySizeLabel(op.partySize, r.game);
                    const showPos = r.game !== 'PUBG' && showPositionForRoom(r.game, op.mode) && op.position;
                    return (
                      <tr key={`api-${r.id}`} className={isFull ? 'home-demo-room-row home-demo-room-row--full' : 'home-demo-room-row'}>
                        <td>{r.title}</td>
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
                        <td className="home-demo-room-memo-cell" title={r.memo?.trim() ? r.memo : ''}>
                          {r.memo?.trim() ? r.memo : '-'}
                        </td>
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
                        <td>{formatRelativeCreatedAt(r.createdAt)}</td>
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
                              className={
                                isFull
                                  ? 'home-demo-room-join-btn home-demo-room-join-btn--full'
                                  : 'home-demo-room-join-btn home-demo-room-join-btn--live'
                              }
                              title={isFull ? '모집 완료' : user ? '참가' : '로그인 후 참가'}
                              disabled={joinRoomId === r.id || isFull}
                              onClick={() => handleApiRoomButton(r)}
                            >
                              {joinRoomId === r.id ? '참가 중…' : isFull ? '모집 완료' : '참가'}
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

        {isPanelOpen && (
          <aside className="main-sidebar section-card sidebar-card matching-sidebar-panel" aria-label="팀 찾기 패널">
            <div className="sidebar-content">
              {panelType === 'match' && randomPanelContent}
              {panelType === 'create' && createPanelContent}
            </div>
          </aside>
        )}
      </div>
    </Layout>
  );
}
