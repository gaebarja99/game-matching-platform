import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import StudioLayout from '../components/StudioLayout';
import { apiUrl } from '../api/client';

const GAME_OPTIONS = [
  { value: 'LEAGUE_OF_LEGENDS', label: '리그 오브 레전드' },
  { value: 'VALORANT', label: '발로란트' },
  { value: 'OVERWATCH', label: '오버워치' },
  { value: 'PUBG', label: 'PUBG' },
  { value: 'COUNTER_STRIKE_2', label: '카운터 스트라이크 2' },
  { value: 'APEX_LEGENDS', label: '에이펙스 레전드' },
  { value: 'OTHERS', label: '기타' },
];

const MAX_TAGS = 5;
const TAG_MAX_LEN = 15;
const TAG_REG = /^[a-zA-Z0-9가-힣]+$/;

interface MyStream {
  id: number;
  title?: string;
  game?: string;
  status?: string;
}

export default function StudioLive() {
  const navigate = useNavigate();
  const [currentStream, setCurrentStream] = useState<MyStream | null>(null);
  const [streamNoData, setStreamNoData] = useState(false);
  const [title, setTitle] = useState('');
  const [game, setGame] = useState('PUBG');
  const [watchPartyOn, setWatchPartyOn] = useState(false);
  const [statsOn, setStatsOn] = useState(true);
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [ageRestrict, setAgeRestrict] = useState(false);
  const [country, setCountry] = useState<'all' | 'kr'>('all');
  const [paidPromo, setPaidPromo] = useState(false);
  const [promoAllow, setPromoAllow] = useState<'yes' | 'no'>('no');
  const [replay, setReplay] = useState<'confirm' | 'auto' | 'none'>('confirm');
  const [registerTab, setRegisterTab] = useState<'easy' | 'obs'>('easy');
  const [regExternalUrl, setRegExternalUrl] = useState('');
  const [registerResult, setRegisterResult] = useState<{ html?: string } | null>(null);
  const [registerLoading, setRegisterLoading] = useState(false);
  const [updateLoading, setUpdateLoading] = useState(false);
  const [chatUrlVisible, setChatUrlVisible] = useState(false);
  const [donationOverlayPosition, setDonationOverlayPosition] = useState<'br' | 'bl' | 'tr' | 'tl'>('br');
  const [donationModalOpen, setDonationModalOpen] = useState(false);
  const [minVideoPang, setMinVideoPang] = useState(0);
  const [minTtsPang, setMinTtsPang] = useState(0);
  const [emoticonOn, setEmoticonOn] = useState(false);
  const [slowModeOn, setSlowModeOn] = useState(false);

  const loadMyStream = useCallback(() => {
    fetch(apiUrl('api/streams/my'), { credentials: 'include' })
      .then((r) => {
        if (r.status === 401) {
          navigate('/streams');
          return [];
        }
        return r.ok ? r.json() : [];
      })
      .then((list: MyStream[]) => {
        const stream =
          Array.isArray(list) && list.length > 0
            ? list.find((s) => s.status === 'LIVE') || list[0]
            : null;
        if (!stream?.id) {
          setCurrentStream(null);
          setStreamNoData(true);
          return;
        }
        setStreamNoData(false);
        setCurrentStream(stream);
        setTitle(stream.title ?? '');
        if (stream.game) setGame(stream.game);
      })
      .catch(() => setStreamNoData(true));
  }, [navigate]);

  useEffect(() => {
    loadMyStream();
  }, [loadMyStream]);

  const isLive = currentStream?.status === 'LIVE';

  const addTag = () => {
    const raw = tagInput.trim();
    if (!raw) return;
    if (tags.length >= MAX_TAGS) {
      alert(`태그는 최대 ${MAX_TAGS}개까지 추가할 수 있습니다.`);
      return;
    }
    if (raw.length > TAG_MAX_LEN || !TAG_REG.test(raw)) {
      alert('공백 및 특수 문자 없이 15자 이내로 입력해 주세요.');
      return;
    }
    if (tags.includes(raw)) {
      alert('이미 추가된 태그입니다.');
      return;
    }
    setTags((t) => [...t, raw]);
    setTagInput('');
  };

  const removeTag = (idx: number) => setTags((t) => t.filter((_, i) => i !== idx));

  const handleEndStream = () => {
    if (!currentStream?.id) return;
    if (!confirm('OBS에서 이미 방송을 종료했는데도 LIVE로 보이시나요? 방송 종료 처리하면 시청 화면에서 방송 중이 아님으로 표시됩니다.')) return;
    fetch(apiUrl(`api/streams/${currentStream.id}/end`), { method: 'PUT', credentials: 'include' })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) {
          alert('방송이 종료 처리되었습니다.');
          loadMyStream();
        } else alert(res.data?.message || '처리 실패');
      })
      .catch(() => alert('요청에 실패했습니다.'));
  };

  const handleUpdate = () => {
    if (!currentStream?.id) {
      alert('등록된 방송이 없습니다. 아래 방송 등록에서 방송 시작을 눌러 주세요.');
      return;
    }
    const t = title.trim();
    if (!t) {
      alert('방송 제목을 입력해 주세요.');
      return;
    }
    setUpdateLoading(true);
    fetch(apiUrl(`api/streams/${currentStream.id}`), {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: t, game }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) alert('설정이 저장되었습니다.');
        else alert(res.data?.message || '저장에 실패했습니다.');
      })
      .catch(() => alert('저장 요청이 실패했습니다.'))
      .finally(() => setUpdateLoading(false));
  };

  const handleRegister = () => {
    const isEasy = registerTab === 'easy';
    const t = title.trim();
    if (!t) {
      alert('위에서 방송 제목을 입력한 뒤 방송 등록을 눌러 주세요.');
      return;
    }
    setRegisterResult(null);
    setRegisterLoading(true);
    const body: { title: string; game: string; externalUrl?: string } = {
      title: t,
      game,
    };
    if (isEasy && regExternalUrl.trim()) body.externalUrl = regExternalUrl.trim();
    fetch(apiUrl('api/streams'), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
      .then((r) => {
        if (!r.ok) return r.text().then((t) => Promise.reject(new Error(t || String(r.status))));
        return r.json();
      })
      .then((data: { id?: number }) => {
        const id = data?.id;
        if (body.externalUrl) {
          setRegisterResult({
            html: `등록되었습니다. 사이트에서 방송이 보입니다.<br><br><a href="${window.location.origin}/watch/${id ?? ''}">시청 페이지로 가기</a> · <a href="${window.location.origin}/streams">스트리밍 목록</a>`,
          });
          loadMyStream();
        } else {
          setRegisterResult({
            html: `방송이 등록되었습니다. 스트림 키와 서버 주소는 <a href="${window.location.origin}/studio/settings">방송 관리 → 설정</a>에서 확인하세요.`,
          });
          loadMyStream();
        }
      })
      .catch((err) => setRegisterResult({ html: '등록 실패: ' + (err?.message || '알 수 없는 오류') }))
      .finally(() => setRegisterLoading(false));
  };

  const backendOrigin = apiUrl('').replace(/\/$/, '');
  const chatWidgetUrl = currentStream?.id ? `${backendOrigin}/chat-widget.html?streamId=${currentStream.id}` : '';
  const overlayUrl = currentStream?.id ? `${backendOrigin}/chat-widget.html?streamId=${currentStream.id}&overlay=1` : '';
  /** 후원 애니메이션 전용 오버레이. position: br(하단우) | bl(하단좌) | tr(상단우) | tl(상단좌) */
  const donationOverlayUrl = (pos: string) =>
    currentStream?.id ? `${backendOrigin}/donation-overlay.html?streamId=${currentStream.id}&position=${pos || 'br'}` : '';

  const copyToClipboard = (text: string, label: string) => {
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(() => alert(`${label}이(가) 복사되었습니다.`));
    } else {
      const el = document.createElement('input');
      el.value = text;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
      alert(`${label}이(가) 복사되었습니다.`);
    }
  };

  const openDonationModal = () => {
    if (!currentStream?.id) return;
    fetch(apiUrl(`api/streams/${currentStream.id}/chat-settings`), { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : {}))
      .then((d: { minVideoPang?: number; minTtsPang?: number }) => {
        setMinVideoPang(d.minVideoPang ?? 0);
        setMinTtsPang(d.minTtsPang ?? 0);
      })
      .catch(() => {});
    setDonationModalOpen(true);
  };

  const saveDonationLimits = () => {
    if (!currentStream?.id) return;
    const video = Math.max(0, parseInt(String(minVideoPang), 10) || 0);
    const tts = Math.max(0, parseInt(String(minTtsPang), 10) || 0);
    fetch(apiUrl(`api/streams/${currentStream.id}/chat-settings/donation-limits`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ minVideoPang: video, minTtsPang: tts }),
    })
      .then((r) => r.json().then((d: { message?: string }) => ({ ok: r.ok, data: d })))
      .then((res) => {
        if (res.ok) {
          setDonationModalOpen(false);
          alert('저장되었습니다.');
        } else alert(res.data?.message || '저장에 실패했습니다.');
      })
      .catch(() => alert('저장에 실패했습니다.'));
  };

  return (
    <StudioLayout>
      <div className="live-content">
        <div className="live-left">
          <div className="preview-box">
            <p className="preview-msg">라이브 스트리밍을 시작하려면 스트리밍 소프트웨어를 연결하세요.</p>
            <p className="preview-msg-sub">방송 시작 및 종료는 스트리밍 소프트웨어에서 가능합니다.</p>
            <button type="button" className="btn-guide" onClick={() => alert('OBS 등 스트리밍 소프트웨어 설정 안내는 준비 중입니다.')}>
              스트리밍 설정 안내
            </button>
            <div className="status">
              <span className={`status-dot ${isLive ? 'live' : ''}`} />
              <span>{isLive ? 'LIVE' : '오프라인'}</span>
            </div>
            {isLive && (
              <button type="button" className="btn-end-stream" onClick={handleEndStream}>
                방송 종료 처리
              </button>
            )}
          </div>

          <div className="form-block">
            <span className="label">광고 타임라인</span>
            <Link to="/studio/settings" className="link-next">중간 광고 설정 &gt;</Link>
          </div>

          {streamNoData && (
            <div className="form-block">
              <p className="hint">
                등록된 방송이 없습니다. 아래 <strong>방송 등록</strong>에서 방송 시작 후 이 페이지에서 설정을 수정할 수 있습니다.
              </p>
            </div>
          )}

          <div className="form-block">
            <label className="label" htmlFor="stream-title">
              방송 제목 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="stream-title"
              placeholder="예: 배고픈 쪼렙 7928의 라이브 방송"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="form-block">
            <label className="label" htmlFor="stream-game">카테고리</label>
            <select id="stream-game" value={game} onChange={(e) => setGame(e.target.value)}>
              {GAME_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="form-block toggle-row">
            <div>
              <span className="label">같이보기 방송</span>
              <p className="toggle-desc">같이보기 방송을 진행할 경우, 설정을 켜고 캠페인을 주기하세요.</p>
            </div>
            <button type="button" className={`toggle-switch ${watchPartyOn ? 'on' : ''}`} role="switch" aria-pressed={watchPartyOn} onClick={() => setWatchPartyOn((v) => !v)}>
              <span className="knob" />
            </button>
          </div>

          <div className="form-block">
            <span className="label">스트림 상태</span>
            <Link to={currentStream?.id ? `/watch/${currentStream.id}` : '#'} className="link-next" onClick={(e) => !currentStream?.id && e.preventDefault()}>
              내 방송 보기 &gt;
            </Link>
          </div>

          <div className="section-title">스트림 상태</div>
          <div className="form-block">
            <p className="hint">오프라인 · 방송 시간 - · 해상도 - · 비트레이트 - · FPS -</p>
            <Link to={currentStream?.id ? `/watch/${currentStream.id}` : '#'} className="link-next">내 방송 보기 &gt;</Link>
          </div>

          <div className="form-block toggle-row">
            <span className="label">정보 보기</span>
            <button type="button" className={`toggle-switch ${statsOn ? 'on' : ''}`} role="switch" aria-pressed={statsOn} onClick={() => setStatsOn((v) => !v)}>
              <span className="knob" />
            </button>
          </div>
          <div className="stats-row">
            <div className="stat-item"><span className="stat-label">동시 시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">최고 동시 시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">평균 동시 시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">채팅 참여자</span> -</div>
            <div className="stat-item"><span className="stat-label">팔로워 수</span> -</div>
            <div className="stat-item"><span className="stat-label">구독 수</span> -</div>
          </div>

          <div className="section-title">태그 (최대 5개)</div>
          <div className="form-block">
            <div className="tag-input-row">
              <input
                type="text"
                placeholder="태그 입력 후 Enter 또는 추가 버튼 클릭"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())}
                maxLength={15}
                autoComplete="off"
              />
              <button type="button" className="btn-add" onClick={addTag}>추가</button>
            </div>
            <div className="tag-chips-wrap">
              {tags.map((t, i) => (
                <span key={i} className="tag-chip">
                  {t}
                  <button type="button" className="tag-remove" aria-label="태그 제거" onClick={() => removeTag(i)}>×</button>
                </span>
              ))}
            </div>
            <p className="hint">공백 및 특수 문자 없이 15자까지. 등록 순서대로 방송 정보에 노출됩니다.</p>
          </div>

          <div className="section-title">미리보기 이미지</div>
          <div className="form-block">
            <div className="upload-area" onClick={() => alert('이미지 업로드는 준비 중입니다.')}>
              <span className="upload-icon">+</span>
              <span>업로드 (1280x720)</span>
            </div>
            <p className="hint">등록하지 않으면 방송 시작 화면이 썸네일로 노출됩니다.</p>
          </div>

          <div className="section-title">연령 제한</div>
          <div className="form-block checkbox-row">
            <input type="checkbox" id="age-restrict" checked={ageRestrict} onChange={(e) => setAgeRestrict(e.target.checked)} />
            <label htmlFor="age-restrict" className="text">
              시청자를 19세로 제한하겠습니다. <a href="#" onClick={(e) => { e.preventDefault(); alert('자세히 보기'); }}>자세히 보기</a>
            </label>
          </div>
          <p className="hint">부적합 콘텐츠 설정 및 미성년자 방송 제한 관련 안내입니다.</p>

          <div className="section-title">시청 국가 설정</div>
          <div className="form-block radio-group horizontal">
            <label><input type="radio" name="country" value="all" checked={country === 'all'} onChange={() => setCountry('all')} /> 모든 국가 허용</label>
            <label><input type="radio" name="country" value="kr" checked={country === 'kr'} onChange={() => setCountry('kr')} /> 한국만 허용</label>
          </div>

          <div className="section-title">유료 프로모션</div>
          <div className="form-block checkbox-row">
            <input type="checkbox" id="paid-promo" checked={paidPromo} onChange={(e) => setPaidPromo(e.target.checked)} />
            <label htmlFor="paid-promo" className="text">간접 광고, 협찬 등 제3자로부터 대가를 받은 유료 프로모션을 포함합니다.</label>
          </div>
          <div className="form-block radio-group horizontal">
            <label><input type="radio" name="promo" value="yes" checked={promoAllow === 'yes'} onChange={() => setPromoAllow('yes')} /> 허용</label>
            <label><input type="radio" name="promo" value="no" checked={promoAllow === 'no'} onChange={() => setPromoAllow('no')} /> 허용하지 않음</label>
          </div>

          <div className="section-title">다시보기 설정</div>
          <div className="form-block radio-group horizontal">
            <label><input type="radio" name="replay" value="confirm" checked={replay === 'confirm'} onChange={() => setReplay('confirm')} /> 확인 후 게시</label>
            <label><input type="radio" name="replay" value="auto" checked={replay === 'auto'} onChange={() => setReplay('auto')} /> 자동 게시</label>
            <label><input type="radio" name="replay" value="none" checked={replay === 'none'} onChange={() => setReplay('none')} /> 다시 보기 안 함</label>
          </div>
          <p className="hint">다시보기 영상 생성 후 게시 여부를 선택할 수 있습니다. 게시되지 않은 영상은 2일간 보관 후 삭제됩니다.</p>

          <div className="section-title">방송 등록</div>
          <p className="hint" style={{ marginBottom: 12 }}>간편 모드(트위치/유튜브) 또는 우리 서버로 OBS 방송을 등록하세요.</p>
          <div className="register-tabs">
            <button type="button" className={registerTab === 'easy' ? 'active' : ''} onClick={() => setRegisterTab('easy')}>간편 (트위치/유튜브)</button>
            <button type="button" className={registerTab === 'obs' ? 'active' : ''} onClick={() => setRegisterTab('obs')}>OBS → 우리 서버</button>
          </div>
          <div id="register-panel-easy" className={`register-panel ${registerTab === 'easy' ? 'show' : ''}`}>
            <p className="hint">위에서 방송 제목·카테고리를 입력한 뒤 등록하세요.</p>
            <div className="form-block">
              <label className="label">트위치 또는 유튜브 방송 주소</label>
              <input type="url" placeholder="https://www.twitch.tv/내채널 또는 https://youtube.com/live/..." value={regExternalUrl} onChange={(e) => setRegExternalUrl(e.target.value)} />
            </div>
          </div>
          <div id="register-panel-obs" className={`register-panel ${registerTab === 'obs' ? 'show' : ''}`}>
            <p className="hint">위에서 방송 제목·카테고리를 입력한 뒤 등록하세요. 등록 후 스트림 키와 서버 주소는 <Link to="/studio/settings">방송 관리 → 설정</Link>에서 확인·재발급하세요.</p>
            <div className="stream-rtmp-notice">
              <strong>OBS 「연결 실패」 시</strong>
              <p>서버에 연결하려면 <strong>RTMP 서버(nginx-rtmp 등)</strong>가 실행 중이어야 합니다. 설정에서 안내하는 서버 주소(예: rtmp://localhost/live)로 수신할 수 있는 RTMP 서버를 먼저 띄운 뒤 OBS에서 방송을 시작하세요.</p>
            </div>
          </div>
          {registerResult && (
            <div className="register-result" dangerouslySetInnerHTML={{ __html: registerResult.html ?? '' }} />
          )}
          <div className="form-block" style={{ display: 'flex', gap: 12, marginTop: 20, alignItems: 'center' }}>
            <button type="button" className="btn-stream-start" onClick={handleRegister} disabled={registerLoading}>
              {registerLoading ? '등록 중...' : '방송 등록'}
            </button>
            <button type="button" className="btn-update" onClick={handleUpdate} disabled={updateLoading}>
              {updateLoading ? '저장 중...' : '업데이트'}
            </button>
          </div>
        </div>

        <div className="live-right">
          {!currentStream?.id ? (
            <div className="chat-panel">
              <div className="chat-icon">❗</div>
              <p>채팅을 사용할 수 없습니다.</p>
              <p style={{ fontSize: '0.85rem', marginTop: 8 }}>아직은 고요합니다.</p>
            </div>
          ) : (
            <div className="chat-widget-block">
              <div className="qs-title">채팅창 URL</div>
              <p className="hint" style={{ fontSize: '0.8rem', marginBottom: 10 }}>
                연동된 방송: {currentStream.title || `방송 ${currentStream.id}`} (ID: {currentStream.id}){isLive ? ' — 방송 중' : ''}
              </p>
              <p className="hint">방송 시 시청자와 소통하려면 채팅창 URL을 OBS 브라우저 소스에 넣거나 새 창으로 열어두세요.</p>
              <div className="chat-widget-url-row">
                <input type={chatUrlVisible ? 'text' : 'password'} readOnly value={chatUrlVisible ? chatWidgetUrl : (chatWidgetUrl ? '••••••••••••••••••••••••••••••••' : '')} placeholder="채팅창 URL" />
                <button type="button" className="btn-widget-eye" onClick={() => setChatUrlVisible((v) => !v)} title="표시/숨기기">{chatUrlVisible ? '🙈' : '👁'}</button>
                <button type="button" className="btn-copy-url" onClick={() => copyToClipboard(chatWidgetUrl, '채팅창 URL')}>복사</button>
                <button type="button" className="btn-open-chat" onClick={() => window.open(chatWidgetUrl, `chat-${currentStream.id}`, 'width=420,height=640')}>스트리머 전용 채팅창 열기</button>
              </div>
              <p className="hint" style={{ marginTop: 12, marginBottom: 6 }}>시청 주소·채팅/오버레이 URL은 <strong>계정당 고정</strong>입니다. 방송할 때마다 바뀌지 않으니 OBS에 한 번만 넣어 두면 됩니다. 스트림 키 재발급은 <Link to="/studio/settings">설정</Link>에서 할 수 있습니다.</p>
              <p className="hint" style={{ marginBottom: 6 }}>방송 화면에 채팅만 깔끔하게 보이게 하려면 아래 <strong>오버레이용 URL</strong>을 OBS 브라우저 소스에 <strong>전체 그대로</strong> 넣으세요.</p>
              <div className="chat-widget-url-row">
                <input type="text" readOnly value={overlayUrl} placeholder="오버레이용 URL" style={{ fontSize: '0.8rem' }} />
                <button type="button" className="btn-copy-url" onClick={() => copyToClipboard(overlayUrl, '오버레이용 채팅창 URL')}>오버레이 URL 복사</button>
              </div>
              <p className="hint" style={{ marginTop: 6, marginBottom: 8, fontSize: '0.85rem' }}>오버레이에 &quot;실시간 연결됨&quot;이 보이면 시청 페이지와 동일 채팅이 표시됩니다. &quot;폴링으로 수신 중&quot;이어도 약 2~3초 지연으로 채팅이 표시됩니다.</p>

              <div className="qs-title" style={{ marginTop: 20 }}>후원 애니메이션 URL (OBS 전용)</div>
              <p className="hint" style={{ fontSize: '0.8rem', marginBottom: 8 }}>시청 화면에는 후원 애니메이션이 표시되지 않습니다. OBS 스튜디오에서 <strong>브라우저 소스</strong>를 추가한 뒤 아래 URL을 넣으면, 시청자가 후원할 때 OBS 화면에만 후원 알림이 재생됩니다. 위치는 아래에서 선택하거나 URL의 <code>position=</code> 값을 br(하단우), bl(하단좌), tr(상단우), tl(상단좌)로 바꿔서 조절할 수 있습니다.</p>
              <div className="chat-widget-url-row" style={{ flexWrap: 'wrap', gap: 8 }}>
                <select
                  id="donation-overlay-position"
                  value={donationOverlayPosition}
                  onChange={(e) => setDonationOverlayPosition(e.target.value as 'br' | 'bl' | 'tr' | 'tl')}
                  style={{ minWidth: 100 }}
                >
                  <option value="br">하단 우 (br)</option>
                  <option value="bl">하단 좌 (bl)</option>
                  <option value="tr">상단 우 (tr)</option>
                  <option value="tl">상단 좌 (tl)</option>
                </select>
                <input type="text" readOnly value={donationOverlayUrl(donationOverlayPosition)} placeholder="후원 오버레이 URL" style={{ flex: 1, minWidth: 180, fontSize: '0.8rem' }} />
                <button type="button" className="btn-copy-url" onClick={() => copyToClipboard(donationOverlayUrl(donationOverlayPosition), '후원 오버레이 URL')}>복사</button>
              </div>

              <ul className="chat-widget-hints">
                <li>채팅창·오버레이 URL은 계정당 고정이므로 방송할 때마다 바꿀 필요 없습니다.</li>
                <li>방송 소프트웨어에 브라우저 소스를 미리 URL로 추가해 주세요.</li>
                <li>너비(가로)는 520px에 최적화되어 있습니다.</li>
                <li>브라우저 소스에 [장면이 활성화되면 브라우저를 새로고침] 옵션을 체크해 주세요.</li>
              </ul>
              <button type="button" className="btn-copy-url" style={{ marginTop: 12 }} onClick={openDonationModal}>후원 팡 제한</button>
            </div>
          )}

          <div className="quick-settings">
            <div className="qs-title">빠른 설정</div>
            <div className="qs-row">
              <span className="qs-label">채팅 이용 권한</span>
              <select>
                <option>모두</option>
                <option>팔로워만</option>
                <option>구독자만</option>
                <option>비활성화</option>
              </select>
            </div>
            <div className="qs-row">
              <span className="qs-label">이모티콘 모드</span>
              <button type="button" className={`toggle-switch ${emoticonOn ? 'on' : ''}`} role="switch" onClick={() => setEmoticonOn((v) => !v)}><span className="knob" /></button>
            </div>
            <div className="qs-row">
              <span className="qs-label">저속 모드</span>
              <button type="button" className={`toggle-switch ${slowModeOn ? 'on' : ''}`} role="switch" onClick={() => setSlowModeOn((v) => !v)}><span className="knob" /></button>
            </div>
            <Link to="/studio/chat" className="qs-link">알림 설정</Link>
            <Link to="/studio/chat" className="qs-link">채팅/금칙어 설정</Link>
          </div>
        </div>
      </div>

      <div className={`donation-modal-backdrop ${donationModalOpen ? 'show' : ''}`} role="dialog" aria-modal="true" onClick={() => setDonationModalOpen(false)}>
        <div className="donation-modal-box" onClick={(e) => e.stopPropagation()}>
          <h2>후원 팡 제한</h2>
          <p className="hint">영상 후원 / TTS(후원 메시지)는 설정한 팡 이상일 때만 가능합니다. 0이면 제한 없음.</p>
          <label><span>영상 후원 최소</span><input type="number" min={0} value={minVideoPang} onChange={(e) => setMinVideoPang(Number(e.target.value) || 0)} /> 팡</label>
          <label><span>TTS 최소</span><input type="number" min={0} value={minTtsPang} onChange={(e) => setMinTtsPang(Number(e.target.value) || 0)} /> 팡</label>
          <div className="modal-actions">
            <button type="button" className="btn-save" onClick={saveDonationLimits}>저장</button>
            <button type="button" className="btn-close" onClick={() => setDonationModalOpen(false)}>닫기</button>
          </div>
        </div>
      </div>
    </StudioLayout>
  );
}
