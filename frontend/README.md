# GameMatcher React 프론트엔드

분리형 아키텍처: Spring Boot API 서버(`http://localhost:8080`)와 분리된 React SPA입니다.

## 실행 방법

1. **API 서버 먼저 실행** (프로젝트 루트에서)
   ```bash
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

2. **프론트엔드 실행**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   기본 주소: http://localhost:3000

## 환경 변수

- `VITE_API_URL`: API 서버 주소 (기본값 `http://localhost:8080`)
- `.env` 파일에 설정하거나, 빌드 시 지정 가능.

## 로그인

- **일반 로그인**: `/login`에서 아이디/비밀번호 제출 → API `POST /api/auth/login` (세션 쿠키)
- **구글 로그인**: "Google로 로그인" 클릭 → API 서버 `GET /oauth2/authorization/google`로 이동 → 로그인 완료 후 백엔드가 `app.frontend.url`(예: http://localhost:3000)로 리다이렉트

## CORS / 세션

- 백엔드 `app.frontend.url`이 설정되어 있으면 해당 Origin에 대해 CORS 허용 및 `credentials: true`로 쿠키 전송.
- API 호출은 `src/api/client.ts`의 `apiFetch()` 사용 (모두 `credentials: 'include'`).

## Firebase 휴대폰 인증 (아이디/비밀번호 찾기, 회원가입)

- `.env`에 `VITE_FIREBASE_*` 값을 설정하면 전화번호 인증이 동작합니다.
- **auth/invalid-app-credential** 또는 **Failed to initialize reCAPTCHA enterprise verification** / **Triggering the reCAPTCHA v2 verification** 발생 시:
  1. [Firebase 콘솔](https://console.firebase.google.com/) → 해당 프로젝트 → **Authentication** → **Settings** → **Authorized domains**에 **localhost**가 있는지 확인. 없으면 **추가**.
  2. **Authentication** → **Sign-in method**에서 **Phone**이 **사용**인지 확인.
  3. [Google Cloud Console](https://console.cloud.google.com/) → **Security** → **reCAPTCHA Enterprise** → 프로젝트에서 쓰는 **웹 키** 선택 → **도메인**에 **localhost** 추가.
  4. **시도**: `.env`에서 `VITE_RECAPTCHA_ENTERPRISE_SITE_KEY`를 비우거나 주석 처리한 뒤 앱 재시작. Enterprise 스크립트를 로드하지 않으면 Firebase가 처음부터 reCAPTCHA v2만 사용할 수 있어, 위 1·2번이 되어 있을 때 동작할 수 있음.
