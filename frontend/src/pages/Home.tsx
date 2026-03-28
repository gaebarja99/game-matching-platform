import { useEffect, useState, useCallback, useMemo } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import LiveThumb from '../components/LiveThumb';
import PositionPicker from '../components/PositionPicker';
import PositionIcon from '../components/PositionIcon';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { resolveProfileImageUrl } from '../api/client';
import {
  createGameRoom,
  fetchGameRoomList,
  deleteGameRoom,
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
  getMatchSession,
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
  tierOptionsForGame,
  createFormShowTier,
  rankLabel,
  modeLabel,
  showPositionForRoom,
  partySizeLabel,
  isLolAram,
  positionRequiredForRandomMatch,
} from '../utils/randomMatchHelpers';
import { saveRandomMatchPending, clearRandomMatchPending } from '../utils/randomMatchPendingStorage';
import { isHiddenGameRoomHost } from '../utils/gameRoomVisibility';
import { getRoomCapacityMeta } from '../utils/gameRoomCapacity';
import './Home.css';

function defaultRandomMatchPosition(game: string): string {
  switch (game) {
    case 'VALORANT':
      return 'DUELIST';
    case 'OVERWATCH':
      return 'TANK';
    case 'COUNTER_STRIKE_2':
      return 'ENTRY';
    default:
      return 'TOP';
  }
}

function isRandomMatchPositionValid(game: string, pos: string | null): boolean {
  if (pos == null || pos === '') return false;
  const p = pos.toUpperCase();
  switch (game) {
    case 'LEAGUE_OF_LEGENDS':
      return ['TOP', 'JUNGLE', 'MID', 'MIDDLE', 'ADC', 'BOTTOM', 'SUPPORT', 'SUP', 'UTILITY'].includes(
        p,
      );
    case 'VALORANT':
      return [
        'DUELIST',
        'SCOUT',
        'STRATEGIST',
        'WATCHER',
        'INITIATOR',
        'CONTROLLER',
        'SENTINEL',
      ].includes(p);
    case 'OVERWATCH':
      return ['TANK', 'DAMAGE', 'SUPPORT', 'DPS', 'HEALER'].includes(p);
    case 'COUNTER_STRIKE_2':
      return ['ENTRY', 'SUPPORT', 'IGL', 'AWPER', 'LURKER', 'ALL'].includes(p);
    default:
      return true;
  }
}

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

type HomeMatchRoomItem = {
  id: number;
  game: string;
  createdAt: string;
  memberCount: number | null;
  hostNickname: string;
};

function UserGlyph({ className = '' }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={14}
      height={14}
      className={className}
      aria-hidden
      focusable="false"
    >
      <path
        fill="currentColor"
        d="M12 12.4a4.2 4.2 0 1 0-4.2-4.2 4.2 4.2 0 0 0 4.2 4.2Zm0 2.1c-3.5 0-6.7 1.8-8.1 4.7a1 1 0 0 0 .9 1.5h14.4a1 1 0 0 0 .9-1.5c-1.4-2.9-4.6-4.7-8.1-4.7Z"
      />
    </svg>
  );
}

function PubgPartySizeButtons({
  value,
  onChange,
  ariaLabel,
}: {
  value: string;
  onChange: (next: 'DUO' | 'SQUAD') => void;
  ariaLabel: string;
}) {
  const isDuo = value === 'DUO' || value === '';
  const isSquad = value === 'SQUAD';
  return (
    <div className="pubg-party-size" role="radiogroup" aria-label={ariaLabel}>
      <button
        type="button"
        className={`pubg-party-size-btn ${isDuo ? 'active' : ''}`}
        onClick={() => onChange('DUO')}
        role="radio"
        aria-checked={isDuo}
        aria-label="듀오 2인"
        title="듀오 2인"
      >
        <span className="pubg-party-size-icons" aria-hidden>
          <UserGlyph />
          <UserGlyph />
        </span>
      </button>
      <button
        type="button"
        className={`pubg-party-size-btn ${isSquad ? 'active' : ''}`}
        onClick={() => onChange('SQUAD')}
        role="radio"
        aria-checked={isSquad}
        aria-label="스쿼드 4인"
        title="스쿼드 4인"
      >
        <span className="pubg-party-size-icons" aria-hidden>
          <UserGlyph />
          <UserGlyph />
          <UserGlyph />
          <UserGlyph />
        </span>
      </button>
    </div>
  );
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

const TEAM_SEARCH_GAME_TABS: Array<{
  id: TeamSearchGameId;
  label: string;
  /** simple-icons CDN slug (https://cdn.simpleicons.org/) */
  simpleIconSlug?: string;
}> = [
  { id: 'ALL', label: '전체' },
  { id: 'LEAGUE_OF_LEGENDS', label: '리그오브레전드', simpleIconSlug: 'leagueoflegends' },
  { id: 'VALORANT', label: '발로란트', simpleIconSlug: 'valorant' },
  // Overwatch는 simple-icons에 없거나 slug가 달라 CDN에서 깨질 수 있어 인라인 SVG 사용
  { id: 'OVERWATCH', label: '오버워치2' },
  { id: 'PUBG', label: 'PUBG', simpleIconSlug: 'pubg' },
  { id: 'COUNTER_STRIKE_2', label: 'CS2', simpleIconSlug: 'counterstrike' },
];

function TeamSearchGameTabIcon({
  gameId,
  slug,
  active,
  theme,
}: {
  gameId: TeamSearchGameId;
  slug?: string;
  active: boolean;
  theme: 'dark' | 'light';
}) {
  // simple-icons CDN: /{slug}/{color}
  // - 기본: 다크=연회색, 라이트=진회색
  // - 활성: 연두색(2ECC71)
  const baseColor = theme === 'dark' ? '9CA3AF' : '111827';
  const activeColor = '2ECC71';
  const color = active ? activeColor : baseColor;

  if (gameId === 'OVERWATCH') {
    return (
      <svg width={18} height={18} viewBox="0 0 24 24" aria-hidden focusable="false">
        <path
          fill="currentColor"
          d="M12 3.5a8.5 8.5 0 1 0 8.5 8.5A8.51 8.51 0 0 0 12 3.5Zm0 2a6.5 6.5 0 0 1 6.16 4.4l-2.47-1.43a1 1 0 0 0-1 1.73l2.9 1.68a6.47 6.47 0 0 1-1.78 4.22l-2.23-3.86a1 1 0 0 0-1.73 1l2.24 3.88A6.5 6.5 0 0 1 5.5 12 6.5 6.5 0 0 1 12 5.5Z"
          opacity={active ? 1 : 0.92}
        />
        <path
          fill="currentColor"
          d="M8.2 8.9 6.7 11.5a1 1 0 0 0 .37 1.36 1 1 0 0 0 1.36-.37l.9-1.56 1.7 1a1 1 0 0 0 1-1.73L9.2 8.7a1 1 0 0 0-1 .2Z"
          opacity={active ? 0.95 : 0.65}
        />
      </svg>
    );
  }

  if (!slug) {
    return (
      <svg width={18} height={18} viewBox="0 0 24 24" aria-hidden focusable="false">
        <circle cx="12" cy="12" r="9.5" fill="currentColor" opacity="0.18" />
        <path d="M6.8 12h10.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <path d="M12 6.8v10.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" opacity="0.85" />
      </svg>
    );
  }

  return (
    <img
      className="home-team-game-tab-img"
      src={`https://cdn.simpleicons.org/${slug}/${color}`}
      alt=""
      width={18}
      height={18}
      loading="lazy"
      decoding="async"
      referrerPolicy="no-referrer"
    />
  );
}

export default function Home() {
  const { user, loading: authLoading } = useAuth();
  const { theme } = useTheme();
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
  const positionRequired = positionRequiredForRandomMatch(matchGame, matchMode);
  const positionDisabled = isLolAram(matchGame, matchMode);

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
  const [matchRoomList, setMatchRoomList] = useState<HomeMatchRoomItem[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [joinRoomId, setJoinRoomId] = useState<number | null>(null);
  const [selectedGame, setSelectedGame] = useState<TeamSearchGameId>('ALL');

  // 팀 찾기 테이블 컬럼 표시 규칙
  // - PUBG 탭: 포지션 컬럼 숨김
  // - 전체(ALL) 탭: 삭제 컬럼 숨김
  const showPositionColumn = selectedGame !== 'PUBG';
  const showDeleteColumn = selectedGame !== 'ALL';
  const tableColumnCount = 7 + (showPositionColumn ? 1 : 0) + (showDeleteColumn ? 1 : 0);

  const fetchRooms = useCallback(() => {
    setLoadingRooms(true);
    fetchGameRoomList(undefined, false)
      .then(setRoomList)
      .finally(() => setLoadingRooms(false));
  }, []);

  const fetchMatchRooms = useCallback(async () => {
    if (!user) {
      setMatchRoomList([]);
      return;
    }

    const sessions = await getMyMatchSessions();
    const detailedSessions = await Promise.all(
      sessions.map(async (session) => {
        const detail = await getMatchSession(session.id);
        return {
          id: session.id,
          game: session.game,
          createdAt: session.createdAt,
          memberCount: detail?.members?.length ?? null,
          hostNickname: detail?.members?.[0]?.nickname?.trim() || '랜덤 매칭',
        };
      }),
    );

    detailedSessions.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    setMatchRoomList(detailedSessions);
  }, [user]);

  useEffect(() => {
    if (authLoading) return;
    fetchRooms();
  }, [authLoading, fetchRooms]);

  useEffect(() => {
    if (authLoading) return;
    void fetchMatchRooms();
  }, [authLoading, fetchMatchRooms]);

  useEffect(() => {
    if (authLoading) return;
    // 방 목록 인원 현황을 자연스럽게 갱신 (예: 1/5 → 2/5)
    const timer = window.setInterval(() => {
      fetchRooms();
      void fetchMatchRooms();
    }, 4000);
    return () => window.clearInterval(timer);
  }, [authLoading, fetchRooms, fetchMatchRooms]);

  const visibleRoomList = useMemo(
    () => roomList.filter((r) => !isHiddenGameRoomHost(r.hostNickname)),
    [roomList],
  );

  const filteredRoomList = useMemo(
    () => (selectedGame === 'ALL' ? visibleRoomList : visibleRoomList.filter((r) => r.game === selectedGame)),
    [visibleRoomList, selectedGame],
  );

  const filteredMatchRoomList = useMemo(
    () => (selectedGame === 'ALL' ? matchRoomList : matchRoomList.filter((room) => room.game === selectedGame)),
    [matchRoomList, selectedGame],
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
    if (matchGame !== 'PUBG') {
      if (matchPartySize !== '') setMatchPartySize('');
      return;
    }
    if (matchPartySize !== 'DUO' && matchPartySize !== 'SQUAD') {
      setMatchPartySize('DUO');
    }
  }, [matchGame, matchPartySize]);

  // matchPosition을 matchGame/positionRequired에만 맞춤 (의존에 matchPosition 넣지 않음 → 불필요한 루프 방지)
  useEffect(() => {
    if (!positionRequired) {
      setMatchPosition((p) => (p == null ? p : null));
      return;
    }
    setMatchPosition((prev) =>
      prev != null && isRandomMatchPositionValid(matchGame, prev)
        ? prev
        : defaultRandomMatchPosition(matchGame),
    );
  }, [matchGame, positionRequired]);

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

  const goMatchRoomChat = (sessionId: number) => {
    navigate(`/match-chat/${sessionId}`);
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

  const [deletingRoomId, setDeletingRoomId] = useState<number | null>(null);
  const handleDeleteRoom = async (r: GameRoomItem) => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (!r.isHost) {
      window.alert('방장만 삭제할 수 있습니다.');
      return;
    }
    const pw = window.prompt('방 삭제용 비밀번호를 입력해 주세요.');
    if (pw == null) return;
    const deletePassword = pw.trim();
    if (!deletePassword) return;
    if (!window.confirm('정말 이 방을 삭제할까요?')) return;

    setDeletingRoomId(r.id);
    try {
      const { ok, message } = await deleteGameRoom(r.id, deletePassword);
      if (!ok) {
        window.alert(message || '삭제에 실패했습니다.');
        return;
      }
      setRoomList((prev) => prev.filter((x) => x.id !== r.id));
    } catch {
      window.alert('삭제 중 오류가 발생했습니다.');
    } finally {
      setDeletingRoomId(null);
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
              {matchGame === 'PUBG' && (
                <>
                  <label className="sidebar-form-label">인원</label>
                  <PubgPartySizeButtons
                    value={matchPartySize}
                    onChange={setMatchPartySize}
                    ariaLabel="PUBG 인원 선택"
                  />
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
          <select
            className="sidebar-form-input"
            value={createGame}
            onChange={(e) => {
              setCreateGame(e.target.value);
              setCreateMode('');
              setCreateTier('');
              setCreatePosition(null);
              setCreateMyPosition(null);
              setCreatePartnerPosition(null);
              setCreatePrimaryRole(null);
              setCreateSecondaryRole(null);
              setCreateFindPosition(null);
              setCreateError('');
            }}
          >
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
              <PubgPartySizeButtons
                value={createPartySize}
                onChange={setCreatePartySize}
                ariaLabel="PUBG 파티 인원 선택"
              />

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
                    <div className="live-card-name">{s.title || '방송 중'}</div>
                    <div className="live-card-game">{s.broadcasterNickname || '?'}</div>
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
                    <TeamSearchGameTabIcon
                      gameId={tab.id}
                      slug={tab.simpleIconSlug}
                      active={selectedGame === tab.id}
                      theme={theme}
                    />
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
                    {showPositionColumn ? <th>포지션</th> : null}
                    <th>메모</th>
                    <th>인원</th>
                    <th>방장</th>
                    <th>등록일</th>
                    <th>참가</th>
                    {showDeleteColumn ? <th>삭제</th> : null}
                  </tr>
                </thead>
                <tbody>
                  {loadingRooms && roomList.length === 0 && matchRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={tableColumnCount} className="home-demo-room-loading-cell">방 목록 불러오는 중…</td>
                    </tr>
                  ) : visibleRoomList.length === 0 && matchRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={tableColumnCount} className="home-demo-room-loading-cell">등록된 방이 없습니다.</td>
                    </tr>
                  ) : filteredRoomList.length === 0 && filteredMatchRoomList.length === 0 ? (
                    <tr>
                      <td colSpan={tableColumnCount} className="home-demo-room-loading-cell">선택한 게임에 등록된 방이 없습니다.</td>
                    </tr>
                  ) : null}
                  {filteredMatchRoomList.map((room) => {
                    const gameLabel = GAME_OPTIONS.find((option) => option.id === room.game)?.label ?? room.game;
                    return (
                      <tr key={`match-${room.id}`} className="home-demo-room-row">
                        <td>[랜덤] {gameLabel}</td>
                        <td>랜덤 매칭</td>
                        {showPositionColumn ? <td>-</td> : null}
                        <td className="home-demo-room-memo-cell" title="내 랜덤 매칭방">내 랜덤 매칭방</td>
                        <td>{room.memberCount != null ? `${room.memberCount}명` : '-'}</td>
                        <td>{room.hostNickname}</td>
                        <td>{formatRelativeCreatedAt(room.createdAt)}</td>
                        <td>
                          <button
                            type="button"
                            className="home-demo-room-join-btn home-demo-room-join-btn--live"
                            title="랜덤 매칭 채팅방으로 이동"
                            onClick={() => goMatchRoomChat(room.id)}
                          >
                            입장
                          </button>
                        </td>
                        {showDeleteColumn ? (
                          <td>
                            <span className="home-demo-room-closed-label">-</span>
                          </td>
                        ) : null}
                      </tr>
                    );
                  })}
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
                        {showPositionColumn ? (
                          <td>
                            {showPos ? (
                              <span className="home-demo-room-position">
                                <PositionIcon position={op.position} game={r.game} showLabel />
                              </span>
                            ) : (
                              '-'
                            )}
                          </td>
                        ) : null}
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
                        {showDeleteColumn ? (
                          <td>
                            {r.isHost ? (
                              <button
                                type="button"
                                className="home-demo-room-delete-btn"
                                disabled={deletingRoomId === r.id}
                                onClick={() => void handleDeleteRoom(r)}
                                title="방 삭제"
                              >
                                {deletingRoomId === r.id ? '삭제 중…' : '삭제'}
                              </button>
                            ) : (
                              <span className="home-demo-room-closed-label">-</span>
                            )}
                          </td>
                        ) : null}
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
