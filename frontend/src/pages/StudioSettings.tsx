import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

interface MyStream {
  id: number;
  externalUrl?: string;
}

export default function StudioSettings() {
  const [streamUrl, setStreamUrl] = useState('');
  const [streamKey, setStreamKey] = useState('');
  const [keyVisible, setKeyVisible] = useState(false);
  const [streamId, setStreamId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [reissueLoading, setReissueLoading] = useState(false);
  const [chatScope, setChatScope] = useState('all');
  const [emoticonOn, setEmoticonOn] = useState(false);
  const [slowOn, setSlowOn] = useState(false);
  const [chatRules, setChatRules] = useState('');

  const backendOrigin = apiUrl('').replace(/\/$/, '');
  const overlayChatUrl = streamId ? `${backendOrigin}/chat-widget.html?streamId=${streamId}&overlay=1` : '';

  useEffect(() => {
    fetch(apiUrl('api/streams/by-user/me'), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : []))
      .then((list: MyStream[]) => {
        const streams = Array.isArray(list) ? list : [];
        const obsStream = streams.find((s) => !s.externalUrl);
        const sid = obsStream?.id ?? streams[0]?.id ?? null;
        setStreamId(sid);
        if (!sid) {
          setLoading(false);
          return;
        }
        fetch(apiUrl(`api/streams/${sid}/obs-setup`), { credentials: 'include' })
          .then((res) => {
            if (!res.ok) return null;
            return res.json();
          })
          .then((obs: { serverUrl?: string; streamKey?: string } | null) => {
            if (obs?.serverUrl) setStreamUrl(obs.serverUrl);
            if (obs?.streamKey) setStreamKey(obs.streamKey);
          })
          .catch(() => {})
          .finally(() => setLoading(false));
      })
      .catch(() => setLoading(false));
  }, []);

  const copyUrl = () => {
    if (streamUrl && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(streamUrl);
      alert('스트림 URL이 복사되었습니다.');
    }
  };
  const copyKey = () => {
    if (streamKey && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(streamKey);
      alert('스트림 키가 복사되었습니다.');
    }
  };
  const copyOverlayChatUrl = () => {
    if (overlayChatUrl && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(overlayChatUrl);
      alert('오버레이 채팅창 URL이 복사되었습니다.');
    }
  };

  const handleReissueStreamKey = () => {
    if (!streamId) return;
    setReissueLoading(true);
    fetch(apiUrl(`api/streams/${streamId}/regenerate-stream-key`), {
      method: 'POST',
      credentials: 'include',
    })
      .then((r) => r.json().then((d: { streamKey?: string; message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok && res.data?.streamKey) {
          setStreamKey(res.data.streamKey);
          alert('스트림 키가 재발급되었습니다. OBS에 새 키를 입력하세요.');
        } else {
          alert(res.data?.message ?? '재발급에 실패했습니다.');
        }
      })
      .catch(() => alert('재발급 요청에 실패했습니다.'))
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
                placeholder="방송 관리 → 방송하기에서 OBS 방송을 등록하면 발급됩니다"
                readOnly
              />
              <button type="button" className="btn-copy" onClick={copyUrl} disabled={!streamUrl}>
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
                placeholder="방송 관리 → 방송하기에서 OBS 방송을 등록하면 발급됩니다"
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
                readOnly
              />
              <button type="button" className="btn-eye" title="표시/숨기기" onClick={() => setKeyVisible((v) => !v)}>
                {keyVisible ? '🙈' : '👁'}
              </button>
              <button type="button" className="btn-copy" onClick={copyKey} disabled={!streamKey}>
                복사
              </button>
              <button
                type="button"
                className="btn-regen"
                onClick={handleReissueStreamKey}
                disabled={!streamId || reissueLoading}
              >
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
                placeholder="방송을 등록하면 오버레이용 URL이 발급됩니다"
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
              <button type="button" className="btn-copy" onClick={copyOverlayChatUrl} disabled={!overlayChatUrl}>
                복사
              </button>
            </div>
            <p className="hint">OBS 브라우저 소스에 넣어 방송 화면에 채팅을 오버레이할 때 사용하는 URL입니다.</p>
          </div>
            <p className="hint">스트림 URL·스트림 키·오버레이 URL은 <strong>계정당 고유</strong>하며 방송할 때마다 바뀌지 않습니다. 필요할 때만 스트림 키를 재발급하면 됩니다. (OBS에는 재발급한 새 키를 다시 입력하세요.)</p>
          <div className="stream-rtmp-notice">
            <strong>OBS에서 「연결 실패」가 나오는 경우</strong>
            <p>위 서버 주소(예: rtmp://localhost/live)로 연결하려면 <strong>RTMP 서버</strong>가 PC에서 실행 중이어야 합니다. nginx-rtmp, SRS 등 RTMP 서버를 별도로 설치·실행한 뒤, OBS 방송 설정 → 서비스: 사용자 지정, 서버/스트림 키에 위 값을 입력하세요.</p>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 참여자 설정</h2>
          <div className="settings-row">
            <label className="label">채팅 참여 범위</label>
            <div className="radio-group">
              <label><input type="radio" name="chat-scope" value="all" checked={chatScope === 'all'} onChange={() => setChatScope('all')} /> 모든 시청자</label>
              <label><input type="radio" name="chat-scope" value="follower" checked={chatScope === 'follower'} onChange={() => setChatScope('follower')} /> 팔로워 전용</label>
              <label><input type="radio" name="chat-scope" value="operator" checked={chatScope === 'operator'} onChange={() => setChatScope('operator')} /> 운영자 전용</label>
            </div>
          </div>
          <div className="settings-row">
            <label className="label">본인인증 여부</label>
            <div className="radio-group">
              <label><input type="radio" name="chat-verify" value="naver" /> 본인인증한 시청자만 채팅 허용</label>
            </div>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 모드 설정</h2>
          <div className="toggle-row">
            <div className="label-wrap">
              <span className="label">이모티콘 모드</span>
              <span className="help" title="이모티콘 사용 여부">?</span>
            </div>
            <button type="button" className={`toggle-switch ${emoticonOn ? 'on' : ''}`} role="switch" aria-pressed={emoticonOn} onClick={() => setEmoticonOn((v) => !v)}><span className="knob" /></button>
          </div>
          <div className="toggle-row">
            <div className="label-wrap">
              <span className="label">저속 모드</span>
              <span className="help" title="채팅 속도 제한">?</span>
            </div>
            <button type="button" className={`toggle-switch ${slowOn ? 'on' : ''}`} role="switch" aria-pressed={slowOn} onClick={() => setSlowOn((v) => !v)}><span className="knob" /></button>
          </div>
        </div>

        <div className="settings-card">
          <h2>채팅 규칙 설정</h2>
          <div className="settings-row">
            <label className="label">채팅 규칙</label>
            <div className="textarea-wrap">
              <textarea value={chatRules} onChange={(e) => setChatRules(e.target.value)} placeholder="채팅 규칙은 한 줄씩 입력해주세요." />
              <button type="button" className="btn-save" onClick={() => alert('채팅 규칙이 저장되었습니다.')}>저장</button>
            </div>
            <p className="hint">• 채팅 규칙은 채팅 참여 전 최초 1회 안내되며 동의를 받습니다.<br />• 규칙을 변경한 경우, 이전 규칙에 동의한 시청자들에게도 다시 동의를 받습니다.</p>
          </div>
          <div className="settings-row">
            <label className="label">채팅 금칙어 설정</label>
            <div className="banned-row">
              <span className="status">등록된 금칙어 0개</span>
              <Link to="/studio/chat" className="btn-manage">금칙어 관리</Link>
            </div>
          </div>
        </div>
      </div>
    </StudioLayout>
  );
}
