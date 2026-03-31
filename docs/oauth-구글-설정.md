# 구글 소셜 로그인 설정 (Spring OAuth 2.0)

벨로그 글 [Spring OAuth 2.0으로 구글 로그인 구현하기](https://velog.io/@alswp006/Spring-OAuth-2.0%EC%9C%BC%EB%A1%9C-%EA%B5%AC%EA%B8%80-%EB%A1%9C%EA%B7%B8%EC%9D%B8-%EA%B5%AC%ED%98%84%ED%95%98%EA%B8%B0)와 같은 방식으로, **구글 클라우드에서 인증 정보를 발급**받은 뒤 이 프로젝트에서 사용하는 방법입니다.

---

## 1. 구글 클라우드에서 인증 정보 발급

### 1) 구글 클라우드 콘솔 접속

- https://console.cloud.google.com/

### 2) 프로젝트 생성/선택

- **프로젝트 선택** → **새 프로젝트** → 원하는 이름(예: GameMatcher)으로 생성

### 3) OAuth 동의 화면 구성

- **API 및 서비스** → **OAuth 동의 화면**
- **외부** 선택 (구글 계정 가진 모든 사용자 사용 가능)
- **앱 정보**: 앱 이름, 사용자 지원 이메일, 개발자 연락처 입력
- **범위**: `email`, `profile`, `openid` 추가
- **테스트 사용자** (외부·테스트 모드일 때): 테스트할 구글 계정 이메일 추가
- **저장 후 계속** → **대시보드로 돌아가기**

### 4) OAuth 클라이언트 ID 생성

- **API 및 서비스** → **사용자 인증 정보** → **사용자 인증 정보 만들기** → **OAuth 클라이언트 ID**
- **애플리케이션 유형**: **웹 애플리케이션**
- **이름**: 예) GameMatcher Web
- **승인된 리디렉션 URI**에 아래 **로컬 + 배포 도메인(운영 시)** 등록
  - `http://localhost:8080/login/oauth2/code/google`
  - `http://127.0.0.1:8080/login/oauth2/code/google`
  - (배포 시) `https://YOUR_PUBLIC_DOMAIN/login/oauth2/code/google`
- **만들기** 클릭

### 5) 클라이언트 ID / 보안 비밀번호 확인

- 생성된 **OAuth 2.0 클라이언트 ID** 목록에서 방금 만든 항목 클릭
- **클라이언트 ID** → `GOOGLE_CLIENT_ID` 로 사용
- **클라이언트 보안 비밀** → `GOOGLE_CLIENT_SECRET` 로 사용 (표시 후 복사해 두기)

---

## 2. 이 프로젝트에서 구글 로그인 실행

### 방법 A: IntelliJ

1. 상단 Run 드롭다운에서 **GameMatcher (OAuth)** 선택
2. **Run** → **Edit Configurations** → **GameMatcher (OAuth)** 선택
3. **Environment variables**에서
   - `GOOGLE_CLIENT_ID` = (위에서 복사한 클라이언트 ID)
   - `GOOGLE_CLIENT_SECRET` = (위에서 복사한 클라이언트 보안 비밀)
4. **Apply** → **OK** 후 **Run**으로 실행

### 방법 B: 터미널 / 배치

**run-oauth.bat** 사용 시:

```bat
set SPRING_PROFILES_ACTIVE=oauth
set GOOGLE_CLIENT_ID=여기에_클라이언트_ID
set GOOGLE_CLIENT_SECRET=여기에_클라이언트_보안_비밀
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=oauth
```

또는 Windows **시스템 환경 변수**에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 설정 후:

```bat
set SPRING_PROFILES_ACTIVE=oauth
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=oauth
```

---

## 3. 동작 방식 (벨로그 글과의 대응)

| 벨로그 글 | 이 프로젝트 |
|-----------|-------------|
| `application-oauth.properties` 에 client-id, client-secret | `application-oauth.properties` + 환경 변수 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| `CustomOAuth2UserService` + 세션에 UserDTO | `OAuth2LoginSuccessHandler`에서 구글 속성(sub, email, name, picture) 추출 → `AuthService.findOrCreateByOAuth(Provider.GOOGLE, ...)` → 세션에 `userId` 저장 |
| scope `profile`, `email` | scope `openid`, `profile`, `email` (구글 표준) |
| 로그인 진입점 `/oauth2/authorization/google` | 동일. 프론트에서 `apiUrl('/oauth2/authorization/google')` 로 이동 |

로그인 성공 후에는 `app.frontend.url`에 맞는 오리진으로 리다이렉트됩니다(저장소 기본은 localhost:5173·8080 등).

---

## 4. 테스트

1. 백엔드를 **oauth** 프로필 + 구글 환경 변수로 실행 (8080 포트)
2. 프론트(5173) 로그인 페이지에서 **Google로 로그인** 클릭
3. 구글 로그인/동의 후 다시 5173으로 돌아오면 성공

문제가 있으면 브라우저 주소창 오류 메시지, 백엔드 로그(콘솔)를 확인하세요.
