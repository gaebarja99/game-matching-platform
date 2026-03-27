import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import {
  confirmRiotVerification,
  fetchAccountConnections,
  linkRiotAccount,
  startOAuthLink,
  startRiotVerification,
  unlinkAccount,
  type AccountConnectionStatus,
  type RiotGameType,
  type RiotVerificationStartResponse,
} from '../api/accountLinks';

type OAuthProvider = 'discord' | 'steam' | 'blizzard';

const TEXT = {
  discordTitle: '\u0044\u0069\u0073\u0063\u006F\u0072\u0064',
  steamTitle: '\u0053\u0074\u0065\u0061\u006D',
  blizzardTitle: '\u0042\u006C\u0069\u007A\u007A\u0061\u0072\u0064',
  riotTitle: '\u0052\u0069\u006F\u0074',
  discordDesc: '\uB514\uC2A4\uCF54\uB4DC \uD504\uB85C\uD544\uACFC \uC5F0\uACB0\uD569\uB2C8\uB2E4.',
  steamDesc: '\u0053\u0074\u0065\u0061\u006D \uACC4\uC815\uC744 \uC5F0\uACB0\uD569\uB2C8\uB2E4.',
  blizzardDesc: '\u0042\u0061\u0074\u0074\u006C\u0065\u002E\u006E\u0065\u0074 \uACC4\uC815\uC744 \uC5F0\uACB0\uD569\uB2C8\uB2E4.',
  riotDesc: '\u004C\u006F\u004C \uB610\uB294 \u0056\u0061\u006C\u006F\u0072\u0061\u006E\u0074 \uACC4\uC815\uC744 \uC778\uC99D \uD6C4 \uC5F0\uACB0\uD569\uB2C8\uB2E4.',
  pageTitle: '\uC678\uBD80 \uACC4\uC815 \uC5F0\uB3D9',
  pageBio:
    '\uAC8C\uC784 \uC804\uC801 \uAC80\uC0C9\uACFC \uD504\uB85C\uD544 \uC2E0\uB8B0\uB3C4\uB97C \uB192\uC774\uAE30 \uC704\uD574 Discord, Steam, Blizzard, Riot \uACC4\uC815\uC744 \uC5F0\uACB0\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.',
  sectionTitle: '\uC5F0\uB3D9 \uAD00\uB9AC',
  sectionHint:
    '\u0044\u0069\u0073\u0063\u006F\u0072\u0064, \u0053\u0074\u0065\u0061\u006D, \u0042\u006C\u0069\u007A\u007A\u0061\u0072\u0064\uB294 \uC778\uC99D \uCC3D\uC73C\uB85C \uC5F0\uACB0\uB418\uACE0 Riot\uC740 \uAC8C\uC784 \uC548\uC5D0\uC11C \uC18C\uC720\uAD8C\uC744 \uD655\uC778\uD55C \uB4A4 \uC5F0\uB3D9\uB429\uB2C8\uB2E4.',
  loadError: '\uC5F0\uB3D9 \uC815\uBCF4\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.',
  linkDone: '\uACC4\uC815 \uC5F0\uB3D9\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.',
  linkFail: '\uACC4\uC815 \uC5F0\uB3D9\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.',
  openFail: '\uC5F0\uB3D9 \uD398\uC774\uC9C0\uB97C \uC5F4\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.',
  disconnectAsk: '\uC5F0\uB3D9\uC744 \uD574\uC81C\uD560\uAE4C\uC694?',
  disconnectDone: '\uC5F0\uB3D9\uC744 \uD574\uC81C\uD588\uC2B5\uB2C8\uB2E4.',
  disconnectFail: '\uC5F0\uB3D9 \uD574\uC81C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.',
  riotNeedInput: '\uAC8C\uC784\uBA85\uACFC \uD0DC\uADF8\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.',
  loginNeeded: '\uB85C\uADF8\uC778 \uD6C4 \uC774\uC6A9\uD574 \uC8FC\uC138\uC694.',
  riotStartFail: 'Riot \uC778\uC99D\uC744 \uC2DC\uC791\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.',
  riotStartDone: '\uC778\uC99D \uC694\uCCAD\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.',
  riotConfirmFail: 'Riot \uC778\uC99D \uD655\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.',
  riotLinkDone: 'Riot \uACC4\uC815 \uC5F0\uB3D9\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.',
  riotManualLink: 'LoL \uC778\uC99D \uC5C6\uC774 \uC5F0\uB3D9',
  riotManualHint:
    'LoL \uC18C\uD658\uC0AC \uC870\uD68C\uAC00 \uACC4\uC18D \uC2E4\uD328\uD558\uBA74 \uC784\uC2DC\uB85C \uC218\uB3D9 \uC5F0\uB3D9\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4. \uB2E4\uB9CC \uBCF8\uC778 \uC778\uC99D \uC644\uB8CC \uC0C1\uD0DC\uB294 \uC544\uB2D9\uB2C8\uB2E4.',
  riotGuide: 'Riot \uC778\uC99D',
  riotGuideFallback:
    '\uC548\uB0B4\uC5D0 \uB530\uB77C \uAC8C\uC784 \uC548\uC5D0\uC11C \uC124\uC815\uC744 \uBC14\uAFBC \uB4A4 \uC778\uC99D \uC644\uB8CC\uB97C \uB20C\uB7EC \uC8FC\uC138\uC694.',
  verifyCode: '\uC778\uC99D \uCF54\uB4DC',
  currentCardId: '\uAE30\uC900 \uCE74\uB4DC',
  currentCardAlt: '\uD604\uC7AC \uD50C\uB808\uC774\uC5B4 \uCE74\uB4DC',
  connected: '\uC5F0\uB3D9\uB428',
  notConnected: '\uBBF8\uC5F0\uB3D9',
  noDisplayName: '\uD45C\uC2DC \uC774\uB984 \uC5C6\uC74C',
  verified: '\uBCF8\uC778 \uD655\uC778 \uC644\uB8CC',
  verifyNeeded: '\uBCF8\uC778 \uD655\uC778 \uD544\uC694',
  noLinkedAccount: '\uC544\uC9C1 \uC5F0\uACB0\uB41C \uACC4\uC815\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.',
  gameNamePlaceholder: '\uAC8C\uC784\uBA85',
  tagPlaceholder: '\uD0DC\uADF8 \uC608: KR1',
  disconnecting: '\uD574\uC81C \uC911...',
  disconnect: '\uC5F0\uB3D9 \uD574\uC81C',
  connecting: '\uC5F0\uACB0 \uC911...',
  startLink: '\uC5F0\uB3D9 \uC2DC\uC791',
  processing: '\uCC98\uB9AC \uC911...',
  verifyDone: '\uC778\uC99D \uC644\uB8CC',
  startRiot: 'Riot \uC778\uC99D \uC2DC\uC791',
  openRiot: 'Riot \uC5F0\uB3D9\uD558\uAE30',
  closeRiot: '\uC785\uB825 \uB2EB\uAE30',
  loading: '\uC5F0\uB3D9 \uC815\uBCF4\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4.',
} as const;

const PROVIDERS: Array<{
  key: 'DISCORD' | 'STEAM' | 'BLIZZARD' | 'RIOT';
  title: string;
  description: string;
  oauth?: OAuthProvider;
}> = [
  { key: 'DISCORD', title: TEXT.discordTitle, description: TEXT.discordDesc, oauth: 'discord' },
  { key: 'STEAM', title: TEXT.steamTitle, description: TEXT.steamDesc, oauth: 'steam' },
  { key: 'BLIZZARD', title: TEXT.blizzardTitle, description: TEXT.blizzardDesc, oauth: 'blizzard' },
  { key: 'RIOT', title: TEXT.riotTitle, description: TEXT.riotDesc },
];

const RIOT_GAME_OPTIONS: Array<{ value: RiotGameType; label: string }> = [
  { value: 'lol', label: 'League of Legends' },
  { value: 'valorant', label: 'Valorant' },
];

export default function ProfileAccountLinks() {
  const { user } = useAuth();
  const [connections, setConnections] = useState<AccountConnectionStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ text: string; ok: boolean | null }>({ text: '', ok: null });
  const [oauthBusy, setOauthBusy] = useState<OAuthProvider | null>(null);
  const [disconnectBusy, setDisconnectBusy] = useState<string | null>(null);
  const [riotGameType, setRiotGameType] = useState<RiotGameType>('lol');
  const [riotGameName, setRiotGameName] = useState('');
  const [riotTagLine, setRiotTagLine] = useState('');
  const [riotBusy, setRiotBusy] = useState(false);
  const [riotVerification, setRiotVerification] = useState<RiotVerificationStartResponse | null>(null);
  const [riotFormOpen, setRiotFormOpen] = useState(false);

  const resetRiotVerificationState = () => {
    setRiotVerification(null);
    setMessage({ text: '', ok: null });
  };

  const loadConnections = async () => {
    setLoading(true);
    try {
      const { ok, data, message: errorMessage } = await fetchAccountConnections();
      if (ok && data?.connections) {
        setConnections(data.connections);
        setMessage({ text: '', ok: null });
      } else {
        setMessage({ text: errorMessage || TEXT.loadError, ok: false });
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!user) return;
    loadConnections().catch(() => {
      setLoading(false);
      setMessage({ text: TEXT.loadError, ok: false });
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
        text: payload.message || (success ? TEXT.linkDone : TEXT.linkFail),
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
        setMessage({ text: errorMessage || TEXT.openFail, ok: false });
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
      setMessage({ text: TEXT.openFail, ok: false });
      setOauthBusy(null);
    }
  };

  const handleDisconnect = async (provider: string) => {
    if (!window.confirm(`${provider} ${TEXT.disconnectAsk}`)) return;
    setDisconnectBusy(provider);
    setMessage({ text: '', ok: null });
    try {
      const lowered = provider.toLowerCase() as 'discord' | 'steam' | 'blizzard' | 'riot';
      const { ok, message: errorMessage } = await unlinkAccount(lowered);
      if (ok) {
        setMessage({ text: `${provider} ${TEXT.disconnectDone}`, ok: true });
        if (provider === 'RIOT') {
          setRiotVerification(null);
        }
        await loadConnections();
      } else {
        setMessage({ text: errorMessage || TEXT.disconnectFail, ok: false });
      }
    } finally {
      setDisconnectBusy(null);
    }
  };

  const handleStartRiotVerification = async () => {
    if (!user) {
      setMessage({ text: TEXT.loginNeeded, ok: false });
      return;
    }

    const gameName = riotGameName.trim();
    const tagLine = riotTagLine.trim();
    if (!gameName || !tagLine) {
      setMessage({ text: TEXT.riotNeedInput, ok: false });
      return;
    }

    setRiotBusy(true);
    setMessage({ text: '', ok: null });
    try {
      const requestPlatform = 'kr';
      const { ok, data, message: errorMessage } = await startRiotVerification(riotGameType, gameName, tagLine, requestPlatform);
      if (!ok || !data) {
        setMessage({ text: errorMessage || TEXT.riotStartFail, ok: false });
        return;
      }

      setRiotVerification(data);
      setMessage({ text: TEXT.riotStartDone, ok: true });
    } finally {
      setRiotBusy(false);
    }
  };

  const handleConfirmRiotVerification = async () => {
    if (!user) {
      setMessage({ text: TEXT.loginNeeded, ok: false });
      return;
    }

    if (!riotVerification?.verificationId) return;

    setRiotBusy(true);
    setMessage({ text: '', ok: null });
    try {
      const { ok, data, message: errorMessage } = await confirmRiotVerification(riotVerification.verificationId);
      if (ok) {
        setMessage({ text: data?.message || TEXT.riotLinkDone, ok: true });
        setRiotGameName('');
        setRiotTagLine('');
        setRiotGameType('lol');
        setRiotVerification(null);
        setRiotFormOpen(false);
        await loadConnections();
      } else {
        setMessage({ text: errorMessage || TEXT.riotConfirmFail, ok: false });
      }
    } finally {
      setRiotBusy(false);
    }
  };

  const handleManualRiotLink = async () => {
    if (!user) {
      setMessage({ text: TEXT.loginNeeded, ok: false });
      return;
    }

    const gameName = riotGameName.trim();
    const tagLine = riotTagLine.trim();
    if (!gameName || !tagLine) {
      setMessage({ text: TEXT.riotNeedInput, ok: false });
      return;
    }

    setRiotBusy(true);
    setMessage({ text: '', ok: null });
    try {
      const { ok, data, message: errorMessage } = await linkRiotAccount(gameName, tagLine);
      if (ok) {
        setMessage({ text: data?.message || TEXT.riotLinkDone, ok: true });
        setRiotVerification(null);
        setRiotFormOpen(false);
        setRiotGameName('');
        setRiotTagLine('');
        await loadConnections();
      } else {
        setMessage({ text: errorMessage || TEXT.riotStartFail, ok: false });
      }
    } finally {
      setRiotBusy(false);
    }
  };

  const renderRiotVerificationPanel = () => {
    if (!riotVerification) return null;

    return (
      <div
        className="account-link-meta"
        style={{ marginTop: 12, padding: 14, border: '1px solid rgba(255,255,255,0.12)', borderRadius: 12 }}
      >
        <strong>{riotVerification.instructionTitle || TEXT.riotGuide}</strong>
        <div style={{ whiteSpace: 'pre-line', lineHeight: 1.6 }}>
          {riotVerification.instructionBody || TEXT.riotGuideFallback}
        </div>
        {riotVerification.verificationMethod === 'LOL_THIRD_PARTY_CODE' && riotVerification.verificationCode ? (
          <div style={{ fontWeight: 700, fontSize: 18, letterSpacing: 1, marginTop: 8 }}>
            {TEXT.verifyCode}: {riotVerification.verificationCode}
          </div>
        ) : null}
        {riotVerification.verificationMethod === 'VALORANT_CARD_SWAP' ? (
          <>
            <div style={{ marginTop: 8 }}>
              {TEXT.currentCardId}: {riotVerification.currentCardId || '-'}
            </div>
            {riotVerification.currentCardImageUrl ? (
              <img
                src={riotVerification.currentCardImageUrl}
                alt={TEXT.currentCardAlt}
                style={{ width: '100%', maxWidth: 220, borderRadius: 12, marginTop: 10 }}
              />
            ) : null}
          </>
        ) : null}
      </div>
    );
  };

  return (
    <>
      <h1 className="profile-page-title">{TEXT.pageTitle}</h1>
      <p className="profile-bio" style={{ marginBottom: 24 }}>
        {TEXT.pageBio}
      </p>

      <section className="myinfo-section">
        <h3>{TEXT.sectionTitle}</h3>
        <p className="myinfo-verify-hint">{TEXT.sectionHint}</p>

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
                    {connected ? TEXT.connected : TEXT.notConnected}
                  </span>
                </div>

                {connected ? (
                  <div className="account-link-meta">
                    <div>{connection?.displayName || TEXT.noDisplayName}</div>
                    <div>{connection?.ownershipVerified ? TEXT.verified : TEXT.verifyNeeded}</div>
                    {connection?.note ? <div>{connection.note}</div> : null}
                  </div>
                ) : provider.key === 'RIOT' ? (
                  riotFormOpen ? (
                    <div className="account-link-riot-form">
                    <select
                      value={riotGameType}
                      onChange={(event) => {
                        const nextGameType = event.target.value as RiotGameType;
                        setRiotGameType(nextGameType);
                        resetRiotVerificationState();
                      }}
                    >
                      {RIOT_GAME_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <input
                      type="text"
                      placeholder={TEXT.gameNamePlaceholder}
                      value={riotGameName}
                      onChange={(event) => {
                        setRiotGameName(event.target.value);
                        resetRiotVerificationState();
                      }}
                    />
                    <input
                      type="text"
                      placeholder={TEXT.tagPlaceholder}
                      value={riotTagLine}
                      onChange={(event) => {
                        setRiotTagLine(event.target.value);
                        resetRiotVerificationState();
                      }}
                    />
                    {renderRiotVerificationPanel()}
                    {riotGameType === 'lol' && !riotVerification ? (
                      <div className="account-link-meta" style={{ marginTop: 10 }}>
                        <div>{TEXT.riotManualHint}</div>
                      </div>
                    ) : null}
                  </div>
                  ) : (
                    <div className="account-link-meta">
                      <div>{TEXT.noLinkedAccount}</div>
                    </div>
                  )
                ) : (
                  <div className="account-link-meta">
                    <div>{TEXT.noLinkedAccount}</div>
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
                      {disconnectBusy === provider.key ? TEXT.disconnecting : TEXT.disconnect}
                    </button>
                  ) : provider.oauth ? (
                    <button
                      type="button"
                      className="btn-myinfo-save"
                      onClick={() => handleOAuthConnect(provider.oauth!)}
                      disabled={oauthBusy === provider.oauth}
                    >
                      {oauthBusy === provider.oauth ? TEXT.connecting : TEXT.startLink}
                    </button>
                  ) : (
                    provider.key === 'RIOT' && !riotFormOpen ? (
                      <button
                        type="button"
                        className="btn-myinfo-save"
                        onClick={() => {
                          setMessage({ text: '', ok: null });
                          setRiotFormOpen(true);
                        }}
                      >
                        {TEXT.openRiot}
                      </button>
                    ) : (
                      <div style={{ display: 'flex', gap: 8, width: '100%' }}>
                        <button
                          type="button"
                          className="btn-myinfo-save"
                          onClick={riotVerification ? handleConfirmRiotVerification : handleStartRiotVerification}
                          disabled={riotBusy}
                          style={{ flex: 1 }}
                        >
                          {riotBusy ? TEXT.processing : riotVerification ? TEXT.verifyDone : TEXT.startRiot}
                        </button>
                          {provider.key === 'RIOT' && riotGameType === 'lol' ? (
                          <button
                            type="button"
                            className="account-link-secondary"
                            onClick={handleManualRiotLink}
                            disabled={riotBusy || riotVerification != null}
                            style={{ flex: 1 }}
                          >
                            {TEXT.riotManualLink}
                          </button>
                        ) : null}
                        {provider.key === 'RIOT' ? (
                          <button
                            type="button"
                            className="account-link-secondary"
                            onClick={() => {
                              setRiotVerification(null);
                              setRiotFormOpen(false);
                              setMessage({ text: '', ok: null });
                            }}
                            disabled={riotBusy}
                            style={{ flex: 1 }}
                          >
                            {TEXT.closeRiot}
                          </button>
                        ) : null}
                      </div>
                    )
                  )}
                </div>
              </section>
            );
          })}
        </div>

        {loading ? <p className="myinfo-msg">{TEXT.loading}</p> : null}
        <p className={`myinfo-msg ${message.ok === true ? 'ok' : message.ok === false ? 'err' : ''}`}>{message.text}</p>
      </section>
    </>
  );
}
