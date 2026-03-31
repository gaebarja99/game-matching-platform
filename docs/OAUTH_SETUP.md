# OAuth(구글/카카오) 로그인 설정

## 실행 시 프로필

- OAuth 로그인을 쓰려면 **oauth** 프로필을 켜서 실행하세요.
  - IDE: Run Configuration → Active profiles: `oauth`
  - 커맨드: `-Dspring.profiles.active=oauth` 또는 `--spring.profiles.active=oauth`

## 카카오 로그인

1. **developers.kakao.com** → 내 애플리케이션 → REST API 키 확인
2. **카카오 로그인 리다이렉트 URI**에 아래가 **완전히 동일하게** 등록되어 있어야 합니다. (끝 슬래시 X, 경로 `oauth2`)
   - 배포(HTTPS): `https://YOUR_PUBLIC_DOMAIN/login/oauth2/code/kakao` (실제 도메인으로 교체)
   - 로컬: `http://localhost:8080/login/oauth2/code/kakao`
   - 로컬: `http://127.0.0.1:8080/login/oauth2/code/kakao`
3. **호출 허용 IP 주소**: 플랫폼 키에서 "호출 허용 IP 주소"를 설정해 두었다면, 백엔드 서버가 카카오 API를 호출할 때 쓰는 IP가 그 목록에 있어야 합니다. **로컬 개발 시**에는 비워 두는 것이 401 방지에 유리합니다.
4. **클라이언트 시크릿**을 사용한다면 '카카오 로그인' 코드를 발급·활성화한 뒤, 아래 값 설정합니다.

### 값 설정 방법 (둘 중 하나)

**방법 A: 환경 변수 (권장)**

- `KAKAO_CLIENT_ID` = REST API 키
- `KAKAO_CLIENT_SECRET` = 클라이언트 시크릿 코드 (사용 시에만)

Windows PowerShell 예:

```powershell
$env:KAKAO_CLIENT_ID="여기에_REST_API_키"
$env:KAKAO_CLIENT_SECRET="여기에_클라이언트_시크릿"
# 그 다음 백엔드 실행 (예: mvn spring-boot:run -Dspring.profiles.active=oauth)
```

**방법 B: 로컬 설정 파일 (커밋하지 말 것)**

- 프로젝트 루트에 `application-oauth-local.properties` 파일을 만들고:

```properties
spring.security.oauth2.client.registration.kakao.client-id=여기에_REST_API_키
spring.security.oauth2.client.registration.kakao.client-secret=여기에_클라이언트_시크릿
```

- `application.properties`에 다음 한 줄이 있으면 이 파일이 로드됩니다:
  `spring.config.import=optional:file:./application-oauth-local.properties`

## 구글 로그인

- `application-oauth.properties`의 구글 기본값 또는 환경 변수 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 사용.
- 리다이렉트 URI (구글 콘솔 **승인된 리디렉션 URI**에 앱과 동일하게 등록):
  - 배포: `https://YOUR_PUBLIC_DOMAIN/login/oauth2/code/google`
  - 로컬: `http://localhost:8080/login/oauth2/code/google` (및 필요 시 `http://127.0.0.1:8080/login/oauth2/code/google`)
