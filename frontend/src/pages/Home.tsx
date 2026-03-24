import { useEffect, useState, useCallback, useMemo } from 'react';
import type { ReactNode } from 'react';
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
import { joinMatchQueue, leaveMatchQueue, getMatchQueueStatus } from '../api/match';
import {
  TIER_OPTIONS,
  PUBG_TIER_OPTIONS,
  VALORANT_TIER_OPTIONS,
  VALORANT_MODE_OPTIONS,
  OVERWATCH_MODE_OPTIONS,
  PUBG_MODE_OPTIONS,
  PUBG_PLATFORM_OPTIONS,
  PUBG_PLATFORM_RADIO,
  PUBG_RANDOM_MAP_CHIPS,
  PUBG_PERSPECTIVE_OPTIONS,
  VALORANT_PARTY_OPTIONS,
  OVERWATCH_PARTY_OPTIONS,
  PUBG_PARTY_OPTIONS,
  CS2_PARTY_OPTIONS,
  RANDOM_MATCH_AND_ROOM_GAME_OPTIONS,
  getMatchModeOptions,
  getControlledPartyOptions,
  tierOptionsForGame,
  createFormShowTier,
  tierLabel,
  modeLabel,
  modeHasNoTier,
  showPositionForRoom,
  partySizeLabel,
  isLolAram,
  isLolSoloRank,
  positionRequiredForRandomMatch,
  getPubgRandomMatchRuleError,
  isSimpleFivePersonRandomMatch,
  isSimplePubgRandomMatch,
} from '../utils/randomMatchHelpers';
import { saveRandomMatchPending, clearRandomMatchPending } from '../utils/randomMatchPendingStorage';
import { isHiddenGameRoomHost } from '../utils/gameRoomVisibility';
import {
  LOL_ROOM_QUEUE_OPTIONS,
  LOL_LANES,
  LOL_LANE_LABELS,
  LOL_QUICK_ROLE_ORDER,
  LOL_QUICK_ROLE_LABELS,
  type LolLane,
  partySlotsFromSize,
  lolCreatePartySizeOptions,
  normalizeLolCreatePartySizeForQueue,
  validateLolRoomForm,
  buildLolRoomGameOptions,
  parseRecruitingLanes,
  lolQueueLabel,
  formatLolTierRangeCell,
  formatRecruitingSummary,
  parseQuickSeekingRq,
  formatQuickSeekingSummary,
  type LolQuickRole,
} from '../utils/lolRoomCreateHelpers';
import './Home.css';

function parseGameOptions(s?: string | null): {
  tier?: string;
  mode?: string;
  position?: string;
  preferredMap?: string;
  preferredMethod?: string;
  preferredLegend?: string;
  platform?: string;
  partySize?: string;
  hp?: string;
  hs?: string;
  rp?: string;
  tm?: string;
  tx?: string;
  ar?: string;
  rq?: string;
} {
  if (!s || !s.trim()) return {};
  try {
    const o = JSON.parse(s) as Record<string, string>;
    return {
      tier: o.tier,
      mode: o.mode,
      position: o.position,
      preferredMap: o.preferredMap,
      preferredMethod: o.preferredMethod,
      preferredLegend: o.preferredLegend,
      platform: o.platform,
      partySize: o.partySize,
      hp: o.hp,
      hs: o.hs,
      rp: o.rp,
      tm: o.tm,
      tx: o.tx,
      ar: o.ar,
      rq: o.rq,
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
  if (roomGame === 'APEX_LEGENDS') return op.preferredLegend || '-';
  return '-';
}

type SidebarTab = 'random' | 'create';

export default function Home() {
  const { user, loading: authLoading } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>('create');
  /** Random match / history / create-room panel (same toggle as former Team Searching header). */
  const [showMatchingSidebar, setShowMatchingSidebar] = useState(false);
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
  const [liveStreams, setLiveStreams] = useState<StreamItem[]>([]);
  const [loadingLive, setLoadingLive] = useState(false);
  const controlledPartyOptions = getControlledPartyOptions(matchGame, matchMode, matchTier);
  const positionRequired = positionRequiredForRandomMatch(matchGame, matchMode);
  const positionDisabled =
    isLolAram(matchGame, matchMode) || (matchGame === 'OVERWATCH' && matchMode === 'OPEN_QUEUE');
  const partySizeDisabled = isLolSoloRank(matchGame, matchMode);
  const overwatchPositionRequired = matchGame === 'OVERWATCH' && (matchMode === 'ROLE_QUEUE_COMP' || matchMode === 'QUICK_PLAY');

  const [createGame, setCreateGame] = useState('LEAGUE_OF_LEGENDS');
  const [createTitle, setCreateTitle] = useState('');
  const [createTier, setCreateTier] = useState('');
  const [createRank, setCreateRank] = useState('SOLO');
  const [createPosition, setCreatePosition] = useState<string | null>(null);
  const [createMode, setCreateMode] = useState('');
  const [createPreferredMap, setCreatePreferredMap] = useState('');
  const [createPreferredMethod, setCreatePreferredMethod] = useState('');
  const [createPreferredLegend, setCreatePreferredLegend] = useState('');
  const [createPlatform, setCreatePlatform] = useState('');
  const [createPartySize, setCreatePartySize] = useState('');
  const [createLolRecruiting, setCreateLolRecruiting] = useState<LolLane[]>([]);
  const [createLolSecondary, setCreateLolSecondary] = useState<string | null>(null);
  const [createLolQuickSeeking, setCreateLolQuickSeeking] = useState<LolQuickRole[]>([]);
  const [createTierMin, setCreateTierMin] = useState('');
  const [createApprovalRequired, setCreateApprovalRequired] = useState(false);
  const [createPassword, setCreatePassword] = useState('');
  const [createMemo, setCreateMemo] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');
  const [roomList, setRoomList] = useState<GameRoomItem[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [joinRoomId, setJoinRoomId] = useState<number | null>(null);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') {
      setCreateLolRecruiting([]);
      setCreateLolSecondary(null);
      setCreateLolQuickSeeking([]);
      setCreateTierMin('');
      setCreateApprovalRequired(false);
    }
  }, [createGame]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    setCreatePartySize((prev) => normalizeLolCreatePartySizeForQueue(createRank, prev));
  }, [createGame, createRank]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    if (createRank === 'ARAM') {
      setCreateLolRecruiting([]);
      setCreateLolSecondary(null);
      setCreatePosition(null);
      return;
    }
    if (createRank === 'QUICK') {
      setCreateLolRecruiting([]);
      return;
    }
    setCreateLolRecruiting((prev) => {
      const filtered = prev.filter((l) => l !== createPosition);
      const uniq = [...new Set(filtered)] as LolLane[];
      let next = uniq;
      if (createRank === 'SOLO') {
        if (next.length > 1) next = [next[0]];
      } else if (createRank === 'FLEX') {
        const cap = Math.max(0, partySlotsFromSize(createPartySize) - 1);
        if (next.length > cap) next = next.slice(0, cap);
      }
      if (next.length === prev.length && next.every((l, i) => l === prev[i])) return prev;
      return next;
    });
  }, [createGame, createRank, createPosition, createPartySize]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    if (createRank !== 'QUICK') setCreateLolSecondary(null);
  }, [createGame, createRank]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    if (createRank !== 'QUICK') setCreateLolQuickSeeking([]);
  }, [createGame, createRank]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS') return;
    if (createRank !== 'SOLO' && createRank !== 'FLEX') return;
    if (createPosition && !(LOL_LANES as readonly string[]).includes(createPosition)) setCreatePosition(null);
  }, [createGame, createRank, createPosition]);

  useEffect(() => {
    if (createGame !== 'LEAGUE_OF_LEGENDS' || createRank !== 'QUICK') return;
    setCreateLolQuickSeeking((prev) => {
      const cap = Math.max(0, partySlotsFromSize(createPartySize) - 1);
      const next = [...new Set(prev)].filter((r) => {
        if (r === 'FILL') return true;
        return r !== createPosition && r !== createLolSecondary;
      }) as LolQuickRole[];
      const trimmed = cap > 0 ? next.slice(0, cap) : [];
      if (trimmed.length === prev.length && trimmed.every((v, i) => v === prev[i])) return prev;
      return trimmed;
    });
  }, [createGame, createRank, createPartySize, createPosition, createLolSecondary]);

  const toggleLolQuickSeeking = (role: LolQuickRole) => {
    if (createGame !== 'LEAGUE_OF_LEGENDS' || createRank !== 'QUICK') return;
    setCreateLolQuickSeeking((prev) => {
      if (prev.includes(role)) return prev.filter((r) => r !== role);
      const cap = Math.max(0, partySlotsFromSize(createPartySize) - 1);
      if (cap === 0 || prev.length >= cap) return prev;
      if (role !== 'FILL' && (role === createPosition || role === createLolSecondary)) return prev;
      return [...prev, role];
    });
  };

  const toggleLolRecruitLane = (lane: LolLane) => {
    if (createGame !== 'LEAGUE_OF_LEGENDS' || createRank === 'ARAM' || createRank === 'QUICK') return;
    if (lane === createPosition) return;
    setCreateLolRecruiting((prev) => {
      if (prev.includes(lane)) return prev.filter((l) => l !== lane);
      if (createRank === 'SOLO') return [lane];
      if (createRank === 'FLEX') {
        const cap = Math.max(0, partySlotsFromSize(createPartySize) - 1);
        if (prev.length >= cap) return prev;
        return [...prev, lane];
      }
      return prev;
    });
  };

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

  const visibleRoomList = useMemo(
    () =>
      roomList.filter((r) => {
        if (isHiddenGameRoomHost(r.hostNickname)) return false;
        if (matchGame !== 'PUBG' || !matchPlatform) return true;
        if (r.game !== 'PUBG') return true;
        const op = parseGameOptions(r.gameOptions);
        if (op.platform && op.platform !== matchPlatform) return false;
        return true;
      }),
    [roomList, matchGame, matchPlatform],
  );

  const lolCreatePartyOptions = useMemo(
    () => (createGame === 'LEAGUE_OF_LEGENDS' ? lolCreatePartySizeOptions(createRank) : []),
    [createGame, createRank],
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

  useEffect(() => {
    if (authLoading || !user) return;
    getMatchQueueStatus().then(applyQueueStatus);
  }, [authLoading, user, applyQueueStatus]);

  useEffect(() => {
    if (authLoading || !user || !matchInQueue) return;
    const t = setInterval(() => {
      getMatchQueueStatus().then(applyQueueStatus);
    }, 1500);
    return () => clearInterval(t);
  }, [authLoading, user, matchInQueue, applyQueueStatus]);

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
    const enabledOptions = controlledPartyOptions.filter((o) => !o.disabled);
    if (enabledOptions.length === 0) {
      if (matchPartySize !== '') setMatchPartySize('');
      return;
    }
    if (!enabledOptions.some((o) => o.value === matchPartySize)) {
      // 오버워치는 규칙 위반 상태에서 안내 문구를 보여주기 위해, 현재 선택값이 disabled이면 자동 교정하지 않는다.
      const current = controlledPartyOptions.find((o) => o.value === matchPartySize);
      if (matchGame === 'OVERWATCH' && current?.disabled) return;
      if (matchGame === 'PUBG' && current?.disabled) return;
      if (matchGame === 'COUNTER_STRIKE_2' && current?.disabled) return;

      setMatchPartySize(enabledOptions[0]?.value ?? '');
    }
  }, [controlledPartyOptions, matchPartySize]);

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
      position: matchGame === 'PUBG' ? undefined : positionRequired ? (matchPosition ?? undefined) : undefined,
      targetTier:
        matchGame === 'PUBG' && matchMode === 'RANKED'
          ? (matchTier.trim() || undefined)
          : createFormShowTier(matchGame, matchMode)
            ? (matchTier.trim() || undefined)
            : undefined,
      tierPolicy: 'ANY' as const,
      pubgPlatform: matchGame === 'PUBG' ? matchPlatform : undefined,
      pubgPerspective: matchGame === 'PUBG' ? matchPerspective : undefined,
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
        platform: matchGame === 'PUBG' ? matchPlatform : undefined,
        perspective: matchGame === 'PUBG' && matchMode === 'NORMAL' ? matchPerspective : undefined,
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
    let gameOptions: string;
    if (createGame === 'LEAGUE_OF_LEGENDS') {
      if (!createPartySize.trim()) {
        setCreateError('인원을 선택해 주세요.');
        setCreating(false);
        return;
      }
      const lolErr = validateLolRoomForm({
        queue: createRank,
        hostPrimary: createPosition,
        hostSecondary: createRank === 'QUICK' ? createLolSecondary : null,
        recruiting: createLolRecruiting,
        recruitingQuick: createRank === 'QUICK' ? createLolQuickSeeking : [],
        partySize: createPartySize,
      });
      if (lolErr) {
        setCreateError(lolErr);
        setCreating(false);
        return;
      }
      gameOptions = JSON.stringify(
        buildLolRoomGameOptions({
          queue: createRank,
          partySize: createPartySize,
          hostPrimary: createPosition,
          hostSecondary: createRank === 'QUICK' ? createLolSecondary : null,
          recruiting: createLolRecruiting,
          recruitingQuick: createRank === 'QUICK' ? createLolQuickSeeking : [],
          tierMin: createTierMin,
          approvalRequired: createApprovalRequired,
        }),
      );
    } else {
      gameOptions = JSON.stringify({
        tier: createTier || undefined,
        mode: createRank || createMode || undefined,
        position: createPosition || undefined,
        preferredMap: createPreferredMap.trim() || undefined,
        preferredMethod: createPreferredMethod.trim() || undefined,
        preferredLegend: createPreferredLegend.trim() || undefined,
        platform: createPlatform || undefined,
        partySize: createPartySize || undefined,
      });
    }
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
      setCreateMode('');
      setCreatePreferredMap('');
      setCreatePreferredMethod('');
      setCreatePreferredLegend('');
      setCreatePlatform('');
      setCreatePartySize('');
      setCreateLolRecruiting([]);
      setCreateLolSecondary(null);
      setCreateLolQuickSeeking([]);
      setCreateTierMin('');
      setCreateApprovalRequired(false);
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
    try {
      const o = JSON.parse(r.gameOptions || '{}') as Record<string, string>;
      if (o.ar === '1') {
        window.alert(
          '이 방은 승인제로 표시되어 있으나, 서버에서 수락 절차는 아직 지원하지 않아 바로 입장됩니다. 호스트와 별도로 조율해 주세요.',
        );
      }
    } catch {
      /* ignore */
    }
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
            <p className="sidebar-panel-desc">
              {matchLobbyCount != null && matchTargetSize != null
                ? `매칭 중... (현재 ${matchLobbyCount} / ${matchTargetSize}명)`
                : '매칭 참여 중입니다.'}
            </p>
            <button type="button" className="sidebar-btn secondary" onClick={handleLeaveMatchQueue}>
              매칭 취소
            </button>
          </>
        ) : (
          <>
            <p className="sidebar-panel-desc">
              {matchGame === 'LEAGUE_OF_LEGENDS'
                ? '포지션을 하나 선택한 뒤 매칭을 시작하세요. 먼저 대기한 순서대로 5명이 모이면 매칭됩니다.'
                : matchGame === 'VALORANT'
                  ? '역할군을 하나 선택한 뒤 매칭을 시작하세요. 먼저 대기한 순서대로 5명이 모이면 매칭됩니다.'
                  : matchGame === 'OVERWATCH'
                    ? '포지션을 하나 선택한 뒤 매칭을 시작하세요. 역할이 겹쳐도 선착순 5명으로 바로 매칭됩니다.'
                    : matchGame === 'COUNTER_STRIKE_2'
                      ? '포지션을 하나 선택한 뒤 매칭을 시작하세요. 선착순 5명이 모이면 매칭됩니다.'
                    : matchGame === 'PUBG'
                      ? '플랫폼과 선호 맵을 선택한 뒤 매칭을 시작하세요. 같은 플랫폼에서 선착순 4명이 모이면 매칭됩니다.'
                  : '같은 조건의 유저와 매칭됩니다.'}
            </p>
            <div className="random-match-form">
              <label className="sidebar-form-label">게임</label>
              <select
                className="sidebar-form-input"
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
                  <option key={o.key} value={o.key}>{o.label}</option>
                ))}
              </select>
              {isSimpleFivePersonRandomMatch(matchGame) ? (
                <>
                  <div className="random-match-position-block">
                    <label className="sidebar-form-label">{matchGame === 'VALORANT' ? '역할군' : '포지션'}</label>
                    <PositionPicker
                      value={matchPosition}
                      onChange={setMatchPosition}
                      game={matchGame}
                      filterMode
                      includeAllOption={false}
                      className="random-match-position"
                    />
                  </div>
                  <button
                    type="button"
                    className="sidebar-btn primary"
                    onClick={handleRandomMatch}
                    disabled={matchJoining}
                    style={{ marginTop: 16 }}
                  >
                    {matchJoining ? '참가 중...' : '매칭 시작'}
                  </button>
                </>
              ) : isSimplePubgRandomMatch(matchGame) ? (
                <>
                  <label className="sidebar-form-label">플랫폼</label>
                  <div className="pubg-radio-group" role="radiogroup" aria-label="PUBG 플랫폼">
                    {PUBG_PLATFORM_RADIO.map((o) => (
                      <label key={o.value} className={`pubg-radio-pill ${matchPlatform === o.value ? 'active' : ''}`}>
                        <input
                          type="radio"
                          name="match-pubg-platform"
                          value={o.value}
                          checked={matchPlatform === o.value}
                          onChange={() => setMatchPlatform(o.value)}
                        />
                        <span>{o.label}</span>
                      </label>
                    ))}
                  </div>
                  <p className="sidebar-form-hint">스팀과 카카오는 서로 다른 매칭 큐입니다. 플랫폼을 선택해 주세요.</p>
                  <label className="sidebar-form-label">선호 맵</label>
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
                    className="sidebar-btn primary"
                    onClick={handleRandomMatch}
                    disabled={matchJoining}
                    style={{ marginTop: 16 }}
                  >
                    {matchJoining ? '참가 중...' : '매칭 시작'}
                  </button>
                </>
              ) : (
                <>
              {getMatchModeOptions(matchGame).length > 0 && (
                <>
                  <label className="sidebar-form-label">모드</label>
                  <select className="sidebar-form-input" value={matchMode} onChange={(e) => setMatchMode(e.target.value)}>
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
              {matchGame !== 'PUBG' && createFormShowTier(matchGame, matchMode) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={matchTier} onChange={(e) => setMatchTier(e.target.value)}>
                    {tierOptionsForGame(matchGame, matchMode).map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              {controlledPartyOptions.length > 0 && (
                <>
                  <label className="sidebar-form-label">인원</label>
                  <select
                    className="sidebar-form-input"
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
                disabled={matchJoining || !!valorantRuleError || !!overwatchRuleError || !!pubgRuleError}
                style={{ marginTop: 16 }}
              >
                {matchJoining ? '참가 중...' : '매칭 시작'}
              </button>

              {overwatchRuleError ? (
                <div className="sidebar-form-error" role="alert">
                  {overwatchRuleError}
                </div>
              ) : valorantRuleError ? (
                <div className="sidebar-form-error" role="alert">
                  {valorantRuleError}
                </div>
              ) : pubgRuleError ? (
                <div className="sidebar-form-error" role="alert">
                  {pubgRuleError}
                </div>
              ) : valorantRuleWarning ? (
                <div className="sidebar-form-warn">{valorantRuleWarning}</div>
              ) : null}
                </>
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
            {RANDOM_MATCH_AND_ROOM_GAME_OPTIONS.map((o) => (
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
              <label className="sidebar-form-label">큐 유형</label>
              <select className="sidebar-form-input" value={createRank} onChange={(e) => setCreateRank(e.target.value)}>
                {LOL_ROOM_QUEUE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">최소 티어</label>
              <select className="sidebar-form-input" value={createTierMin} onChange={(e) => setCreateTierMin(e.target.value)} aria-label="최소 티어">
                {TIER_OPTIONS.map((o) => (
                  <option key={`tier-${o.value || '_'}`} value={o.value}>{o.label}</option>
                ))}
              </select>
              <p className="sidebar-form-hint">비우면 티어 제한 없음으로 저장됩니다.</p>
              <label className="sidebar-form-label">인원</label>
              <select
                className="sidebar-form-input"
                value={createPartySize}
                onChange={(e) => setCreatePartySize(e.target.value)}
                disabled={createRank === 'SOLO'}
              >
                {lolCreatePartyOptions.map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              {createRank === 'SOLO' && (
                <p className="sidebar-form-hint">솔로 랭크는 2인 고정입니다.</p>
              )}
              {(createRank === 'SOLO' || createRank === 'FLEX') && (
                <>
                  <label className="sidebar-form-label">내 포지션</label>
                  <div className="lol-lane-icon-row" role="group" aria-label="내 포지션">
                    {LOL_LANES.map((lane) => (
                      <button
                        key={lane}
                        type="button"
                        className={`lol-lane-icon-btn lol-lane-icon-btn--${lane.toLowerCase()}${createPosition === lane ? ' active' : ''}`}
                        title={LOL_LANE_LABELS[lane]}
                        onClick={() => setCreatePosition(lane)}
                      >
                        <PositionIcon position={lane} game={createGame} showLabel={false} />
                      </button>
                    ))}
                  </div>
                  <label className="sidebar-form-label">구인 중인 포지션</label>
                  <div className="lol-lane-icon-row lol-lane-icon-row--recruit" role="group" aria-label="구인 포지션">
                    {LOL_LANES.map((lane) => {
                      const active = createLolRecruiting.includes(lane);
                      const blocked = lane === createPosition;
                      const cap = createRank === 'SOLO' ? 1 : Math.max(0, partySlotsFromSize(createPartySize) - 1);
                      const flexBlocked = createRank === 'FLEX' && !active && (cap === 0 || createLolRecruiting.length >= cap);
                      return (
                        <button
                          key={lane}
                          type="button"
                          className={`lol-lane-icon-btn lol-lane-icon-btn--${lane.toLowerCase()}${active ? ' active' : ''}`}
                          title={LOL_LANE_LABELS[lane]}
                          disabled={blocked || flexBlocked}
                          onClick={() => toggleLolRecruitLane(lane)}
                        >
                          <PositionIcon position={lane} game={createGame} showLabel={false} />
                        </button>
                      );
                    })}
                  </div>
                  {createRank === 'FLEX' && (
                    <p className="sidebar-form-hint">
                      자유 랭크는 최대 5인까지. 구인 슬롯은 최대 {Math.max(0, partySlotsFromSize(createPartySize) - 1)}개, 내 포지션과 중복 불가.
                    </p>
                  )}
                  {createRank === 'SOLO' && (
                    <p className="sidebar-form-hint">구인 포지션은 1개만, 내 포지션과 달라야 합니다.</p>
                  )}
                </>
              )}
              {createRank === 'QUICK' && (
                <>
                  <label className="sidebar-form-label">주 역할군</label>
                  <div className="lol-lane-icon-row" role="group" aria-label="주 역할군">
                    {LOL_QUICK_ROLE_ORDER.map((role) => (
                      <button
                        key={role}
                        type="button"
                        className={`lol-lane-icon-btn lol-lane-icon-btn--${role === 'ADC' ? 'adc' : role.toLowerCase()}${createPosition === role ? ' active' : ''}`}
                        title={LOL_QUICK_ROLE_LABELS[role]}
                        onClick={() => setCreatePosition(role)}
                      >
                        <PositionIcon position={role} game={createGame} showLabel={false} />
                      </button>
                    ))}
                  </div>
                  <label className="sidebar-form-label">부 역할군</label>
                  <div className="lol-lane-icon-row" role="group" aria-label="부 역할군">
                    {LOL_QUICK_ROLE_ORDER.map((role) => (
                      <button
                        key={`sec-${role}`}
                        type="button"
                        className={`lol-lane-icon-btn lol-lane-icon-btn--${role === 'ADC' ? 'adc' : role.toLowerCase()}${createLolSecondary === role ? ' active' : ''}`}
                        title={LOL_QUICK_ROLE_LABELS[role]}
                        onClick={() => setCreateLolSecondary(role)}
                      >
                        <PositionIcon position={role} game={createGame} showLabel={false} />
                      </button>
                    ))}
                  </div>
                  <p className="sidebar-form-hint">주·부 역할은 서로 달라야 합니다.</p>
                  <label className="sidebar-form-label">찾는 포지션</label>
                  <div className="lol-lane-icon-row lol-lane-icon-row--recruit" role="group" aria-label="찾는 포지션">
                    {LOL_QUICK_ROLE_ORDER.map((role) => {
                      const active = createLolQuickSeeking.includes(role);
                      const cap = Math.max(0, partySlotsFromSize(createPartySize) - 1);
                      const conflicts =
                        role !== 'FILL' && (role === createPosition || role === createLolSecondary);
                      const seekBlocked = !active && (cap === 0 || createLolQuickSeeking.length >= cap || conflicts);
                      return (
                        <button
                          key={`seek-${role}`}
                          type="button"
                          className={`lol-lane-icon-btn lol-lane-icon-btn--${role === 'ADC' ? 'adc' : role.toLowerCase()}${active ? ' active' : ''}`}
                          title={LOL_QUICK_ROLE_LABELS[role]}
                          disabled={seekBlocked}
                          onClick={() => toggleLolQuickSeeking(role)}
                        >
                          <PositionIcon position={role} game={createGame} showLabel={false} />
                        </button>
                      );
                    })}
                  </div>
                  <p className="sidebar-form-hint">
                    팀에 필요한 역할을 고릅니다. 최대 {Math.max(0, partySlotsFromSize(createPartySize) - 1)}개, 주·부와 같은 라인(채우기 제외)은 선택할 수 없습니다.
                  </p>
                </>
              )}
              <label className="sidebar-form-label lol-approval-label">
                <input
                  type="checkbox"
                  checked={createApprovalRequired}
                  onChange={(e) => setCreateApprovalRequired(e.target.checked)}
                />
                {' '}승인 후 입장(표시만 — 참가는 즉시 처리)
              </label>
              <p className="sidebar-form-hint">승인제는 방 정보에만 저장되며, 서버 수락 절차는 추후 연동 예정입니다.</p>
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
              <label className="sidebar-form-label">인원</label>
              <select className="sidebar-form-input" value={createPartySize} onChange={(e) => setCreatePartySize(e.target.value)}>
                {VALORANT_PARTY_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
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
              <label className="sidebar-form-label">인원</label>
              <select className="sidebar-form-input" value={createPartySize} onChange={(e) => setCreatePartySize(e.target.value)}>
                {OVERWATCH_PARTY_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">역할</label>
              <PositionPicker value={createPosition} onChange={setCreatePosition} game={createGame} />
            </>
          )}

          {createGame === 'PUBG' && (
            <>
              <label className="sidebar-form-label">파티</label>
              <PositionPicker
                value={createPartySize || 'ALL'}
                onChange={(v) => setCreatePartySize(v === 'ALL' ? '' : (v ?? ''))}
                game={createGame}
              />
              <label className="sidebar-form-label">인원</label>
              <select className="sidebar-form-input" value={createPartySize} onChange={(e) => setCreatePartySize(e.target.value)}>
                {PUBG_PARTY_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">모드</label>
              <select className="sidebar-form-input" value={createMode} onChange={(e) => setCreateMode(e.target.value)}>
                {PUBG_MODE_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              {createFormShowTier('PUBG', createMode) && (
                <>
                  <label className="sidebar-form-label">티어</label>
                  <select className="sidebar-form-input" value={createTier} onChange={(e) => setCreateTier(e.target.value)}>
                    {PUBG_TIER_OPTIONS.map((o) => (
                      <option key={o.value || '_'} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </>
              )}
              <label className="sidebar-form-label">플랫폼</label>
              <select className="sidebar-form-input" value={createPlatform} onChange={(e) => setCreatePlatform(e.target.value)}>
                {PUBG_PLATFORM_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">선호 맵(경쟁전 제외 선택)</label>
              <input type="text" className="sidebar-form-input" value={createPreferredMap} onChange={(e) => setCreatePreferredMap(e.target.value)} placeholder="선호 맵(선택)" maxLength={100} />
            </>
          )}

          {createGame === 'COUNTER_STRIKE_2' && (
            <>
              <label className="sidebar-form-label">인원</label>
              <select className="sidebar-form-input" value={createPartySize} onChange={(e) => setCreatePartySize(e.target.value)}>
                {CS2_PARTY_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">역할</label>
              <PositionPicker value={createPosition} onChange={setCreatePosition} game={createGame} />
              <label className="sidebar-form-label">티어</label>
              <select className="sidebar-form-input" value={createTier} onChange={(e) => setCreateTier(e.target.value)}>
                {TIER_OPTIONS.map((o) => (
                  <option key={o.value || '_'} value={o.value}>{o.label}</option>
                ))}
              </select>
              <label className="sidebar-form-label">선호법</label>
              <input type="text" className="sidebar-form-input" value={createPreferredMethod} onChange={(e) => setCreatePreferredMethod(e.target.value)} placeholder="예: Dust2, Mirage" maxLength={100} />
            </>
          )}

          <label className="sidebar-form-label">글 삭제용 비밀번호</label>
          <input type="password" className="sidebar-form-input" value={createPassword} onChange={(e) => setCreatePassword(e.target.value)} placeholder="삭제 시 입력할 비밀번호" />

          <label className="sidebar-form-label">{createGame === 'PUBG' ? '방에 대한 메모' : '짧은 메모'}</label>
          <textarea
            className="sidebar-form-input sidebar-form-textarea"
            value={createMemo}
            onChange={(e) => setCreateMemo(e.target.value)}
            placeholder="찾는 조건이나 하고 싶은 말을 적어주세요"
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
                setCreateLolRecruiting([]);
                setCreateLolSecondary(null);
                setCreateLolQuickSeeking([]);
                setCreateTierMin('');
                setCreateApprovalRequired(false);
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
                  <span className="live-badge">LIVE</span>
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
          <section className="section-card home-demo-room-section" aria-labelledby="home-demo-room-heading">
            <div className="home-demo-room-header">
              <div>
                <h2 id="home-demo-room-heading" className="home-demo-room-title">Team Searching</h2>
                <p className="home-demo-room-sub">팀 검색 조건 · 서버에 등록된 열린 방 목록</p>
              </div>
              <div className="home-demo-room-actions">
                <button
                  type="button"
                  className={`team-search-quick-btn ${showMatchingSidebar && sidebarTab === 'random' ? 'active' : ''}`}
                  onClick={() => {
                    if (showMatchingSidebar && sidebarTab === 'random') setShowMatchingSidebar(false);
                    else {
                      setSidebarTab('random');
                      setShowMatchingSidebar(true);
                    }
                  }}
                >
                  랜덤 매칭
                </button>
                <button
                  type="button"
                  className={`team-search-quick-btn ${showMatchingSidebar && sidebarTab === 'create' ? 'active' : ''}`}
                  onClick={() => {
                    if (showMatchingSidebar && sidebarTab === 'create') setShowMatchingSidebar(false);
                    else {
                      setSidebarTab('create');
                      setShowMatchingSidebar(true);
                    }
                  }}
                >
                  방 만들기
                </button>
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
                    const recruitingLanes = parseRecruitingLanes(op.rp);
                    const quickSeeking = op.mode === 'QUICK' ? parseQuickSeekingRq(op.rq) : [];
                    const hostPrimary = op.hp || op.position;
                    const tierCell =
                      r.game === 'LEAGUE_OF_LEGENDS'
                        ? op.tm || op.tx || op.tier
                          ? formatLolTierRangeCell(op.tm, op.tx, op.tier)
                          : '-'
                        : modeHasNoTier(r.game, op.mode)
                          ? '-'
                          : op.tier
                            ? tierLabel(
                                op.tier,
                                r.game === 'PUBG' || r.game === 'VALORANT' || r.game === 'COUNTER_STRIKE_2' ? r.game : undefined,
                                r.game === 'COUNTER_STRIKE_2' ? op.mode : undefined,
                              )
                            : '-';
                    let rankCell = '-';
                    if (r.game === 'LEAGUE_OF_LEGENDS') rankCell = lolQueueLabel(op.mode ?? '');
                    else if (['VALORANT', 'OVERWATCH', 'APEX_LEGENDS', 'PUBG', 'COUNTER_STRIKE_2'].includes(r.game)) rankCell = modeLabel(r.game, op.mode ?? '');
                    else rankCell = op.mode || '-';
                    let noteCell = extraColumnValue(op, r.game);
                    if (r.game === 'PUBG' && op.preferredMap) {
                      noteCell = noteCell !== '-' ? `${noteCell} · ${op.preferredMap}` : op.preferredMap;
                    }
                    if (op.ar === '1') {
                      noteCell = noteCell !== '-' ? `${noteCell} · 승인제` : '승인제';
                    }
                    const partyCell = partySizeLabel(op.partySize, r.game);
                    let positionTd: ReactNode;
                    if (r.game === 'LEAGUE_OF_LEGENDS') {
                      if (isLolAram(r.game, op.mode ?? '')) {
                        positionTd = '-';
                      } else if (op.mode === 'QUICK') {
                        positionTd = (
                          <div className="home-demo-lol-position-cell">
                            {hostPrimary ? (
                              <span className="home-demo-lol-role-pair">
                                <span className="home-demo-lol-role-tag">주</span>
                                <span className="home-demo-room-position">
                                  <PositionIcon position={hostPrimary} game={r.game} showLabel />
                                </span>
                              </span>
                            ) : (
                              '-'
                            )}
                            {op.hs ? (
                              <span className="home-demo-lol-role-pair">
                                <span className="home-demo-lol-role-tag">부</span>
                                <span className="home-demo-room-position">
                                  <PositionIcon position={op.hs} game={r.game} showLabel />
                                </span>
                              </span>
                            ) : null}
                            {quickSeeking.length > 0 ? (
                              <span className="home-demo-lol-recruit">찾는 {formatQuickSeekingSummary(quickSeeking)}</span>
                            ) : null}
                          </div>
                        );
                      } else {
                        positionTd = (
                          <div className="home-demo-lol-position-cell">
                            {hostPrimary ? (
                              <span className="home-demo-room-position">
                                <PositionIcon position={hostPrimary} game={r.game} showLabel />
                              </span>
                            ) : (
                              '-'
                            )}
                            {op.hs && (LOL_LANES as readonly string[]).includes(op.hs) ? (
                              <span className="home-demo-lol-sec">부 {LOL_LANE_LABELS[op.hs as LolLane]}</span>
                            ) : null}
                            {recruitingLanes.length > 0 ? (
                              <span className="home-demo-lol-recruit">구인 {formatRecruitingSummary(recruitingLanes)}</span>
                            ) : null}
                          </div>
                        );
                      }
                    } else if (r.game !== 'PUBG' && showPositionForRoom(r.game, op.mode) && op.position) {
                      positionTd = (
                        <span className="home-demo-room-position">
                          <PositionIcon position={op.position} game={r.game} showLabel />
                        </span>
                      );
                    } else {
                      positionTd = '-';
                    }
                    return (
                      <tr key={`api-${r.id}`}>
                        <td>{r.title}</td>
                        <td>{tierCell}</td>
                        <td>{rankCell}</td>
                        <td>{positionTd}</td>
                        <td>{noteCell}</td>
                        <td>{partyCell}</td>
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
              <button type="button" className={sidebarTab === 'create' ? 'active' : ''} onClick={() => setSidebarTab('create')}>방 만들기</button>
            </div>
            <div className="sidebar-content">
              {sidebarTab === 'random' && randomPanelContent}
              {sidebarTab === 'create' && createPanelContent}
            </div>
          </aside>
        )}
      </div>
    </Layout>
  );
}

