import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import {
  fetchAccountConnections,
  linkRiotAccount,
  startOAuthLink,
  unlinkAccount,
  type AccountConnectionStatus,
} from '../api/accountLinks';
import { patchProfile, type ProfilePatchBody } from '../api/profile';
import { RiotLinkedGameStats } from '../components/RiotLinkedGameStats';

type OAuthProvider = 'discord' | 'steam' | 'blizzard';

const PROVIDERS: Array<{
  key: 'DISCORD' | 'STEAM' | 'BLIZZARD' | 'RIOT';
  title: string;
  description: string;
  oauth?: OAuthProvider;
}> = [
  { key: 'DISCORD', title: 'Discord', description: '디스코드 프로필과 연결합니다.', oauth: 'discord' },
  { key: 'STEAM', title: 'Steam', description: 'Steam 계정을 연결합니다.', oauth: 'steam' },
  { key: 'BLIZZARD', title: 'Blizzard', description: 'Battle.net 계정을 연결합니다.', oauth: 'blizzard' },
  { key: 'RIOT', title: 'Riot', description: '게임명과 태그로 Riot 계정을 연결합니다.' },
];

export default function ProfileAccountLinks() {
  const { user } = useAuth();
  const [connections, setConnections] = useState<AccountConnectionStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [oauthBusy, setOauthBusy] = useState<OAuthProvider | null>(null);
  const [disconnectBusy, setDisconnectBusy] = useState<string | null>(null);
  const [riotGameName, setRiotGameName] = useState('');
  const [riotTagLine, setRiotTagLine] = useState('');
  const [riotBusy, setRiotBusy] = useState(false);
  const [linkPublicBusy, setLinkPublicBusy] = useState<string | null>(null);

  const loadConnections = async () => {
    setLoading(true);
    try {
      const { ok, data, message: errorMessage } = await fetchAccountConnections();
      if (ok && data?.connections) {
        setConnections(data.connections);
        setMessage({ text: '', ok: null });
      } else {
        setMessage({ text: errorMessage || '연동 정보를 불러오지 못했습니다.', ok: false });
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) return;
    loadConnections().catch(() => {
      setLoading(false);
      setMessage({ text: '연동 정보를 불러오지 못했습니다.', ok: false });
    });
  }, [user?.id]);

  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const payload = event.data as {
        type?: string;
        result?: string;
        message?: string;
      } | null;

      if (!payload || payload.type !== 'ACCOUNT_LINK_RESULT') return;

      const success = payload.result === 'success';
      setOauthBusy(null);
      setMessage({
        text: payload.message || (success ? '계정 연동이 완료되었습니다.' : '계정 연동에 실패했습니다.'),
        ok: success,
      });
      if (success) {
        loadConnections().catch(() => {});
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const openPopup = (url: string, title: string) => {
    const width = 560;
    const height = 760;
    const left = Math.max(0, Math.round(window.screenX + (window.outerWidth - width) / 2));
    const top = Math.max(0, Math.round(window.screenY + (window.outerHeight - height) / 2));
    return window.open(
      url,
      title,
      `popup=yes,width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`,
    );
  };

  const handleOAuthConnect = async (provider: OAuthProvider) => {
    setOauthBusy(provider);
    setMessage({ text: '', ok: null });
    try {
      const { ok, data, message: errorMessage } = await startOAuthLink(provider);
      if (!ok || !data?.authorizationUrl) {
        setMessage({ text: errorMessage || '연동 페이지를 열지 못했습니다.', ok: false });
        setOauthBusy(null);
        return;
      }

      const popup = openPopup(data.authorizationUrl, `link-${provider}`);
      if (!popup) {
        window.location.href = data.authorizationUrl;
        return;
      }
      popup.focus();
    } catch {
      setMessage({ text: '연동 페이지를 열지 못했습니다.', ok: false });
      setOauthBusy(null);
    }
  };

  const handleDisconnect = async (provider: string) => {
    if (!window.confirm(`${provider} 연동을 해제할까요?`)) return;
    setDisconnectBusy(provider);
    setMessage({ text: '', ok: null });
    try {
      const lowered = provider.toLowerCase() as 'discord' | 'steam' | 'blizzard' | 'riot';
      const { ok, message: errorMessage } = await unlinkAccount(lowered);
      if (ok) {
        setMessage({ text: `${provider} 연동을 해제했습니다.`, ok: true });
        await loadConnections();
      } else {
        setMessage({ text: errorMessage || '연동 해제에 실패했습니다.', ok: false });
      }
    } finally {
      setDisconnectBusy(null);
    }
  };

  function linkVisibilityPatchKey(providerKey: string): keyof ProfilePatchBody | null {
    switch (providerKey.toUpperCase()) {
      case 'DISCORD':
        return 'discordLinkVisible';
      case 'STEAM':
        return 'steamLinkVisible';
      case 'BLIZZARD':
        return 'blizzardLinkVisible';
      case 'RIOT':
        return 'riotLinkVisible';
      default:
        return null;
    }
  }

  const handleToggleLinkPublic = async (providerKey: string, visible: boolean) => {
    if (!user?.id) return;
    const patchKey = linkVisibilityPatchKey(providerKey);
    if (!patchKey) return;
    setLinkPublicBusy(providerKey);
    setMessage({ text: '', ok: null });
    try {
      await patchProfile(user.id, { [patchKey]: visible });
      setMessage({ text: visible ? '공개 프로필에 표시하도록 저장했습니다.' : '공개 프로필에서 숨기도록 저장했습니다.', ok: true });
      await loadConnections();
    } catch (e) {
      setMessage({ text: e instanceof Error ? e.message : '저장에 실패했습니다.', ok: false });
    } finally {
      setLinkPublicBusy(null);
    }
  };

  const handleRiotVisibilityPatch = async (body: Pick<ProfilePatchBody, 'riotLinkVisible' | 'riotLolRankVisible' | 'riotValorantRankVisible'>) => {
    if (!user?.id) return;
    setLinkPublicBusy('RIOT');
    setMessage({ text: '', ok: null });
    try {
      await patchProfile(user.id, body);
      setMessage({ text: '공개 설정을 저장했습니다.', ok: true });
      await loadConnections();
    } catch (e) {
      setMessage({ text: e instanceof Error ? e.message : '저장에 실패했습니다.', ok: false });
    } finally {
      setLinkPublicBusy(null);
    }
  };

  const handleRiotConnect = async () => {
    const gameName = riotGameName.trim();
    const tagLine = riotTagLine.trim();
    if (!gameName || !tagLine) {
      setMessage({ text: 'Riot 게임명과 태그를 입력해 주세요.', ok: false });
      return;
    }

    setRiotBusy(true);
    setMessage({ text: '', ok: null });
    try {
      const { ok, data, message: errorMessage } = await linkRiotAccount(gameName, tagLine);
      if (ok) {
        setMessage({ text: data?.message || 'Riot 계정을 연동했습니다.', ok: true });
        setRiotGameName('');
        setRiotTagLine('');
        await loadConnections();
      } else {
        setMessage({ text: errorMessage || 'Riot 계정 연동에 실패했습니다.', ok: false });
      }
    } finally {
      setRiotBusy(false);
    }
  };

  return (
    <>
      <h1 className="profile-page-title">외부 계정 연동</h1>
      <p className="profile-bio" style={{ marginBottom: 24 }}>
        게임 전적 검색과 프로필 신뢰도를 높이기 위해 Discord, Steam, Blizzard, Riot 계정을 연결할 수 있습니다.
      </p>

      <section className="myinfo-section">
        <h3>연동 관리</h3>
        <p className="myinfo-verify-hint">
          Discord, Steam, Blizzard는 인증 창으로 연결되고 Riot은 게임명과 태그를 직접 입력해 연동합니다.
        </p>

        <div className="account-link-grid">
          {PROVIDERS.map((provider) => {
            const connection = connections.find((entry) => entry.provider?.toUpperCase() === provider.key);
            const connected = Boolean(connection?.connected);

            return (
              <section key={provider.key} className="account-link-card">
                <div className="account-link-card-head">
                  <div>
                    <strong>{provider.title}</strong>
                    <p>{provider.description}</p>
                  </div>
                  <span className={`account-link-badge ${connected ? 'is-connected' : 'is-disconnected'}`}>
                    {connected ? '연동됨' : '미연동'}
                  </span>
                </div>

                {connected ? (
                  <div className="account-link-meta">
                    <div>{connection?.displayName || '표시 이름 없음'}</div>
                    {connection?.secondaryValue && provider.key !== 'RIOT' ? (
                      <div>{connection.secondaryValue}</div>
                    ) : null}
                    {provider.key === 'RIOT' && connection ? (
                      <RiotLinkedGameStats
                        riotDisplayName={connection.displayName}
                        lolRankSummary={connection.lolRankSummary}
                        valorantRankSummary={connection.valorantRankSummary}
                      />
                    ) : null}
                    {connection?.note ? <div>{connection.note}</div> : null}
                    <div>{connection?.ownershipVerified ? '본인 확인 완료' : '본인 확인 필요'}</div>
                    {provider.key === 'RIOT' ? (
                      <>
                        <label className="account-link-public-row account-link-public-row--riot-parent">
                          <input
                            type="checkbox"
                            checked={connection?.publicProfileVisible !== false}
                            disabled={linkPublicBusy === 'RIOT'}
                            onChange={(e) => void handleRiotVisibilityPatch({ riotLinkVisible: e.target.checked })}
                          />
                          <span>Riot 닉네임·연동 공개 프로필에 표시</span>
                        </label>
                        <div
                          className={`account-link-riot-children${
                            connection?.publicProfileVisible === false ? ' is-disabled' : ''
                          }`}
                          aria-disabled={connection?.publicProfileVisible === false}
                        >
                          <label className="account-link-public-row">
                            <input
                              type="checkbox"
                              checked={connection?.publicLolRankVisible !== false}
                              disabled={
                                linkPublicBusy === 'RIOT' || connection?.publicProfileVisible === false
                              }
                              onChange={(e) => void handleRiotVisibilityPatch({ riotLolRankVisible: e.target.checked })}
                            />
                            <span>리그 오브 레전드 랭크 정보 공개</span>
                          </label>
                          <label className="account-link-public-row">
                            <input
                              type="checkbox"
                              checked={connection?.publicValorantRankVisible !== false}
                              disabled={
                                linkPublicBusy === 'RIOT' || connection?.publicProfileVisible === false
                              }
                              onChange={(e) =>
                                void handleRiotVisibilityPatch({ riotValorantRankVisible: e.target.checked })
                              }
                            />
                            <span>발로란트 경쟁 티어 정보 공개</span>
                          </label>
                        </div>
                      </>
                    ) : (
                      <label className="account-link-public-row">
                        <input
                          type="checkbox"
                          checked={connection?.publicProfileVisible !== false}
                          disabled={linkPublicBusy === provider.key}
                          onChange={(e) => void handleToggleLinkPublic(provider.key, e.target.checked)}
                        />
                        <span>공개 프로필·프로필 조회에 이 연동 표시</span>
                      </label>
                    )}
                  </div>
                ) : provider.key === 'RIOT' ? (
                  <div className="account-link-riot-form">
                    <input
                      type="text"
                      placeholder="게임명"
                      value={riotGameName}
                      onChange={(event) => setRiotGameName(event.target.value)}
                    />
                    <input
                      type="text"
                      placeholder="태그 예: KR1"
                      value={riotTagLine}
                      onChange={(event) => setRiotTagLine(event.target.value)}
                    />
                  </div>
                ) : (
                  <div className="account-link-meta">
                    <div>아직 연결된 계정이 없습니다.</div>
                  </div>
                )}

                <div className="account-link-actions">
                  {connected ? (
                    <button
                      type="button"
                      className="account-link-secondary"
                      onClick={() => handleDisconnect(provider.key)}
                      disabled={disconnectBusy === provider.key}
                    >
                      {disconnectBusy === provider.key ? '해제 중...' : '연동 해제'}
                    </button>
                  ) : provider.oauth ? (
                    <button
                      type="button"
                      className="account-link-primary-btn"
                      onClick={() => handleOAuthConnect(provider.oauth!)}
                      disabled={oauthBusy === provider.oauth}
                    >
                      {oauthBusy === provider.oauth ? '연결 중...' : '연동 시작'}
                    </button>
                  ) : (
                    <button type="button" className="account-link-primary-btn" onClick={handleRiotConnect} disabled={riotBusy}>
                      {riotBusy ? '연동 중...' : 'Riot 연동'}
                    </button>
                  )}
                </div>
              </section>
            );
          })}
        </div>

        {loading ? <p className="myinfo-msg">연동 정보를 불러오는 중입니다.</p> : null}
        <p className={`myinfo-msg ${message.ok === true ? 'ok' : message.ok === false ? 'err' : ''}`}>{message.text}</p>
      </section>
    </>
  );
}
