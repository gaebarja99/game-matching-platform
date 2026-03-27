import { useCallback, useEffect, useState } from 'react';
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
  const [emoticonOn, setEmoticonOn] = useState(false);
  const [slowModeOn, setSlowModeOn] = useState(false);

  const loadMyStream = useCallback(() => {
    fetch(apiUrl('api/streams/my'), { credentials: 'include' })
      .then((response) => {
        if (response.status === 401) {
          navigate('/streams');
          return [];
        }
        return response.ok ? response.json() : [];
      })
      .then((list: MyStream[]) => {
        const stream =
          Array.isArray(list) && list.length > 0
            ? list.find((item) => item.status === 'LIVE') || list[0]
            : null;

        if (!stream?.id) {
          setCurrentStream(null);
          setStreamNoData(true);
          return;
        }

        setStreamNoData(false);
        setCurrentStream(stream);
        setTitle(stream.title ?? '');
        if (stream.game) {
          setGame(stream.game);
        }
      })
      .catch(() => setStreamNoData(true));
  }, [navigate]);

  useEffect(() => {
    loadMyStream();
  }, [loadMyStream]);

  const isLive = currentStream?.status === 'LIVE';
  const backendOrigin = apiUrl('').replace(/\/$/, '');
  const chatWidgetUrl = currentStream?.id ? `${backendOrigin}/chat-widget.html?streamId=${currentStream.id}` : '';
  const overlayUrl = currentStream?.id ? `${backendOrigin}/chat-widget.html?streamId=${currentStream.id}&overlay=1` : '';

  const copyToClipboard = (text: string, label: string) => {
    if (!text || !navigator.clipboard?.writeText) {
      return;
    }
    navigator.clipboard.writeText(text);
    window.alert(`${label}이(가) 복사되었습니다.`);
  };

  const addTag = () => {
    const raw = tagInput.trim();
    if (!raw) {
      return;
    }
    if (tags.length >= MAX_TAGS) {
      window.alert(`태그는 최대 ${MAX_TAGS}개까지 추가할 수 있습니다.`);
      return;
    }
    if (raw.length > TAG_MAX_LEN || !TAG_REG.test(raw)) {
      window.alert('공백과 특수문자 없이 15자 이내로 입력해 주세요.');
      return;
    }
    if (tags.includes(raw)) {
      window.alert('이미 추가한 태그입니다.');
      return;
    }
    setTags((current) => [...current, raw]);
    setTagInput('');
  };

  const removeTag = (index: number) => {
    setTags((current) => current.filter((_, currentIndex) => currentIndex !== index));
  };

  const handleEndStream = () => {
    if (!currentStream?.id) {
      return;
    }
    if (!window.confirm('방송 종료 처리하시겠어요? OBS를 끈 뒤에도 LIVE가 남아 있을 때 상태를 정리합니다.')) {
      return;
    }

    fetch(apiUrl(`api/streams/${currentStream.id}/end`), {
      method: 'PUT',
      credentials: 'include',
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          window.alert('방송 종료 처리되었습니다.');
          loadMyStream();
        } else {
          window.alert(result.data?.message || '처리에 실패했습니다.');
        }
      })
      .catch(() => window.alert('요청에 실패했습니다.'));
  };

  const handleUpdate = () => {
    if (!currentStream?.id) {
      window.alert('등록된 방송이 없습니다. 아래 방송 등록에서 먼저 방송을 만들어 주세요.');
      return;
    }

    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      window.alert('방송 제목을 입력해 주세요.');
      return;
    }

    setUpdateLoading(true);
    fetch(apiUrl(`api/streams/${currentStream.id}`), {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: trimmedTitle, game }),
    })
      .then((response) => response.json().then((data: { message?: string }) => ({ ok: response.ok, data })))
      .then((result) => {
        if (result.ok) {
          window.alert('방송 정보가 저장되었습니다.');
          loadMyStream();
        } else {
          window.alert(result.data?.message || '저장에 실패했습니다.');
        }
      })
      .catch(() => window.alert('저장 요청에 실패했습니다.'))
      .finally(() => setUpdateLoading(false));
  };

  const handleRegister = () => {
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      window.alert('방송 제목을 먼저 입력해 주세요.');
      return;
    }

    const body: { title: string; game: string; externalUrl?: string } = {
      title: trimmedTitle,
      game,
    };

    if (registerTab === 'easy' && regExternalUrl.trim()) {
      body.externalUrl = regExternalUrl.trim();
    }

    setRegisterResult(null);
    setRegisterLoading(true);

    fetch(apiUrl('api/streams'), {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
      .then((response) => {
        if (!response.ok) {
          return response.text().then((text) => Promise.reject(new Error(text || String(response.status))));
        }
        return response.json();
      })
      .then((data: { id?: number }) => {
        const streamId = data?.id;
        if (body.externalUrl) {
          setRegisterResult({
            html: `방송이 등록되었습니다.<br><br><a href="${window.location.origin}/watch/${streamId ?? ''}">시청 페이지로 이동</a> · <a href="${window.location.origin}/streams">스트림 목록 보기</a>`,
          });
        } else {
          setRegisterResult({
            html: `방송이 등록되었습니다. 스트림 URL과 스트림 키는 <a href="${window.location.origin}/studio/settings">설정</a>에서 확인할 수 있습니다.`,
          });
        }
        loadMyStream();
      })
      .catch((error) => {
        setRegisterResult({
          html: `등록 실패: ${error?.message || '알 수 없는 오류'}`,
        });
      })
      .finally(() => setRegisterLoading(false));
  };

  return (
    <StudioLayout>
      <div className="live-content">
        <div className="live-left">
          <div className="preview-box">
            <p className="preview-msg">방송을 시작하려면 OBS 또는 외부 송출 도구를 연결해 주세요.</p>
            <p className="preview-msg-sub">후원 오버레이와 스트림 키 설정은 이제 설정 페이지에서 관리합니다.</p>
            <button
              type="button"
              className="btn-guide"
              onClick={() => window.alert('스트림 키와 오버레이 URL은 스튜디오 설정에서 확인할 수 있습니다.')}
            >
              설정 안내
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
            <span className="label">방송 설정</span>
            <Link to="/studio/settings" className="link-next">
              스트림 키, 채팅 오버레이, 후원 오버레이 설정 &gt;
            </Link>
          </div>

          {streamNoData && (
            <div className="form-block">
              <p className="hint">등록된 방송이 없습니다. 아래 방송 등록에서 먼저 방송을 생성해 주세요.</p>
            </div>
          )}

          <div className="form-block">
            <label className="label" htmlFor="stream-title">
              방송 제목 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="stream-title"
              placeholder="예: 배그 듀오 랭크 생방송"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </div>

          <div className="form-block">
            <label className="label" htmlFor="stream-game">카테고리</label>
            <select id="stream-game" value={game} onChange={(event) => setGame(event.target.value)}>
              {GAME_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-block toggle-row">
            <div>
              <span className="label">같이보기 방송</span>
              <p className="toggle-desc">동시 시청형 콘텐츠 여부를 표시하는 토글입니다.</p>
            </div>
            <button
              type="button"
              className={`toggle-switch ${watchPartyOn ? 'on' : ''}`}
              role="switch"
              aria-pressed={watchPartyOn}
              onClick={() => setWatchPartyOn((value) => !value)}
            >
              <span className="knob" />
            </button>
          </div>

          <div className="form-block">
            <span className="label">방송 상태</span>
            <Link
              to={currentStream?.id ? `/watch/${currentStream.id}` : '#'}
              className="link-next"
              onClick={(event) => !currentStream?.id && event.preventDefault()}
            >
              내 방송 보기 &gt;
            </Link>
          </div>

          <div className="section-title">스트림 상태</div>
          <div className="form-block">
            <p className="hint">실시간 품질 수치는 추후 연동 예정입니다. 현재는 방송 정보와 채팅 URL만 바로 확인할 수 있습니다.</p>
            <Link
              to={currentStream?.id ? `/watch/${currentStream.id}` : '#'}
              className="link-next"
              onClick={(event) => !currentStream?.id && event.preventDefault()}
            >
              방송 페이지 열기 &gt;
            </Link>
          </div>

          <div className="form-block toggle-row">
            <span className="label">정보 보기</span>
            <button
              type="button"
              className={`toggle-switch ${statsOn ? 'on' : ''}`}
              role="switch"
              aria-pressed={statsOn}
              onClick={() => setStatsOn((value) => !value)}
            >
              <span className="knob" />
            </button>
          </div>
          <div className="stats-row">
            <div className="stat-item"><span className="stat-label">현재 시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">최대 동시시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">평균 동시시청자</span> -</div>
            <div className="stat-item"><span className="stat-label">채팅 참여자</span> -</div>
            <div className="stat-item"><span className="stat-label">팔로워 수</span> -</div>
            <div className="stat-item"><span className="stat-label">구독자 수</span> -</div>
          </div>

          <div className="section-title">태그 (최대 5개)</div>
          <div className="form-block">
            <div className="tag-input-row">
              <input
                type="text"
                placeholder="태그 입력 후 Enter 또는 추가 버튼"
                value={tagInput}
                onChange={(event) => setTagInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    addTag();
                  }
                }}
                maxLength={15}
                autoComplete="off"
              />
              <button type="button" className="btn-add" onClick={addTag}>
                추가
              </button>
            </div>
            <div className="tag-chips-wrap">
              {tags.map((tag, index) => (
                <span key={tag} className="tag-chip">
                  {tag}
                  <button type="button" className="tag-remove" aria-label="태그 제거" onClick={() => removeTag(index)}>
                    ×
                  </button>
                </span>
              ))}
            </div>
            <p className="hint">공백과 특수문자 없이 최대 15자까지 입력할 수 있습니다.</p>
          </div>

          <div className="section-title">미리보기 이미지</div>
          <div className="form-block">
            <div className="upload-area" onClick={() => window.alert('썸네일 업로드 기능은 준비 중입니다.')}>
              <span className="upload-icon">+</span>
              <span>업로드 (1280x720)</span>
            </div>
            <p className="hint">등록하지 않으면 기본 썸네일이 노출됩니다.</p>
          </div>

          <div className="section-title">연령 제한</div>
          <div className="form-block checkbox-row">
            <input
              type="checkbox"
              id="age-restrict"
              checked={ageRestrict}
              onChange={(event) => setAgeRestrict(event.target.checked)}
            />
            <label htmlFor="age-restrict" className="text">
              시청자를 19세 이상으로 제한합니다.
            </label>
          </div>

          <div className="section-title">시청 국가 설정</div>
          <div className="form-block radio-group horizontal">
            <label>
              <input type="radio" name="country" value="all" checked={country === 'all'} onChange={() => setCountry('all')} /> 전체 허용
            </label>
            <label>
              <input type="radio" name="country" value="kr" checked={country === 'kr'} onChange={() => setCountry('kr')} /> 한국만 허용
            </label>
          </div>

          <div className="section-title">유료 프로모션</div>
          <div className="form-block checkbox-row">
            <input
              type="checkbox"
              id="paid-promo"
              checked={paidPromo}
              onChange={(event) => setPaidPromo(event.target.checked)}
            />
            <label htmlFor="paid-promo" className="text">
              광고, 협찬 등 유료 프로모션이 포함된 방송입니다.
            </label>
          </div>
          <div className="form-block radio-group horizontal">
            <label>
              <input type="radio" name="promo" value="yes" checked={promoAllow === 'yes'} onChange={() => setPromoAllow('yes')} /> 허용
            </label>
            <label>
              <input type="radio" name="promo" value="no" checked={promoAllow === 'no'} onChange={() => setPromoAllow('no')} /> 허용 안 함
            </label>
          </div>

          <div className="section-title">다시보기 설정</div>
          <div className="form-block radio-group horizontal">
            <label>
              <input type="radio" name="replay" value="confirm" checked={replay === 'confirm'} onChange={() => setReplay('confirm')} /> 확인 후 게시
            </label>
            <label>
              <input type="radio" name="replay" value="auto" checked={replay === 'auto'} onChange={() => setReplay('auto')} /> 자동 게시
            </label>
            <label>
              <input type="radio" name="replay" value="none" checked={replay === 'none'} onChange={() => setReplay('none')} /> 다시보기 없음
            </label>
          </div>
          <p className="hint">현재는 UI 상태만 제공하며 실제 저장 연동은 추후 확장 가능합니다.</p>

          <div className="section-title">방송 등록</div>
          <p className="hint" style={{ marginBottom: 12 }}>
            외부 플랫폼 링크를 등록하거나, GameMatcher 자체 방송을 생성할 수 있습니다.
          </p>
          <div className="register-tabs">
            <button type="button" className={registerTab === 'easy' ? 'active' : ''} onClick={() => setRegisterTab('easy')}>
              간편 등록
            </button>
            <button type="button" className={registerTab === 'obs' ? 'active' : ''} onClick={() => setRegisterTab('obs')}>
              OBS 송출
            </button>
          </div>
          <div className={`register-panel ${registerTab === 'easy' ? 'show' : ''}`}>
            <p className="hint">트위치, 유튜브 등 외부 방송 주소가 있으면 함께 등록할 수 있습니다.</p>
            <div className="form-block">
              <label className="label">외부 방송 주소</label>
              <input
                type="url"
                placeholder="https://www.twitch.tv/... 또는 https://youtube.com/live/..."
                value={regExternalUrl}
                onChange={(event) => setRegExternalUrl(event.target.value)}
              />
            </div>
          </div>
          <div className={`register-panel ${registerTab === 'obs' ? 'show' : ''}`}>
            <p className="hint">
              방송 등록 후 스트림 URL, 스트림 키, 채팅 오버레이, 후원 오버레이는 <Link to="/studio/settings">설정</Link>에서 확인하세요.
            </p>
            <div className="stream-rtmp-notice">
              <strong>OBS 연결이 안 될 때</strong>
              <p>로컬 RTMP 서버가 실행 중인지 확인한 뒤, 설정 페이지에 나온 서버 주소와 스트림 키를 OBS에 입력해 주세요.</p>
            </div>
          </div>
          {registerResult && <div className="register-result" dangerouslySetInnerHTML={{ __html: registerResult.html ?? '' }} />}
          <div className="form-block" style={{ display: 'flex', gap: 12, marginTop: 20, alignItems: 'center' }}>
            <button type="button" className="btn-stream-start" onClick={handleRegister} disabled={registerLoading}>
              {registerLoading ? '등록 중...' : '방송 등록'}
            </button>
            <button type="button" className="btn-update" onClick={handleUpdate} disabled={updateLoading}>
              {updateLoading ? '저장 중...' : '정보 저장'}
            </button>
          </div>
        </div>

        <div className="live-right">
          {!currentStream?.id ? (
            <div className="chat-panel">
              <div className="chat-icon">💬</div>
              <p>활성 방송이 아직 없습니다.</p>
              <p style={{ fontSize: '0.85rem', marginTop: 8 }}>방송을 등록하면 채팅 URL을 바로 확인할 수 있어요.</p>
            </div>
          ) : (
            <div className="chat-widget-block">
              <div className="qs-title">채팅창 URL</div>
              <p className="hint" style={{ fontSize: '0.8rem', marginBottom: 10 }}>
                연결된 방송: {currentStream.title || `방송 ${currentStream.id}`} (ID: {currentStream.id})
                {isLive ? ' · 현재 방송 중' : ''}
              </p>
              <p className="hint">팝업 채팅창 URL은 OBS 또는 별도 창에서 사용할 수 있습니다.</p>
              <div className="chat-widget-url-row">
                <input
                  type={chatUrlVisible ? 'text' : 'password'}
                  readOnly
                  value={chatUrlVisible ? chatWidgetUrl : (chatWidgetUrl ? '********************************' : '')}
                  placeholder="채팅창 URL"
                />
                <button type="button" className="btn-widget-eye" onClick={() => setChatUrlVisible((value) => !value)} title="표시/숨기기">
                  {chatUrlVisible ? '숨김' : '표시'}
                </button>
                <button type="button" className="btn-copy-url" onClick={() => copyToClipboard(chatWidgetUrl, '채팅창 URL')}>
                  복사
                </button>
                <button
                  type="button"
                  className="btn-open-chat"
                  onClick={() => window.open(chatWidgetUrl, `chat-${currentStream.id}`, 'width=420,height=640')}
                >
                  전용 채팅창 열기
                </button>
              </div>
              <p className="hint" style={{ marginTop: 12, marginBottom: 6 }}>
                방송 화면에 채팅만 띄우려면 아래 오버레이 URL을 OBS 브라우저 소스에 넣으면 됩니다.
              </p>
              <div className="chat-widget-url-row">
                <input type="text" readOnly value={overlayUrl} placeholder="오버레이 URL" style={{ fontSize: '0.8rem' }} />
                <button type="button" className="btn-copy-url" onClick={() => copyToClipboard(overlayUrl, '오버레이 채팅 URL')}>
                  오버레이 복사
                </button>
              </div>
              <p className="hint" style={{ marginTop: 6, marginBottom: 8, fontSize: '0.85rem' }}>
                후원 오버레이는 이 페이지가 아니라 <Link to="/studio/settings">설정</Link>에서 관리합니다.
              </p>
            </div>
          )}

          <div className="quick-settings">
            <div className="qs-title">빠른 설정</div>
            <div className="qs-row">
              <span className="qs-label">채팅 권한</span>
              <select>
                <option>모두</option>
                <option>팔로워만</option>
                <option>구독자만</option>
                <option>비활성화</option>
              </select>
            </div>
            <div className="qs-row">
              <span className="qs-label">이모티콘 모드</span>
              <button type="button" className={`toggle-switch ${emoticonOn ? 'on' : ''}`} role="switch" onClick={() => setEmoticonOn((value) => !value)}>
                <span className="knob" />
              </button>
            </div>
            <div className="qs-row">
              <span className="qs-label">슬로우 모드</span>
              <button type="button" className={`toggle-switch ${slowModeOn ? 'on' : ''}`} role="switch" onClick={() => setSlowModeOn((value) => !value)}>
                <span className="knob" />
              </button>
            </div>
            <Link to="/studio/settings" className="qs-link">채팅/오버레이 설정</Link>
            <Link to="/studio/settings" className="qs-link">후원 오버레이 설정</Link>
          </div>
        </div>
      </div>
    </StudioLayout>
  );
}
