import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import Hls from 'hls.js';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import WatchLayout from '../components/WatchLayout';
import { useAuth } from '../contexts/AuthContext';
import { apiUrl, getWsUrl, resolveProfileImageUrl } from '../api/client';

declare global {
  interface Window {
    IMP?: {
      init: (storeId: string) => void;
      request_pay: (
        params: { pg: string; pay_method: string; merchant_uid: string; amount: number; name: string; buyer_name?: string; m_redirect_url?: string },
        callback: (r: { success?: boolean; imp_uid?: string; merchant_uid?: string; error_msg?: string }) => void
      ) => void;
    };
  }
}

interface StreamInfo {
  id: number;
  title?: string;
  game?: string;
  status?: string;
  viewerCount?: number;
  userId?: number;
  broadcasterNickname?: string;
  broadcasterProfileImageUrl?: string;
  followerCount?: number;
  endedAt?: string | null;
  playbackUrl?: string | null;
  externalUrl?: string | null;
  startedAt?: string | null;
  /** 운영자가 지정한 파트너 스트리머일 때만 true */
  partner?: boolean;
}

interface WeeklyDonor {
  rank?: number;
  userId?: number;
  displayName?: string;
  totalPang?: number;
  consecutiveDonationDays?: number;
}

/** 팡 금액별 채팅 카드 tier 클래스: 1000=초록, 10000=보라, 100000=핑크 */
function donationTierClass(amount: number | undefined): string {
  if (amount == null) return 'tier-pang';
  if (amount >= 100_000) return 'tier-mega';
  if (amount >= 10_000) return 'tier-super';
  return 'tier-pang';
}

/** 레벨 구간별 뱃지 색상 클래스 (1~999, 1000~1999, ... 9000~9999) */
function levelBadgeClass(level: number | undefined): string {
  if (level == null || level < 1) return 'level-1-999';
  const tier = Math.min(9, Math.floor(level / 1000));
  return ['level-1-999', 'level-1000-1999', 'level-2000-2999', 'level-3000-3999', 'level-4000-4999', 'level-5000-5999', 'level-6000-6999', 'level-7000-7999', 'level-8000-8999', 'level-9000-9999'][tier];
}

type ChatMessageItem =
  | { type: 'chat'; displayName?: string; text?: string; streamer?: boolean; profileImageUrl?: string; userId?: number; level?: number }
  | { type: 'donation'; donorName?: string; amount?: number; tier?: string; donorMessage?: string; donorProfileImageUrl?: string; consecutiveDonationDays?: number; donorUserId?: number }
  | { type: 'system'; text?: string };

function formatDuration(startedAt: string | null | undefined): string {
  if (!startedAt) return '';
  const start = new Date(startedAt).getTime();
  const now = Date.now();
  const sec = Math.floor((now - start) / 1000);
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  return (
    (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s + ' 스트리밍 중'
  );
}

function toEmbedVideoUrl(url: string): string | null {
  if (!url) return null;
  try {
    const u = new URL(url);
    const host = (u.hostname || '').toLowerCase();
    if (host.includes('twitch.tv')) {
      const path = (u.pathname || '').replace(/^\/+|\/+$/g, '').split('/')[0];
      if (path)
        return `https://player.twitch.tv/?channel=${encodeURIComponent(path)}&parent=${encodeURIComponent(window.location.hostname || 'localhost')}`;
    }
    if (host.includes('youtube.com') || host === 'youtu.be') {
      const v = u.searchParams.get('v') || (u.pathname || '').split('/').filter(Boolean).pop();
      if (v) return `https://www.youtube.com/embed/${v}?autoplay=1&mute=1`;
    }
  } catch {
    /* ignore */
  }
  return null;
}

export default function Watch() {
  const { streamId } = useParams<{ streamId: string }>();
  const [stream, setStream] = useState<StreamInfo | null>(null);
  const [weeklyRank, setWeeklyRank] = useState<WeeklyDonor[]>([]);
  const [weeklyTotal, setWeeklyTotal] = useState<number | null>(null);
  const [chatInput, setChatInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [isFollowing, setIsFollowing] = useState(true);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [playerLoading, setPlayerLoading] = useState(false);
  const [playerError, setPlayerError] = useState<string | null>(null);
  const [streamDuration, setStreamDuration] = useState('');
  const [isMuted, setIsMuted] = useState(true);
  const [isPaused, setIsPaused] = useState(false);
  const [streamerAvatarError, setStreamerAvatarError] = useState(false);
  const [chatMessages, setChatMessages] = useState<ChatMessageItem[]>([]);
  const [chatConnected, setChatConnected] = useState(false);
  const [chatNotice, setChatNotice] = useState('');
  const [donationModalOpen, setDonationModalOpen] = useState(false);
  const [donationAmount, setDonationAmount] = useState('');
  const [donationMessage, setDonationMessage] = useState('');
  const [donationSubmitting, setDonationSubmitting] = useState(false);
  const [donationError, setDonationError] = useState('');
  const [subscriptionModalOpen, setSubscriptionModalOpen] = useState(false);
  const [subscriptionAgree, setSubscriptionAgree] = useState(false);
  const [subscriptionSubmitting, setSubscriptionSubmitting] = useState(false);
  const [subscriptionError, setSubscriptionError] = useState('');
  const [controlsVisible, setControlsVisible] = useState(false);
  const [chatPanelOpen, setChatPanelOpen] = useState(true);
  /** 주간 후원 랭킹 표시: collapsed(접힘) | expanded(펼침) | full(전체+접기) */
  const [weeklyRankView, setWeeklyRankView] = useState<'collapsed' | 'expanded' | 'full'>('collapsed');
  const donorUserIdsRef = useRef<Set<number>>(new Set());
  const videoRef = useRef<HTMLVideoElement>(null);
  const playerWrapRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const durationIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const joinedStreamIdRef = useRef<number | null>(null);
  const { user } = useAuth();

  useEffect(() => {
    if (!streamId) {
      setLoading(false);
      return;
    }
    const id = Number(streamId);
    if (Number.isNaN(id)) {
      setLoading(false);
      return;
    }
    Promise.all([
      fetch(apiUrl(`api/streams/${id}`), { credentials: 'include' }).then((r) => (r.ok ? r.json() : null)),
      fetch(apiUrl(`api/streams/${id}/weekly-donor-rank`), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
      fetch(apiUrl(`api/streams/${id}/donors`), { credentials: 'include' }).then((r) => (r.ok ? r.json() : [])),
    ])
      .then(([s, rankList, donorIds]: [StreamInfo | null, WeeklyDonor[], number[]]) => {
        if (Array.isArray(donorIds)) donorIds.forEach((uid) => donorUserIdsRef.current.add(uid));
        setStream(s ?? null);
        const list = Array.isArray(rankList) ? rankList : [];
        setWeeklyRank(list);
        const total = list.reduce((sum: number, r: { totalPang?: number }) => sum + (Number(r.totalPang) || 0), 0);
        setWeeklyTotal(total);
      })
      .catch(() => setStream(null))
      .finally(() => setLoading(false));
  }, [streamId]);

  // 시청자 수 집계: 라이브 방송 시 viewer/join, 이탈 시 viewer/leave
  useEffect(() => {
    const id = streamId ? Number(streamId) : NaN;
    if (!streamId || Number.isNaN(id) || !stream || stream.status !== 'LIVE') return;
    fetch(apiUrl(`api/streams/${id}/viewer/join`), { method: 'POST', credentials: 'include' })
      .then((r) => {
        if (r.ok) {
          joinedStreamIdRef.current = id;
          // 진입 직후 시청자 수 한 번 더 조회해 즉시 반영
          fetch(apiUrl(`api/streams/${id}`), { credentials: 'include' })
            .then((res) => (res.ok ? res.json() : null))
            .then((s: StreamInfo | null) => {
              if (s) setStream((prev) => (prev ? { ...prev, viewerCount: s.viewerCount ?? prev.viewerCount } : null));
            })
            .catch(() => {});
        }
      })
      .catch(() => {});
    return () => {
      if (joinedStreamIdRef.current != null) {
        fetch(apiUrl(`api/streams/${joinedStreamIdRef.current}/viewer/leave`), { method: 'POST', credentials: 'include' }).catch(() => {});
        joinedStreamIdRef.current = null;
      }
    };
  }, [streamId, stream?.id, stream?.status]);

  // 시청자 수 실시간 갱신 (10초마다)
  useEffect(() => {
    const id = streamId ? Number(streamId) : NaN;
    if (!streamId || Number.isNaN(id) || !stream) return;
    const interval = setInterval(() => {
      fetch(apiUrl(`api/streams/${id}`), { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : null))
        .then((s: StreamInfo | null) => {
          if (s && stream) setStream((prev) => (prev ? { ...prev, viewerCount: s.viewerCount ?? prev.viewerCount } : null));
        })
        .catch(() => {});
    }, 10000);
    return () => clearInterval(interval);
  }, [streamId, stream?.id]);

  const isLive = stream?.status === 'LIVE' && !stream?.endedAt;
  const gameLabel =
    stream?.game === 'LEAGUE_OF_LEGENDS'
      ? '리그 오브 레전드'
      : stream?.game === 'PUBG'
        ? '배틀그라운드'
        : stream?.game === 'VALORANT'
          ? '발로란트'
          : stream?.game ?? '—';

  // 채팅 히스토리 로드 + WebSocket 연결
  useEffect(() => {
    const id = streamId ? Number(streamId) : NaN;
    if (!streamId || Number.isNaN(id)) return;

    setChatNotice(user ? '채팅 연결 중...' : '로그인 후 채팅을 이용할 수 있습니다.');
    fetch(apiUrl(`api/streams/${id}/chat-timeline?limit=100`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: unknown) => {
        if (!Array.isArray(list)) return;
        const items: ChatMessageItem[] = [];
        for (const item of list as Record<string, unknown>[]) {
          if (!item) continue;
          if (item.type === 'donation') {
            items.push({
              type: 'donation',
              donorName: item.donorName as string,
              amount: item.amount as number,
              tier: item.tier as string,
              donorMessage: item.donorMessage as string,
              donorProfileImageUrl: item.donorProfileImageUrl as string,
            });
          } else if (item.displayName != null && item.text != null) {
            items.push({
              type: 'chat',
              displayName: item.displayName as string,
              text: item.text as string,
              streamer: !!item.streamer,
              profileImageUrl: item.profileImageUrl as string,
              userId: item.userId as number,
              level: item.level as number,
            });
          }
        }
        setChatMessages(items);
      })
      .catch(() => {});

    const wsUrl = getWsUrl();
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as unknown as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        setChatConnected(true);
        setChatNotice('');
        client.subscribe('/topic/stream/' + id, (message) => {
          if (!message?.body) return;
          try {
            const d = JSON.parse(message.body) as Record<string, unknown>;
            if (d?.type === 'donation') {
              const donorUserId = d.donorUserId != null ? Number(d.donorUserId) : undefined;
              if (donorUserId != null) donorUserIdsRef.current.add(donorUserId);
              setChatMessages((prev) => [
                ...prev,
                {
                  type: 'donation',
                  donorName: d.donorName as string,
                  amount: d.amount as number,
                  tier: (d.tier as string) || '팡',
                  donorMessage: d.donorMessage as string,
                  donorProfileImageUrl: d.donorProfileImageUrl as string,
                  consecutiveDonationDays: d.consecutiveDonationDays as number,
                  donorUserId,
                },
              ]);
              // 주간 후원 랭킹 실시간 반영 (후원 애니메이션은 OBS 전용 URL에서만 표시)
              fetch(apiUrl(`api/streams/${id}/weekly-donor-rank`), { credentials: 'include' })
                .then((r) => (r.ok ? r.json() : []))
                .then((list: unknown) => {
                  const arr = Array.isArray(list) ? list : [];
                  setWeeklyRank(arr);
                  setWeeklyTotal(arr.reduce((sum: number, r: { totalPang?: number }) => sum + (Number(r.totalPang) || 0), 0));
                })
                .catch(() => {});
              return;
            }
            if (d?.type === 'system' && d.text) {
              setChatMessages((prev) => [...prev, { type: 'system', text: d.text as string }]);
              return;
            }
            if (d?.displayName != null && d?.text != null) {
              setChatMessages((prev) => [
                ...prev,
                {
                  type: 'chat',
                  displayName: d.displayName as string,
                  text: d.text as string,
                  streamer: !!d.streamer,
                  profileImageUrl: d.profileImageUrl as string,
                  userId: d.userId as number,
                  level: d.level as number,
                },
              ]);
            }
          } catch {
            /* ignore */
          }
        });
      },
      onStompError: () => setChatNotice('채팅 연결에 실패했습니다. 새로고침해 주세요.'),
    });
    stompClientRef.current = client;
    client.activate();

    return () => {
      try {
        client.deactivate();
      } catch {
        /* ignore */
      }
      stompClientRef.current = null;
      setChatConnected(false);
    };
  }, [streamId, user]);

  useEffect(() => {
    const streamerId = stream?.userId;
    if (!user || !streamerId) {
      setIsSubscribed(false);
      return;
    }
    fetch(apiUrl(`api/subscription/check?userId=${encodeURIComponent(String(streamerId))}`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : { subscribed: false }))
      .then((d: { subscribed?: boolean }) => setIsSubscribed(!!d.subscribed))
      .catch(() => setIsSubscribed(false));
  }, [user, stream?.userId]);

  const sendChat = useCallback(() => {
    const text = chatInput.trim();
    if (!text || !streamId || !chatConnected) return;
    try {
      stompClientRef.current?.publish({ destination: '/app/chat/' + streamId, body: text });
      setChatInput('');
    } catch {
      /* ignore */
    }
  }, [chatInput, streamId, chatConnected]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages.length]);

  const submitDonation = useCallback(() => {
    const id = streamId ? Number(streamId) : NaN;
    if (Number.isNaN(id) || !user) {
      setDonationError('로그인 후 후원할 수 있습니다.');
      return;
    }
    const amount = Math.floor(Number(donationAmount) || 0);
    if (amount < 1000) {
      setDonationError('최소 1,000팡 이상 후원해 주세요.');
      return;
    }
    setDonationError('');
    setDonationSubmitting(true);
    fetch(apiUrl('api/donate'), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        streamId: id,
        amount,
        message: donationMessage.trim() || undefined,
      }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then(({ ok, data }) => {
        if (ok) {
          setDonationModalOpen(false);
          setDonationAmount('');
          setDonationMessage('');
          setWeeklyTotal((prev) => (prev ?? 0) + amount);
        } else {
          setDonationError((data?.message as string) || '후원에 실패했습니다.');
        }
      })
      .catch(() => setDonationError('요청에 실패했습니다.'))
      .finally(() => setDonationSubmitting(false));
  }, [streamId, user, donationAmount, donationMessage]);

  const submitSubscription = useCallback(() => {
    const streamerId = stream?.userId;
    if (!user) {
      setSubscriptionError('로그인 후 구독할 수 있습니다.');
      return;
    }
    if (!streamerId) {
      setSubscriptionError('스트리머 정보를 찾을 수 없습니다.');
      return;
    }
    if (!subscriptionAgree) {
      setSubscriptionError('정기구독 자동결제 동의가 필요합니다.');
      return;
    }
    setSubscriptionError('');
    setSubscriptionSubmitting(true);
    fetch(apiUrl('api/subscription/orders'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ userId: streamerId }),
    })
      .then((r) => r.json().then((d: { orderId?: string; amount?: number; orderName?: string; storeId?: string; pg?: string; payMethod?: string; message?: string }) => ({ status: r.status, data: d })))
      .then((res) => {
        if (res.status !== 200 || !res.data?.orderId || res.data.amount == null) {
          setSubscriptionError(res.data?.message ?? '구독 주문 생성에 실패했습니다.');
          return;
        }
        const { orderId, amount, orderName, storeId, pg, payMethod } = res.data;
        if (!storeId || typeof window.IMP === 'undefined') {
          setSubscriptionError('결제를 사용할 수 없습니다. 관리자에게 문의하세요.');
          return;
        }
        window.IMP.init(storeId);
        window.IMP.request_pay(
          {
            pg: pg || 'html5_inicis.INIpayTest',
            pay_method: payMethod || 'card',
            merchant_uid: orderId,
            amount,
            name: orderName || 'GameMatcher 정기구독',
            buyer_name: user?.nickname || user?.username || undefined,
          },
          (response) => {
            if (!response.success || !response.imp_uid) {
              setSubscriptionError(response.error_msg || '결제가 취소되었거나 실패했습니다.');
              setSubscriptionSubmitting(false);
              return;
            }
            fetch(apiUrl('api/subscription/confirm'), {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              credentials: 'include',
              body: JSON.stringify({ orderId, impUid: response.imp_uid }),
            })
              .then((r) => r.json().then((d: { subscribed?: boolean; message?: string }) => ({ ok: r.ok, data: d })))
              .then((confirmRes) => {
                if (confirmRes.ok && confirmRes.data?.subscribed) {
                  setIsSubscribed(true);
                  setSubscriptionModalOpen(false);
                  setSubscriptionAgree(false);
                } else {
                  setSubscriptionError(confirmRes.data?.message ?? '구독 처리에 실패했습니다.');
                }
              })
              .catch(() => setSubscriptionError('구독 확인 중 오류가 발생했습니다.'))
              .finally(() => setSubscriptionSubmitting(false));
          }
        );
      })
      .catch(() => {
        setSubscriptionError('네트워크 오류가 발생했습니다.');
        setSubscriptionSubmitting(false);
      });
  }, [stream?.userId, user, subscriptionAgree]);

  // 스트리밍 경과 시간 1초마다 갱신
  useEffect(() => {
    if (!isLive || !stream?.startedAt) {
      setStreamDuration('');
      return;
    }
    setStreamDuration(formatDuration(stream.startedAt));
    durationIntervalRef.current = setInterval(() => {
      setStreamDuration(formatDuration(stream.startedAt ?? null));
    }, 1000);
    return () => {
      if (durationIntervalRef.current) {
        clearInterval(durationIntervalRef.current);
        durationIntervalRef.current = null;
      }
    };
  }, [isLive, stream?.startedAt]);

  // HLS 재생 (외부 URL이 있으면 iframe만 쓰고 HLS는 스킵)
  useEffect(() => {
    if (!stream || loading) return;
    if (stream.externalUrl) {
      setPlayerError(null);
      setPlayerLoading(false);
      return;
    }
    const video = videoRef.current;
    if (!video) return;

    setPlayerError(null);

    // LIVE + playbackUrl: HLS
    if (isLive && stream.playbackUrl) {
      setPlayerLoading(true);
      if (Hls.isSupported()) {
        const hls = new Hls();
        hlsRef.current = hls;
        hls.loadSource(stream.playbackUrl);
        hls.attachMedia(video);
        const tryPlay = () => {
          setPlayerLoading(false);
          video.muted = true;
          video.play().catch(() => {});
        };
        hls.on(Hls.Events.MANIFEST_PARSED, tryPlay);
        hls.on(Hls.Events.LEVEL_LOADED, tryPlay);
        video.addEventListener('loadeddata', tryPlay);
        video.addEventListener('canplay', tryPlay);
        video.addEventListener('canplaythrough', tryPlay);
        setTimeout(tryPlay, 300);
        hls.on(Hls.Events.ERROR, (_e, d) => {
          if (d.fatal) {
            setPlayerError('재생할 수 없습니다. 방송이 아직 시작되지 않았거나 종료되었을 수 있습니다.');
            setPlayerLoading(false);
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = stream.playbackUrl;
        const tryPlay = () => {
          setPlayerLoading(false);
          video.muted = true;
          video.play().catch(() => {});
        };
        video.addEventListener('loadedmetadata', tryPlay);
        video.addEventListener('loadeddata', tryPlay);
        video.addEventListener('canplay', tryPlay);
        video.addEventListener('canplaythrough', tryPlay);
        setTimeout(tryPlay, 300);
      } else {
        setPlayerError('이 브라우저는 HLS 재생을 지원하지 않습니다.');
        setPlayerLoading(false);
      }
      return () => {
        if (hlsRef.current) {
          hlsRef.current.destroy();
          hlsRef.current = null;
        }
        video.removeAttribute('src');
        video.load();
      };
    }

    setPlayerLoading(false);
  }, [stream?.id, isLive, stream?.playbackUrl, stream?.externalUrl, loading]);

  // 스트리머 프로필 이미지 URL이 바뀌면 로드 에러 플래그 초기화
  useEffect(() => {
    setStreamerAvatarError(false);
  }, [stream?.broadcasterProfileImageUrl]);

  const onPlayPause = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play().catch(() => {});
      setIsPaused(false);
    } else {
      video.pause();
      setIsPaused(true);
    }
  }, []);

  const onMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !video.muted;
    setIsMuted(video.muted);
  }, []);

  const onFullscreen = useCallback(() => {
    const wrap = playerWrapRef.current;
    if (!wrap) return;
    if (!document.fullscreenElement) {
      wrap.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen();
    }
  }, []);

  const embedUrl = stream?.externalUrl ? toEmbedVideoUrl(stream.externalUrl) : null;
  const showVideo = isLive && (stream?.playbackUrl || embedUrl);

  return (
    <WatchLayout>
      <div className="main-wrap">
        <div className="video-section">
          <div
            className="player-wrap"
            ref={playerWrapRef}
            onMouseEnter={() => setControlsVisible(true)}
            onMouseLeave={() => setControlsVisible(false)}
          >
            {showVideo && embedUrl ? (
              <iframe
                title="방송"
                src={embedUrl}
                className="player-embed"
                allowFullScreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              />
            ) : showVideo && stream?.playbackUrl ? (
              <>
                <video
                  ref={videoRef}
                  playsInline
                  muted={isMuted}
                  onClick={onPlayPause}
                  onPlay={() => setIsPaused(false)}
                  onPause={() => setIsPaused(true)}
                  style={{ display: playerError ? 'none' : 'block' }}
                />
                {isLive && <div className="player-live-badge show">LIVE</div>}
                <div className="player-controls-bar">
                  <button type="button" className="btn-control" onClick={onPlayPause} title="재생/일시정지" aria-label="재생 일시정지">
                    {isPaused ? (
                      <svg className="icon-play" viewBox="0 0 24 24">
                        <polygon points="5 3 19 12 5 21 5 3" fill="currentColor" />
                      </svg>
                    ) : (
                      <svg className="icon-pause" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                        <rect x="6" y="4" width="4" height="16" rx="1" />
                        <rect x="14" y="4" width="4" height="16" rx="1" />
                      </svg>
                    )}
                  </button>
                  <button type="button" className="btn-control" onClick={onMute} title="음소거" aria-label="음량">
                    {isMuted ? (
                      <svg className="icon-volume-off" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                        <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                        <line x1="23" y1="9" x2="17" y2="15" />
                        <line x1="17" y1="9" x2="23" y2="15" />
                      </svg>
                    ) : (
                      <svg className="icon-volume-on" viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                        <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                        <path d="M15.54 8.46a5 5 0 0 1 0 7.07M19.07 4.93a10 10 0 0 1 0 14.14" />
                      </svg>
                    )}
                  </button>
                  <span className="live-label">
                    <span className="live-dot" />
                    실시간
                  </span>
                  <span className="control-spacer" />
                  <button type="button" className="btn-control" onClick={onFullscreen} title="전체화면" aria-label="전체화면">
                    <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                      <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
                    </svg>
                  </button>
                </div>
                {playerLoading && <div className="player-loading">로딩 중...</div>}
                {playerError && <div className="player-error-msg">{playerError}</div>}
              </>
            ) : (
              <>
                {loading && <div className="watch-player-placeholder">불러오는 중...</div>}
                {!loading && !stream && <div className="watch-player-placeholder">방송을 찾을 수 없습니다.</div>}
                {!loading && stream && !isLive && <div className="watch-player-placeholder">오프라인</div>}
                {!loading && stream && isLive && !stream.playbackUrl && !embedUrl && (
                  <div className="watch-player-placeholder">재생 URL이 없습니다. 방송이 곧 시작될 수 있습니다.</div>
                )}
              </>
            )}
          </div>
          {stream && !isLive && (
            <div className="player-end-msg">방송이 종료되었습니다.</div>
          )}
          {stream && (
            <div className="stream-info">
              <div className="title">{stream.title || '제목 없음'}</div>
              <div className="stream-meta">
                <span className="game">{gameLabel}</span>
                <span className="viewers">{stream.viewerCount ?? 0}명 시청 중</span>
                {isLive && streamDuration && <span className="stream-info-duration">{streamDuration}</span>}
                {isLive && <span className="live-badge">LIVE</span>}
              </div>
              <div className="streamer-row">
                <div className="streamer-avatar-wrap">
                  {resolveProfileImageUrl(stream.broadcasterProfileImageUrl) && !streamerAvatarError ? (
                    <img
                      className="streamer-avatar"
                      src={resolveProfileImageUrl(stream.broadcasterProfileImageUrl)!}
                      alt=""
                      onError={() => setStreamerAvatarError(true)}
                    />
                  ) : (
                    <span className="streamer-avatar-fallback">{(stream.broadcasterNickname || '?')[0]}</span>
                  )}
                </div>
                <div className="streamer-info-col">
                  <div className="streamer-name">
                    {stream.broadcasterNickname || '스트리머'}
                    {stream.partner && <span className="streamer-partner-badge" title="파트너 스트리머">✓</span>}
                  </div>
                  <div className="streamer-followers">팔로워 {stream.followerCount ?? 0}명</div>
                </div>
                <div className="stream-actions">
                  <button
                    type="button"
                    className={`btn-follow ${isFollowing ? 'following' : ''}`}
                    onClick={() => setIsFollowing((v) => !v)}
                  >
                    {isFollowing ? '팔로잉' : '팔로우'}
                  </button>
                  <button
                    type="button"
                    className={`btn-sub ${isSubscribed ? 'subscribed' : ''}`}
                    onClick={() => {
                      if (!isSubscribed) {
                        setSubscriptionError('');
                        setSubscriptionModalOpen(true);
                        return;
                      }
                      if (!stream?.userId) return;
                      fetch(apiUrl(`api/subscription/${stream.userId}`), { method: 'DELETE', credentials: 'include' })
                        .then((r) => (r.ok ? r.json() : { subscribed: true }))
                        .then((d: { subscribed?: boolean }) => setIsSubscribed(!!d.subscribed))
                        .catch(() => {});
                    }}
                  >
                    구독
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {!chatPanelOpen && (
          <button type="button" className="chat-open-fab" onClick={() => setChatPanelOpen(true)} title="채팅 열기" aria-label="채팅 열기">
            채팅
          </button>
        )}
        <aside className={`chat-panel ${chatPanelOpen ? '' : 'chat-panel-closed'}`}>
          <div className="chat-header">
            <span className="chat-title-text">채팅</span>
            <button type="button" className="chat-settings-btn" title="채팅 설정" aria-label="채팅 설정">
              <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                <circle cx="12" cy="12" r="3" />
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-1.08a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h1.08a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v1.08a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-1.08a1.65 1.65 0 0 0-1.51 1z" />
              </svg>
            </button>
            <button type="button" className="chat-toggle-btn" onClick={() => setChatPanelOpen(false)} title="채팅창 닫기" aria-label="채팅창 닫기">
              <svg viewBox="0 0 24 24" stroke="currentColor" fill="none" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div className={`chat-weekly-rank ${weeklyRankView}`}>
            <button type="button" className="chat-weekly-rank-header" onClick={() => setWeeklyRankView((v) => (v === 'collapsed' ? 'expanded' : 'collapsed'))} aria-expanded={weeklyRankView !== 'collapsed'}>
              <span className="chat-weekly-rank-title">주간 후원 랭킹</span>
              <span className="chat-weekly-rank-toggle-icon" aria-hidden="true">
                {weeklyRankView === 'collapsed' ? (
                  <svg viewBox="0 0 24 24" width={16} height={16} stroke="currentColor" fill="none" strokeWidth="2"><polyline points="6 9 12 15 18 9"/></svg>
                ) : (
                  <svg viewBox="0 0 24 24" width={16} height={16} stroke="currentColor" fill="none" strokeWidth="2"><polyline points="18 15 12 9 6 15"/></svg>
                )}
              </span>
            </button>
            <div className="chat-weekly-rank-hint">이 방송(계정) 기준이며, 방송을 켜고 꺼도 유지됩니다. 매주 월요일 0시에 리셋됩니다.</div>
            {weeklyRank.length > 0 ? (
              <>
                {/* 접힌 상태: 상위 3명만 한 줄로 */}
                {weeklyRankView === 'collapsed' && (
                  <div className="chat-weekly-rank-collapsed">
                    {weeklyRank.slice(0, 3).map((r, i) => {
                      const rank = r.rank ?? i + 1;
                      const badgeEmoji = rank === 1 ? '🥇' : rank === 2 ? '🥈' : '🥉';
                      return (
                        <div key={i} className="chat-weekly-rank-collapsed-item">
                          <span className="chat-weekly-rank-collapsed-badge">{badgeEmoji}</span>
                          <span className="chat-weekly-rank-collapsed-name">{r.displayName || '—'}</span>
                          <span className="chat-weekly-rank-collapsed-pang">{(Number(r.totalPang) || 0).toLocaleString()}</span>
                        </div>
                      );
                    })}
                  </div>
                )}
                {/* 펼침/전체: 카드형 목록 */}
                {(weeklyRankView === 'expanded' || weeklyRankView === 'full') && (
                  <ul className="chat-weekly-rank-list">
                    {weeklyRank.slice(0, 10).map((r, i) => {
                      const rank = r.rank ?? i + 1;
                      const badgeEmoji = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : rank >= 4 && rank <= 10 ? '⭐' : '★';
                      const badgeClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : rank >= 4 && rank <= 10 ? 'rank-star' : 'rank-other';
                      const consecutiveDays = r.consecutiveDonationDays != null ? Number(r.consecutiveDonationDays) : 0;
                      return (
                        <li key={i} className="chat-weekly-rank-item">
                          <div className={`chat-weekly-rank-badge ${badgeClass}`}>{badgeEmoji}</div>
                          <div className="chat-weekly-rank-name">{r.displayName || '—'}</div>
                          <div className="chat-weekly-rank-pang">
                            <img src={apiUrl('images/pang-sparkle.svg')} alt="" />
                            {(Number(r.totalPang) || 0).toLocaleString()}
                          </div>
                          {consecutiveDays >= 1 && (
                            <div className="chat-weekly-rank-consecutive">연속후원 {consecutiveDays}일</div>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                )}
                {(weeklyRankView === 'expanded' || weeklyRankView === 'full') && (
                  <div className="chat-weekly-rank-actions">
                    {weeklyRankView === 'expanded' ? (
                      <button type="button" className="chat-weekly-rank-more-btn" onClick={() => setWeeklyRankView('full')}>더보기</button>
                    ) : (
                      <button type="button" className="chat-weekly-rank-fold-btn" onClick={() => setWeeklyRankView('collapsed')}>접기</button>
                    )}
                  </div>
                )}
              </>
            ) : (
              <div className="chat-weekly-rank-empty">이번 주 후원이 없습니다.</div>
            )}
          </div>
          <div className="chat-messages">
            {chatMessages.map((msg, idx) => {
              const chatUserId = msg.type === 'chat' ? msg.userId : undefined;
              const donorRank = chatUserId != null && weeklyRank.length >= 1 && Number(weeklyRank[0]?.userId) === chatUserId ? 1 : (chatUserId != null && weeklyRank.length >= 2 && Number(weeklyRank[1]?.userId) === chatUserId ? 2 : (chatUserId != null && weeklyRank.length >= 3 && Number(weeklyRank[2]?.userId) === chatUserId ? 3 : 0));
              const isDonor = chatUserId != null && donorUserIdsRef.current.has(chatUserId);
              return msg.type === 'chat' ? (
                <div key={idx} className={`chat-msg ${msg.streamer ? 'chat-msg-streamer' : ''} ${isDonor ? 'chat-msg-donor' : ''}`}>
                  <div className="chat-msg-body">
                    {msg.streamer && <span className="streamer-badge">방송자</span>}
                    {msg.streamer && stream?.partner && <span className="chat-badge partner-badge" title="파트너">✓</span>}
                    {(msg.level != null && msg.level >= 1) && (
                      <span className={`chat-badge level-badge ${levelBadgeClass(msg.level)}`} title={`레벨 ${msg.level}`}>LV.{msg.level}</span>
                    )}
                    {isDonor && donorRank === 0 && <span className="chat-badge donor-badge donor-heart" title="후원자">❤</span>}
                    {donorRank === 1 && <span className="chat-badge donor-badge donor-rank-1" title="후원 1등">🥇</span>}
                    {donorRank === 2 && <span className="chat-badge donor-badge donor-rank-2" title="후원 2등">🥈</span>}
                    {donorRank === 3 && <span className="chat-badge donor-badge donor-rank-3" title="후원 3등">🥉</span>}
                    <span className="user">{msg.displayName ?? '—'}</span>
                    {msg.text}
                  </div>
                </div>
              ) : msg.type === 'donation' ? (
                <div key={idx} className="chat-msg chat-msg-donation-card">
                  <div className={`twitch-alert ${donationTierClass(msg.amount)}`}>
                    <div className="twitch-alert-header">{msg.tier ?? '팡'}</div>
                    <div className="twitch-alert-body">
                      <div className="twitch-alert-donor">
                        🎉 <span className="twitch-name">{(msg.donorName ?? '익명').trim()}님이</span> {(msg.amount ?? 0).toLocaleString()}팡 후원! 팡! <img src={apiUrl('images/pang-sparkle.svg')} alt="팡" className="pang-emoji-img" />
                      </div>
                      {msg.donorMessage && <div className="twitch-alert-msg">{msg.donorMessage}</div>}
                      {msg.consecutiveDonationDays != null && msg.consecutiveDonationDays >= 1 && (
                        <div className="twitch-alert-consecutive">연속후원 {msg.consecutiveDonationDays}일</div>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <div key={idx} className="chat-msg chat-msg-system">
                  <div className="chat-msg-body">{msg.text}</div>
                </div>
              );
            })}
            {chatNotice && <div className="chat-notice">{chatNotice}</div>}
            {!streamId && !chatNotice && <div className="chat-notice">방송을 선택하면 채팅이 표시됩니다.</div>}
            <div ref={chatEndRef} />
          </div>
          <div className="chat-input-wrap">
            <input
              type="text"
              className="chat-input"
              placeholder={user ? '메시지를 입력해주세요' : '로그인 후 채팅 가능'}
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), sendChat())}
              disabled={!user || !chatConnected}
            />
            <div className="chat-toolbar">
              <button type="button" className="btn-donate" onClick={() => setDonationModalOpen(true)}>
                후원하기
              </button>
              <button type="button" className="chat-toolbar-icon" title="채팅" aria-label="채팅">
                💬
              </button>
              <button type="button" className="chat-toolbar-icon" title="이미지" aria-label="이미지">
                🖼
              </button>
              <button type="button" className="chat-toolbar-icon" title="공유" aria-label="공유">
                ↗
              </button>
              <button type="button" className="chat-toolbar-icon" title="설정" aria-label="설정">
                ⚙
              </button>
            </div>
          </div>
        </aside>
      </div>

      {subscriptionModalOpen && (
        <div className="main-modal-backdrop show" role="dialog" aria-modal="true" onClick={() => setSubscriptionModalOpen(false)}>
          <div className="main-modal-box subscription-modal subscription-modal-8080" onClick={(e) => e.stopPropagation()}>
            <button type="button" className="subscription-modal-close" onClick={() => setSubscriptionModalOpen(false)} aria-label="닫기">&times;</button>
            <div className="subscription-modal-header">
              {resolveProfileImageUrl(stream?.broadcasterProfileImageUrl) ? (
                <img src={resolveProfileImageUrl(stream?.broadcasterProfileImageUrl)!} alt="" className="subscription-streamer-avatar" />
              ) : (
                <div className="subscription-streamer-avatar subscription-streamer-avatar-placeholder" aria-hidden>
                  {(stream?.broadcasterNickname || '?')[0]}
                </div>
              )}
              <div className="subscription-streamer-info">
                <span className="subscription-streamer-name">{stream?.broadcasterNickname || '스트리머'}</span>
                {stream?.partner && <span className="subscription-verified" title="파트너 스트리머">✓</span>}
              </div>
            </div>
            <div className="subscription-benefits">
              <h3>구독 혜택</h3>
              <ul>
                <li><span className="benefit-icon">❤</span> {stream?.broadcasterNickname || '스트리머'} 스트리머 자동 후원</li>
                <li><span className="benefit-icon">📺</span> {stream?.broadcasterNickname || '스트리머'} 방송 시청 시 재생 전, 중간 광고 제거</li>
                <li><span className="benefit-icon">💬</span> 구독자 전용 채팅</li>
                <li><span className="benefit-icon">G</span> GameMatcher 마일리지 10% 기본 적립!</li>
              </ul>
            </div>
            <div className="subscription-footer">
              <p>
                <label className="subscription-checkbox-label">
                  <input type="checkbox" checked={subscriptionAgree} onChange={(e) => setSubscriptionAgree(e.target.checked)} />
                  <span>정기구독 자동결제에 동의합니다.</span>
                </label>
                <button type="button" className="sub-guide-link">안내보기</button>
              </p>
              {subscriptionError && <p className="modal-error" style={{ marginBottom: 10 }}>{subscriptionError}</p>}
              <button type="button" className="btn-subscribe-submit" onClick={submitSubscription} disabled={!subscriptionAgree || subscriptionSubmitting}>
                {subscriptionSubmitting ? '처리 중…' : '매월 4,900원에 정기구독하기'}
              </button>
            </div>
          </div>
        </div>
      )}

      {donationModalOpen && (
        <div className="main-modal-backdrop show" role="dialog" aria-modal="true" onClick={() => !donationSubmitting && setDonationModalOpen(false)}>
          <div className="main-modal-box" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">후원하기</h2>
            <p className="modal-footer" style={{ marginBottom: 16 }}>팡으로 스트리머를 후원합니다. (1팡 = 1.2원)</p>
            {!user && <p className="modal-error">로그인 후 후원할 수 있습니다.</p>}
            <div className="modal-field">
              <label htmlFor="watch-donation-amount">팡 개수 (최소 1,000팡)</label>
              <input
                id="watch-donation-amount"
                type="number"
                min={1000}
                placeholder="1000"
                value={donationAmount}
                onChange={(e) => setDonationAmount(e.target.value)}
                disabled={!user}
              />
              <div className="donation-quick-btns">
                {[1000, 5000, 10000, 50000, 100000].map((amt) => (
                  <button
                    key={amt}
                    type="button"
                    className="donation-quick-btn"
                    onClick={() => setDonationAmount((prev) => String((Number(prev) || 0) + amt))}
                    disabled={!user}
                  >
                    {amt >= 10000 ? amt / 10000 + '만' : amt.toLocaleString()}
                  </button>
                ))}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="watch-donation-msg">메시지 (선택)</label>
              <input
                id="watch-donation-msg"
                type="text"
                placeholder="후원 메시지를 입력하세요"
                value={donationMessage}
                onChange={(e) => setDonationMessage(e.target.value)}
                disabled={!user}
              />
            </div>
            {donationError && <p className="modal-error">{donationError}</p>}
            <div className="modal-actions">
              <button type="button" className="btn-primary" onClick={submitDonation} disabled={!user || donationSubmitting}>
                {donationSubmitting ? '처리 중…' : '후원하기'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => { if (!donationSubmitting) setDonationModalOpen(false); }} disabled={donationSubmitting}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </WatchLayout>
  );
}
