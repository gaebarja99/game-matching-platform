import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface MyStream {
  id: number;
  externalUrl?: string;
}

type OverlayPosition = 'br' | 'bl' | 'tr' | 'tl';
type ChatPermissionScope = 'ALL' | 'FOLLOWER' | 'MANAGER';

interface ChatSettingsResponse {
  minVideoPang?: number;
  minTtsPang?: number;
  chatPermissionScope?: ChatPermissionScope;
  slowModeEnabled?: boolean;
  slowModeSeconds?: number;
  chatRules?: string;
}

export default function StudioSettings() {
  const [streamUrl, setStreamUrl] = useState('');
  const [streamKey, setStreamKey] = useState('');
  const [keyVisible, setKeyVisible] = useState(false);
  const [streamId, setStreamId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [reissueLoading, setReissueLoading] = useState(false);
  const [chatScope, setChatScope] = useState<ChatPermissionScope>('ALL');
  const [slowOn, setSlowOn] = useState(false);
  const [chatRules, setChatRules] = useState('');
  const [donationOverlayPosition, setDonationOverlayPosition] = useState<OverlayPosition>('br');
  const [minVideoPang, setMinVideoPang] = useState(0);
  const [minTtsPang, setMinTtsPang] = useState(0);
  const [savingDonationLimits, setSavingDonationLimits] = useState(false);
  const [savingChatPreferences, setSavingChatPreferences] = useState(false);
  const [savingChatRules, setSavingChatRules] = useState(false);
  const [updatingAll, setUpdatingAll] = useState(false);

  const backendOrigin = apiUrl('').replace(/\/$/, '');
  const overlayChatUrl = streamId ? `${backendOrigin}/chat-widget.html?streamId=${streamId}&overlay=1` : '';
  const donationOverlayUrl = streamId
    ? `${backendOrigin}/donation-overlay.html?streamId=${streamId}&position=${donationOverlayPosition}`
    : '';

  useEffect(() => {
    fetch(apiUrl('api/streams/by-user/me'), { credentials: 'include' })
      .then((response) => (response.ok ? response.json() : []))
      .then((list: MyStream[]) => {
        const streams = Array.isArray(list) ? list : [];
        const obsStream = streams.find((item) => !item.externalUrl);
        const sid = obsStream?.id ?? streams[0]?.id ?? null;
        setStreamId(sid);
        if (!sid) {
          setLoading(false);
          return;
        }

        Promise.all([
          fetch(apiUrl(`api/streams/${sid}/obs-setup`), { credentials: 'include' })
            .then((response) => (response.ok ? response.json() : null))
            .catch(() => null),
          fetch(apiUrl(`api/streams/${sid}/chat-settings`), { credentials: 'include' })
            .then((response) => (response.ok ? response.json() : null))
            .catch(() => null),
        ])
          .then(([obs, chatSettings]: [{ serverUrl?: string; streamKey?: string } | null, ChatSettingsResponse | null]) => {
            if (obs?.serverUrl) setStreamUrl(obs.serverUrl);
            if (obs?.streamKey) setStreamKey(obs.streamKey);
            setMinVideoPang(chatSettings?.minVideoPang ?? 0);
            setMinTtsPang(chatSettings?.minTtsPang ?? 0);
            setChatScope(chatSettings?.chatPermissionScope ?? 'ALL');
            setSlowOn(!!chatSettings?.slowModeEnabled);
            setChatRules(chatSettings?.chatRules ?? '');
          })
          .finally(() => setLoading(false));
      })
      .catch(() => setLoading(false));
  }, []);

  const copyText = (text: string, label: string) => {
    if (!text || !navigator.clipboard?.writeText) return;
    navigator.clipboard.writeText(text);
    window.alert(`${label}이(가) 복사되었습니다.`);
  };

  const handleReissueStreamKey = () => {
    if (!streamId) return;
    setReissueLoading(true);
    fetch(apiUrl(`api/streams/${streamId}/regenerate-stream-key`), {
      method: 'POST',
      credentials: 'include',
    })
      .then((response) => response.json().then((data: { streamKey?: string; message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok && result.data?.streamKey) {
          setStreamKey(result.data.streamKey);
          window.alert('스트림 키가 재발급되었습니다. OBS에도 새 키를 다시 입력해 주세요.');
        } else {
          window.alert(result.data?.message ?? '재발급에 실패했습니다.');
        }
      })
      .catch(() => window.alert('재발급 요청에 실패했습니다.'))
      .finally(() => setReissueLoading(false));
  };

  const saveDonationLimits = () => {
    if (!streamId) return;
    setSavingDonationLimits(true);
    fetch(apiUrl(`api/streams/${streamId}/chat-settings/donation-limits`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        minVideoPang: Math.max(0, minVideoPang || 0),
        minTtsPang: Math.max(0, minTtsPang || 0),
      }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          window.alert('후원 제한이 저장되었습니다.');
        } else {
          window.alert(result.data?.message ?? '저장에 실패했습니다.');
        }
      })
      .catch(() => window.alert('저장에 실패했습니다.'))
      .finally(() => setSavingDonationLimits(false));
  };

  const saveChatPreferences = () => {
    if (!streamId) return;
    setSavingChatPreferences(true);
    fetch(apiUrl(`api/streams/${streamId}/chat-settings/preferences`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        chatPermissionScope: chatScope,
        slowModeEnabled: slowOn,
        slowModeSeconds: 5,
      }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          window.alert('채팅 참여 설정이 저장되었습니다.');
        } else {
          window.alert(result.data?.message ?? '저장에 실패했습니다.');
        }
      })
      .catch(() => window.alert('저장에 실패했습니다.'))
      .finally(() => setSavingChatPreferences(false));
  };

  const saveChatRules = () => {
    if (!streamId) return;
    setSavingChatRules(true);
    fetch(apiUrl(`api/streams/${streamId}/chat-settings/rules`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        chatRules,
      }),
    })
      .then((response) => response.json().then((data: { message?: string; chatRules?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          setChatRules(result.data.chatRules ?? chatRules);
          window.alert('채팅 규칙이 저장되었습니다.');
        } else {
          window.alert(result.data?.message ?? '저장에 실패했습니다.');
        }
      })
      .catch(() => window.alert('저장에 실패했습니다.'))
      .finally(() => setSavingChatRules(false));
  };

  const updateAllSettings = async () => {
    if (!streamId || updatingAll) return;
    setUpdatingAll(true);
    try {
      const donationResponse = await fetch(apiUrl(`api/streams/${streamId}/chat-settings/donation-limits`), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          minVideoPang: Math.max(0, minVideoPang || 0),
          minTtsPang: Math.max(0, minTtsPang || 0),
        }),
      });
      const donationData = await donationResponse.json().catch(() => ({} as { message?: string }));
      if (!donationResponse.ok) {
        window.alert(donationData.message ?? '후원 설정 업데이트에 실패했습니다.');
        return;
      }

      const preferencesResponse = await fetch(apiUrl(`api/streams/${streamId}/chat-settings/preferences`), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          chatPermissionScope: chatScope,
          slowModeEnabled: slowOn,
          slowModeSeconds: 5,
        }),
      });
      const preferencesData = await preferencesResponse.json().catch(() => ({} as { message?: string }));
      if (!preferencesResponse.ok) {
        window.alert(preferencesData.message ?? '채팅 설정 업데이트에 실패했습니다.');
        return;
      }

      const rulesResponse = await fetch(apiUrl(`api/streams/${streamId}/chat-settings/rules`), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          chatRules,
        }),
      });
      const rulesData = await rulesResponse.json().catch(() => ({} as { message?: string; chatRules?: string }));
      if (!rulesResponse.ok) {
        window.alert(rulesData.message ?? '채팅 규칙 업데이트에 실패했습니다.');
        return;
      }

      setChatRules(rulesData.chatRules ?? chatRules);
      window.alert('설정이 업데이트되었습니다.');
    } catch {
      window.alert('업데이트에 실패했습니다.');
    } finally {
      setUpdatingAll(false);
    }
  };

  return (
    <StudioLayout>
      <div className="settings-main">
        <div className="settings-update-bar">
          <button type="button" className="btn-update" onClick={() => void updateAllSettings()} disabled={!streamId || updatingAll}>
            {updatingAll ? '업데이트 중...' : '업데이트'}
          </button>
        </div>

        <div className="settings-card">
          <h2>스트림 설정</h2>

          <div className="settings-row">
            <label className="label">스트림 URL</label>
            <div className="input-row">
              <input
                type="text"
                className="readonly"
                value={loading ? '불러오는 중...' : streamUrl}
                placeholder="방송을 등록하면 스트림 URL이 발급됩니다."
                readOnly
              />
              <button type="button" className="btn-copy" onClick={() => copyText(streamUrl, '스트림 URL')} disabled={!streamUrl}>
                복사
              </button>
            </div>
          </div>

          <div className="settings-row">
            <label className="label">스트림 키</label>
            <div className="input-row">
              <input
                type={keyVisible ? 'text' : 'password'}
                className="readonly"
                value={streamKey}
                placeholder="방송을 등록하면 스트림 키가 발급됩니다."
                readOnly
                style={{
                  flex: 1,
                  minWidth: 200,
                  padding: '10px 14px',
                  borderRadius: 8,
                  border: '1px solid var(--studio-border-strong)',
                  background: 'var(--studio-bg-card)',
                  color: 'var(--studio-text)',
                  fontSize: '0.9rem',
                }}
              />
              <button type="button" className="btn-eye" title="표시/숨기기" onClick={() => setKeyVisible((visible) => !visible)}>
                {keyVisible ? '숨김' : '표시'}
              </button>
              <button type="button" className="btn-copy" onClick={() => copyText(streamKey, '스트림 키')} disabled={!streamKey}>
                복사
              </button>
              <button type="button" className="btn-regen" onClick={handleReissueStreamKey} disabled={!streamId || reissueLoading}>
                {reissueLoading ? '처리 중...' : '재발급'}
              </button>
            </div>
          </div>

          <div className="settings-row">
            <label className="label">오버레이 채팅창 URL</label>
            <div className="input-row">
              <input
                type="text"
                className="readonly"
                value={overlayChatUrl}
                placeholder="방송을 등록하면 오버레이 URL이 발급됩니다."
                readOnly
                style={{
                  flex: 1,
                  minWidth: 200,
                  padding: '10px 14px',
                  borderRadius: 8,
                  border: '1px solid var(--studio-border-strong)',
                  background: 'var(--studio-bg-card)',
                  color: 'var(--studio-text)',
                  fontSize: '0.9rem',
                }}
              />
              <button type="button" className="btn-copy" onClick={() => copyText(overlayChatUrl, '오버레이 채팅창 URL')} disabled={!overlayChatUrl}>
                복사
              </button>
            </div>
            <p className="hint">OBS 브라우저 소스에 넣어 방송 화면에 채팅을 오버레이할 때 사용하는 URL입니다.</p>
          </div>

          <div className="settings-row">
            <label className="label">후원 애니메이션 URL</label>
            <div className="input-row" style={{ flexWrap: 'wrap', gap: 8 }}>
              <select
                value={donationOverlayPosition}
                onChange={(event) => setDonationOverlayPosition(event.target.value as OverlayPosition)}
                style={{ minWidth: 120 }}
              >
                <option value="br">하단 우 (br)</option>
                <option value="bl">하단 좌 (bl)</option>
                <option value="tr">상단 우 (tr)</option>
                <option value="tl">상단 좌 (tl)</option>
              </select>
              <input
                type="text"
                className="readonly"
                value={donationOverlayUrl}
                placeholder="후원 오버레이 URL"
                readOnly
                style={{
                  flex: 1,
                  minWidth: 220,
                  padding: '10px 14px',
                  borderRadius: 8,
                  border: '1px solid var(--studio-border-strong)',
                  background: 'var(--studio-bg-card)',
                  color: 'var(--studio-text)',
                  fontSize: '0.9rem',
                }}
              />
              <button type="button" className="btn-copy" onClick={() => copyText(donationOverlayUrl, '후원 애니메이션 URL')} disabled={!donationOverlayUrl}>
                복사
              </button>
            </div>
            <p className="hint">시청 화면에는 후원 애니메이션이 표시되지 않고, OBS 브라우저 소스에서만 동작합니다.</p>
            <ul className="chat-widget-hints">
              <li>오버레이 URL은 방송을 다시 켜도 그대로 사용할 수 있습니다.</li>
              <li>브라우저 소스 너비는 520px 기준으로 맞추는 것을 권장합니다.</li>
              <li>필요하면 URL의 `position=` 값을 바꿔 위치를 바로 조절할 수 있습니다.</li>
            </ul>
          </div>

          <div className="settings-row">
            <label className="label">후원 팡 제한</label>
            <div className="input-row" style={{ alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span>영상 후원 최소</span>
                <input type="number" min={0} value={minVideoPang} onChange={(event) => setMinVideoPang(Number(event.target.value) || 0)} style={{ width: 120 }} />
                <span>팡</span>
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span>TTS 최소</span>
                <input type="number" min={0} value={minTtsPang} onChange={(event) => setMinTtsPang(Number(event.target.value) || 0)} style={{ width: 120 }} />
                <span>팡</span>
              </label>
              <button type="button" className="btn-save" onClick={saveDonationLimits} disabled={!streamId || savingDonationLimits}>
                {savingDonationLimits ? '저장 중...' : '저장'}
              </button>
            </div>
            <p className="hint">0이면 제한이 없고, 설정한 값 이상일 때만 영상 후원과 TTS가 허용됩니다.</p>
          </div>

          <p className="hint">
            스트림 URL, 스트림 키, 오버레이 URL은 <strong>계정 기준</strong>으로 유지됩니다. 필요할 때만 스트림 키를 재발급해 주세요.
          </p>

          <div className="stream-rtmp-notice">
            <strong>OBS에서 연결이 실패하는 경우</strong>
            <p>로컬 RTMP 서버가 실행 중인지 확인한 뒤 `rtmp://localhost/live`와 스트림 키를 OBS에 입력해 주세요.</p>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 참여자 설정</h2>
          <div className="settings-row">
            <label className="label">채팅 참여 범위</label>
            <div className="radio-group">
              <label><input type="radio" name="chat-scope" value="ALL" checked={chatScope === 'ALL'} onChange={() => setChatScope('ALL')} /> 모든 시청자</label>
              <label><input type="radio" name="chat-scope" value="FOLLOWER" checked={chatScope === 'FOLLOWER'} onChange={() => setChatScope('FOLLOWER')} /> 팔로워 전용</label>
              <label><input type="radio" name="chat-scope" value="MANAGER" checked={chatScope === 'MANAGER'} onChange={() => setChatScope('MANAGER')} /> 운영자 전용</label>
            </div>
          </div>
          <div className="settings-row">
            <button type="button" className="btn-save" onClick={saveChatPreferences} disabled={!streamId || savingChatPreferences}>
              {savingChatPreferences ? '저장 중...' : '참여 설정 저장'}
            </button>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 모드 설정</h2>
          <div className="settings-row">
            <label className="label">클린봇</label>
            <div className="banned-row">
              <span className="status">켜짐</span>
              <span className="hint">욕설 필터와 금칙어 관리를 함께 사용합니다.</span>
            </div>
          </div>
          <div className="toggle-row">
            <div className="label-wrap">
              <span className="label">저속 모드</span>
              <span className="help" title="시청자는 5초에 한 번만 채팅할 수 있습니다.">?</span>
            </div>
            <button
              type="button"
              className={`toggle-switch ${slowOn ? 'on' : ''}`}
              role="switch"
              aria-pressed={slowOn}
              onClick={() => setSlowOn((value) => !value)}
            >
              <span className="knob" />
            </button>
          </div>
          <p className="hint">저속 모드를 켜면 한 사용자는 5초에 한 번만 채팅할 수 있습니다.</p>
          <div className="settings-row">
            <button type="button" className="btn-save" onClick={saveChatPreferences} disabled={!streamId || savingChatPreferences}>
              {savingChatPreferences ? '저장 중...' : '모드 저장'}
            </button>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 규칙 설정</h2>
          <div className="settings-row">
            <label className="label">채팅 규칙</label>
            <div className="textarea-wrap">
              <textarea value={chatRules} onChange={(event) => setChatRules(event.target.value)} placeholder="채팅 규칙은 한 줄씩 입력해 주세요." />
              <button type="button" className="btn-save" onClick={saveChatRules} disabled={!streamId || savingChatRules}>
                {savingChatRules ? '저장 중...' : '저장'}
              </button>
            </div>
            <p className="hint">규칙은 시청자에게 안내 문구로 활용할 수 있고, 변경 시 다시 확인받는 기준으로 쓸 수 있습니다.</p>
          </div>
          <div className="settings-row">
            <label className="label">채팅 금칙어 설정</label>
            <div className="banned-row">
              <span className="status">금칙어 관리는 별도 화면에서 설정합니다.</span>
              <Link to="/studio/chat" className="btn-manage">금칙어 관리</Link>
            </div>
          </div>
          <div className="settings-row">
            <label className="label">활동 제한 해제 요청</label>
            <div className="banned-row">
              <span className="status">0건</span>
              <span className="hint">차단/제한 관리 화면과 연동 예정입니다.</span>
            </div>
          </div>
          <div className="settings-row">
            <label className="label">주간 후원 랭킹</label>
            <div className="banned-row">
              <span className="status">노출</span>
              <span className="hint">시청 페이지 채팅 영역에 주간 후원 랭킹이 표시됩니다.</span>
            </div>
          </div>
        </div>
      </div>
    </StudioLayout>
  );
}
