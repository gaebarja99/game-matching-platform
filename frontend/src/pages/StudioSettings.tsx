import { useEffect, useState } from 'react';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface MyStream {
  id: number;
  externalUrl?: string;
}

type OverlayPosition = 'br' | 'bl' | 'tr' | 'tl';

export default function StudioSettings() {
  const [streamUrl, setStreamUrl] = useState('');
  const [streamKey, setStreamKey] = useState('');
  const [keyVisible, setKeyVisible] = useState(false);
  const [streamId, setStreamId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [reissueLoading, setReissueLoading] = useState(false);
  const [donationOverlayPosition, setDonationOverlayPosition] = useState<OverlayPosition>('br');

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

        fetch(apiUrl(`api/streams/${sid}/obs-setup`), { credentials: 'include' })
          .then((response) => (response.ok ? response.json() : null))
          .catch(() => null)
          .then((obs: { serverUrl?: string; streamKey?: string } | null) => {
            if (obs?.serverUrl) setStreamUrl(obs.serverUrl);
            if (obs?.streamKey) setStreamKey(obs.streamKey);
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

  return (
    <StudioLayout>
      <div className="settings-main">
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
                className="studio-select"
                aria-label="후원 애니메이션 표시 위치"
                value={donationOverlayPosition}
                onChange={(event) => setDonationOverlayPosition(event.target.value as OverlayPosition)}
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
              <li>후원 사운드 크기는 URL 뒤에 `&volume=0.1` 같은 값을 붙여 조절할 수 있습니다. 범위는 `0`부터 `1`까지이며, `0`은 무음입니다.</li>
            </ul>
          </div>

          <p className="hint">
            스트림 URL, 스트림 키, 오버레이 URL은 <strong>계정 기준</strong>으로 유지됩니다. 필요할 때만 스트림 키를 재발급해 주세요.
          </p>

          <div className="stream-rtmp-notice">
            <strong>OBS에서 연결이 실패하는 경우</strong>
            <p>로컬 RTMP 서버가 실행 중인지 확인한 뒤 `rtmp://localhost/live`와 스트림 키를 OBS에 입력해 주세요.</p>
          </div>
        </div>
      </div>
    </StudioLayout>
  );
}
