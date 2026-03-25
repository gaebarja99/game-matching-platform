# reCAPTCHA Enterprise assessment API 사용 방법

## 1. request.json 준비

`request.json`의 다음 값을 실제 값으로 바꿉니다.

| 자리 표시 | 설명 |
|-----------|------|
| **TOKEN** | `grecaptcha.enterprise.execute()` 호출에서 반환된 토큰 |
| **USER_ACTION** | (선택) `grecaptcha.enterprise.execute()` 호출 시 지정한 action (예: `LOGIN`) |

예시 (실제 토큰은 긴 문자열입니다):

```json
{
  "event": {
    "token": "03AGdBq24...",
    "expectedAction": "LOGIN",
    "siteKey": "6LfBwYgsAAAAAFYOrpLwfsb9n_juEVlFH9xRZTeb"
  }
}
```

## 2. HTTP POST 요청

저장된 `request.json`을 본문으로 다음 URL로 POST 요청을 보냅니다.

- **URL**: `https://recaptchaenterprise.googleapis.com/v1/projects/gamematcher-ddec4/assessments?key=API_KEY`
- **API_KEY**: 현재 프로젝트와 연결된 API 키 (Google Cloud Console → 사용자 인증 정보에서 확인)

### curl 예시

```bash
# API_KEY를 실제 키로 바꾼 뒤 실행
curl -X POST "https://recaptchaenterprise.googleapis.com/v1/projects/gamematcher-ddec4/assessments?key=API_KEY" \
  -H "Content-Type: application/json" \
  -d @request.json
```

### PowerShell 예시

```powershell
# $apiKey에 실제 API 키 설정
$apiKey = "API_KEY"
Invoke-RestMethod -Uri "https://recaptchaenterprise.googleapis.com/v1/projects/gamematcher-ddec4/assessments?key=$apiKey" `
  -Method Post `
  -ContentType "application/json" `
  -InFile request.json
```

응답에 `riskAnalysis` 등이 포함되면 토큰 검증 결과를 확인할 수 있습니다.
