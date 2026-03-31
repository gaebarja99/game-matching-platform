import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { apiFetch } from '../api/client';
import StreamsLayout from './StreamsLayout';

const STUDIO_SIDEBAR_STORAGE_KEY = 'gamematcher-studio-sidebar-open';

function loadStoredOpenGroups(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(STUDIO_SIDEBAR_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Record<string, boolean>;
      if (parsed && typeof parsed === 'object') return parsed;
    }
  } catch {
    /* ignore */
  }

  return {
    broadcast: true,
    analysis: true,
    viewers: true,
    revenue: true,
    channel: true,
  };
}

function saveOpenGroups(groups: Record<string, boolean>) {
  try {
    localStorage.setItem(STUDIO_SIDEBAR_STORAGE_KEY, JSON.stringify(groups));
  } catch {
    /* ignore */
  }
}

interface StudioLayoutProps {
  children: React.ReactNode;
}

type ManagedChannel = {
  ownerUserId: number;
  ownerNickname: string;
  ownerLoginId: string;
  roleName: string;
};

export default function StudioLayout({ children }: StudioLayoutProps) {
  const location = useLocation();
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(loadStoredOpenGroups);
  const [managedChannels, setManagedChannels] = useState<ManagedChannel[]>([]);

  const path = location.pathname;
  const ownerUserIdParam = new URLSearchParams(location.search).get('ownerUserId');
  const parsedOwnerUserId = ownerUserIdParam ? Number(ownerUserIdParam) : NaN;
  const selectedOwnerUserId = Number.isFinite(parsedOwnerUserId) ? parsedOwnerUserId : null;
  const isDashboard = path === '/studio';
  const isBroadcast = path.startsWith('/studio/live') || path.startsWith('/studio/settings') || path.startsWith('/studio/alerts');
  const isAnalysis = path.startsWith('/studio/analysis');
  const isViewers = path.startsWith('/studio/viewers');
  const isRevenue = path.startsWith('/studio/revenue');
  const isChannel = path.startsWith('/studio/channel') || path.startsWith('/studio/chat');

  useEffect(() => {
    let cancelled = false;

    const loadManagedChannels = async () => {
      const query = selectedOwnerUserId ? `?ownerUserId=${selectedOwnerUserId}` : '';
      const response = await apiFetch<{ managedChannels?: ManagedChannel[] }>(`api/studio/channel/context${query}`);
      if (cancelled) return;
      if (!response.ok || !response.data) {
        setManagedChannels([]);
        return;
      }
      setManagedChannels(Array.isArray(response.data.managedChannels) ? response.data.managedChannels : []);
    };

    loadManagedChannels();

    return () => {
      cancelled = true;
    };
  }, [selectedOwnerUserId]);

  useEffect(() => {
    const nextOpen: Record<string, boolean> = {};
    if (isBroadcast) nextOpen.broadcast = true;
    if (isAnalysis) nextOpen.analysis = true;
    if (isViewers) nextOpen.viewers = true;
    if (isRevenue) nextOpen.revenue = true;
    if (isChannel) nextOpen.channel = true;

    if (Object.keys(nextOpen).length > 0) {
      setOpenGroups((prev) => {
        const merged = { ...prev, ...nextOpen };
        saveOpenGroups(merged);
        return merged;
      });
    }
  }, [isAnalysis, isBroadcast, isChannel, isRevenue, isViewers]);

  const toggleGroup = (key: string) => {
    setOpenGroups((prev) => {
      const next = { ...prev, [key]: !prev[key] };
      saveOpenGroups(next);
      return next;
    });
  };

  const buildManagedChannelLink = (ownerUserId: number, section: 'manage' | 'permissions' = 'manage') =>
    `/studio/channel/${section}?ownerUserId=${ownerUserId}`;

  const isManagedChannelActive = (ownerUserId: number) =>
    isChannel && selectedOwnerUserId === ownerUserId;

  return (
    <StreamsLayout sidebarVariant="full" showBroadcastSidebar={false}>
      <div className="studio-page studio-page--in-streams">
        <div className="studio-body">
          <button
            type="button"
            className="studio-sidebar-menu-btn"
            onClick={() => setSidebarVisible((visible) => !visible)}
            aria-label="스튜디오 메뉴"
            aria-expanded={sidebarVisible}
          >
            <svg viewBox="0 0 24 24" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={2}>
              <line x1={3} y1={6} x2={21} y2={6} />
              <line x1={3} y1={12} x2={21} y2={12} />
              <line x1={3} y1={18} x2={21} y2={18} />
            </svg>
          </button>
          <aside className="studio-sidebar" style={{ display: sidebarVisible ? undefined : 'none' }}>
            <div className="nav-section">
              <div className="nav-section-title">스튜디오</div>
              <Link to="/studio" className={`nav-item ${isDashboard ? 'active' : ''}`}>
                <span className="icon">📋</span>
                대시보드
              </Link>
            </div>

            <div className="nav-section">
              <div className="nav-section-title">방송 관리</div>
              <div className={`nav-group ${openGroups.broadcast ? 'open' : ''}`}>
                <button type="button" className={`nav-item nav-trigger ${isBroadcast ? 'active' : ''}`} onClick={() => toggleGroup('broadcast')}>
                  <span className="icon">📺</span>
                  방송 관리
                  <span className="nav-arrow">▲</span>
                </button>
                <ul className="nav-sub">
                  <li>
                    <Link to="/studio/live" className={path === '/studio/live' ? 'active' : ''}>
                      <span className="icon">📁</span>
                      방송하기
                    </Link>
                  </li>
                  <li>
                    <Link to="/studio/settings" className={path === '/studio/settings' ? 'active' : ''}>
                      <span className="icon">⚙️</span>
                      설정
                    </Link>
                  </li>
                  <li>
                    <Link to="/studio/alerts" className={path === '/studio/alerts' ? 'active' : ''}>
                      <span className="icon">🔔</span>
                      알림
                    </Link>
                  </li>
                </ul>
              </div>
            </div>

            <div className="nav-section">
              <div className="nav-section-title">분석</div>
              <div className={`nav-group ${openGroups.analysis ? 'open' : ''}`}>
                <button type="button" className={`nav-item nav-trigger ${isAnalysis ? 'active' : ''}`} onClick={() => toggleGroup('analysis')}>
                  <span className="icon">📊</span>
                  라이브 분석
                  <span className="nav-arrow">▲</span>
                </button>
                <ul className="nav-sub">
                  <li>
                    <Link to="/studio/analysis/live" className={path === '/studio/analysis/live' ? 'active' : ''}>
                      라이브 분석
                    </Link>
                  </li>
                </ul>
              </div>
            </div>

            <div className="nav-section">
              <div className="nav-section-title">시청자 관리</div>
              <div className={`nav-group ${openGroups.viewers ? 'open' : ''}`}>
                <button type="button" className={`nav-item nav-trigger ${isViewers ? 'active' : ''}`} onClick={() => toggleGroup('viewers')}>
                  <span className="icon">👥</span>
                  시청자 관리
                  <span className="nav-arrow">▲</span>
                </button>
                <ul className="nav-sub">
                  <li>
                    <Link to="/studio/viewers/followers" className={path === '/studio/viewers/followers' ? 'active' : ''}>
                      팔로워
                    </Link>
                  </li>
                  <li>
                    <Link to="/studio/viewers/fans" className={path === '/studio/viewers/fans' ? 'active' : ''}>
                      팬
                    </Link>
                  </li>
                  <li>
                    <Link to="/studio/viewers/subscribers" className={path === '/studio/viewers/subscribers' ? 'active' : ''}>
                      구독자
                    </Link>
                  </li>
                  <li>
                    <Link
                      to="/studio/viewers/blacklist"
                      className={path === '/studio/viewers/blacklist' || path === '/studio/viewers/blocklist' ? 'active' : ''}
                    >
                      블랙리스트
                    </Link>
                  </li>
                </ul>
              </div>
            </div>

            <div className="nav-section">
              <div className="nav-section-title">수익</div>
              <div className={`nav-group ${openGroups.revenue ? 'open' : ''}`}>
                <button type="button" className={`nav-item nav-trigger ${isRevenue ? 'active' : ''}`} onClick={() => toggleGroup('revenue')}>
                  <span className="icon">💰</span>
                  수익 관리
                  <span className="nav-arrow">▲</span>
                </button>
                <ul className="nav-sub">
                  <li>
                    <Link to="/studio/revenue" className={path === '/studio/revenue' ? 'active' : ''}>
                      수익 현황
                    </Link>
                  </li>
                </ul>
              </div>
            </div>

            <div className="nav-section">
              <div className="nav-section-title">채널</div>
              <div className={`nav-group ${openGroups.channel ? 'open' : ''}`}>
                <button type="button" className={`nav-item nav-trigger ${isChannel ? 'active' : ''}`} onClick={() => toggleGroup('channel')}>
                  <span className="icon">🔗</span>
                  채널/권한 관리
                  <span className="nav-arrow">▲</span>
                </button>
                <ul className="nav-sub">
                  <li>
                    <Link to="/studio/channel/manage" className={path === '/studio/channel/manage' && !selectedOwnerUserId ? 'active' : ''}>
                      채널 관리
                    </Link>
                  </li>
                  <li>
                    <Link to="/studio/channel/permissions" className={path === '/studio/channel/permissions' && !selectedOwnerUserId ? 'active' : ''}>
                      권한 관리
                    </Link>
                  </li>
                  {managedChannels.map((channel) => (
                    <li key={channel.ownerUserId}>
                      <Link
                        to={buildManagedChannelLink(channel.ownerUserId)}
                        className={isManagedChannelActive(channel.ownerUserId) ? 'active' : ''}
                      >
                        {channel.ownerNickname}의 채널
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </aside>

          <main className="studio-main">{children}</main>
        </div>
      </div>
    </StreamsLayout>
  );
}
