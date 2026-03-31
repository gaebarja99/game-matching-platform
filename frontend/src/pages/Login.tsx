import { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Login() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { login, googleLoginUrl, kakaoLoginUrl, naverLoginUrl } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const oauthError = searchParams.get('error');
    const message = searchParams.get('message');
    if (oauthError === 'oauth_failed') {
      const text = message ?? '';
      const isToken401 =
        text.includes('invalid_token_response') ||
        text.includes('401 Unauthorized') ||
        /\b401\b/.test(text);
      if (isToken401) {
        setError(
          '구글 로그인 토큰 요청이 거절되었습니다(401). Google Cloud 콘솔 → OAuth 클라이언트 → 보안 비밀을 재발급한 뒤 GOOGLE_CLIENT_SECRET(또는 application-oauth-local.properties)에 넣으세요. 공개 저장소에 있던 기본 시크릿은 무효일 수 있습니다. 승인된 리디렉션 URI에 http://localhost:8080/login/oauth2/code/google (필요 시 127.0.0.1 동일 경로)을 등록했는지 확인하세요.',
        );
      } else {
        setError(text || '소셜 로그인에 실패했습니다. 다시 시도해 주세요.');
      }
      setSearchParams({}, { replace: true });
    } else if (oauthError === 'oauth_not_configured') {
      setError('소셜 로그인이 설정되지 않았습니다. 아이디/비밀번호로 로그인하거나, 백엔드를 프로젝트 루트(D:\\GameMatcher)에서 실행했는지 확인하세요.');
      setSearchParams({}, { replace: true });
    } else if (oauthError === 'kakao_secret_required') {
      setError(
        '카카오 로그인에 Client Secret 이 필요합니다. Kakao Developers → 앱 → 제품 설정 → 카카오 로그인에서 시크릿 확인 후, 환경변수 KAKAO_CLIENT_SECRET 또는 src/main/resources/application-oauth-local.properties 에 spring.security.oauth2.client.registration.kakao.client-secret 을 설정하세요.',
      );
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const clearErrorOnInput = () => {
    if (error) setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const { ok, message } = await login(loginId.trim(), password);
      if (ok) {
        navigate('/', { replace: true });
        return;
      }
      setError(message ?? '로그인에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="main-modal-backdrop show">
      <div className="main-modal-box">
        <h2 className="modal-title">로그인</h2>
        <form onSubmit={handleSubmit}>
          <div className="modal-field">
            <label htmlFor="main-login-id">아이디</label>
            <input
              id="main-login-id"
              type="text"
              value={loginId}
              onChange={(e) => { setLoginId(e.target.value); clearErrorOnInput(); }}
              onFocus={clearErrorOnInput}
              required
              autoComplete="username"
              autoFocus
            />
          </div>
          <div className="modal-field">
            <label htmlFor="main-login-pw">비밀번호</label>
            <input
              id="main-login-pw"
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); clearErrorOnInput(); }}
              onFocus={clearErrorOnInput}
              required
              autoComplete="current-password"
            />
          </div>
          {error && <p className="modal-error">{error}</p>}
          <div className="login-account-links">
            <Link to="/find-login-id">아이디 찾기</Link>
            <span className="sep">·</span>
            <Link to="/find-password">비밀번호 찾기</Link>
          </div>
          <div className="modal-actions">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? '로그인 중…' : '로그인'}
            </button>
            <Link to="/" className="btn-secondary">취소</Link>
          </div>
          <div className="login-divider">또는</div>
          <a href={googleLoginUrl} className="btn-google">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden>
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Google로 로그인
          </a>
          <a href={kakaoLoginUrl} className="btn-kakao">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden>
              <path fill="#191919" d="M12 3C6.5 3 2 6.58 2 11c0 3.54 2.46 6.63 6 8.37l-1.6 4.9c-.1.3.18.58.46.42l5.14-3.4C17.5 21 22 17.42 22 11c0-4.42-4.5-8-10-8z" />
            </svg>
            카카오로 로그인
          </a>
          <a href={naverLoginUrl} className="btn-naver">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden>
              <path fill="#fff" d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" />
            </svg>
            네이버로 로그인
          </a>
        </form>
        <p className="modal-footer">계정이 없으신가요? <Link to="/register">회원가입</Link></p>
      </div>
    </div>
  );
}
