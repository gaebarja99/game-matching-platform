@echo off
chcp 65001 >nul
set SPRING_PROFILES_ACTIVE=oauth

rem ===== 구글 로그인 =====
rem Google Cloud Console에서 발급한 클라이언트 ID / 보안 비밀 입력 (또는 시스템 환경변수 사용)
rem set GOOGLE_CLIENT_ID=여기에_클라이언트_ID
rem set GOOGLE_CLIENT_SECRET=여기에_클라이언트_보안_비밀

rem ===== 카카오 로그인 (선택) =====
rem set KAKAO_CLIENT_ID=여기에_REST_API_키_입력
rem set KAKAO_CLIENT_SECRET=

echo [OAuth 프로필] 실행 중 (프로필: oauth) ...
echo 카카오 로그인을 쓰려면 KAKAO_CLIENT_ID 를 위에서 설정하거나 시스템 환경변수에 넣으세요.
echo.

call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=oauth

pause
