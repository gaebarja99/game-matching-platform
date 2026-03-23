# OBS Studio로 사이트 스트리밍 연동

GameMatcher 사이트에서 OBS로 송출한 라이브 스트림을 시청할 수 있도록 연동하는 방법입니다.

## 흐름 요약

1. **방송자**: API로 스트림 생성 → 응답으로 받은 **서버 URL**과 **스트림 키**를 OBS에 입력 후 방송 시작.
2. **RTMP 서버**(예: nginx-rtmp): OBS에서 보낸 RTMP를 받아 HLS(.m3u8)로 변환.
3. **사이트**: `/watch.html?streamId=1` 또는 `?url=재생URL` 로 HLS 스트림 시청.

## 1. 백엔드 API (Spring Boot)

- `POST /api/streams` — 스트림 생성 (body: `title`, `game`). 응답에 `id`, `playbackUrl` 등 포함.
- `GET /api/streams/{id}/obs-setup` — OBS에 넣을 **서버 URL**과 **스트림 키** 조회.
- `GET /api/streams/live` — 현재 라이브 중인 스트림 목록.
- `GET /api/streams/{id}` — 스트림 상세(재생 URL 포함).

## 2. OBS Studio 설정

1. OBS 실행 → **설정** → **방송**.
2. **서비스**: "사용자 지정" 선택.
3. **서버**: API에서 받은 서버 URL 입력 (예: `rtmp://localhost/live`).
4. **스트림 키**: API에서 받은 스트림 키 입력.
5. **방송 시작** 클릭.

## 3. RTMP/HLS 서버 (nginx-rtmp)

OBS는 RTMP로 송출하므로, RTMP를 받아 HLS로 만들어 주는 서버가 필요합니다.

- **nginx-rtmp** 예시 설정: 프로젝트 루트의 `config-examples/nginx-rtmp.conf` 참고.
- 해당 설정에서:
  - `on_publish` → `http://127.0.0.1:8080/api/streams/notify/live` (스트림 키 검증 및 LIVE 처리).
  - `on_publish_done` → `http://127.0.0.1:8080/api/streams/notify/end` (방송 종료 처리).
- HLS 출력 경로를 Spring Boot 또는 nginx에서 웹으로 서빙하도록 맞추고, `application.properties`의 `app.streaming.hls-base-url`을 그 URL에 맞게 설정.

## 4. application.properties

```properties
# 실제 서버 주소로 변경
app.streaming.rtmp-server-url=rtmp://your-server-ip-or-domain/live
app.streaming.hls-base-url=http://your-server-ip-or-domain:8080/hls
```

- 방송자는 OBS "서버"에 `rtmp-server-url` 값을 넣습니다.
- 시청 페이지는 `hls-base-url` + 스트림키 + `/index.m3u8` 형태의 URL로 재생합니다.

## 5. 시청 페이지

- **URL**: `http://localhost:8080/watch.html?streamId=1`  
  또는 재생 URL을 직접 지정: `watch.html?url=http://.../live/스트림키/index.m3u8`
- HLS는 **hls.js**로 재생됩니다 (Safari는 네이티브 지원).

## 정리

| 역할       | 할 일 |
|------------|--------|
| 방송자     | API로 스트림 생성 → OBS에 서버/스트림 키 입력 → 방송 시작 |
| 서버       | nginx-rtmp로 RTMP 수신 → HLS 출력 → Spring Boot가 콜백으로 LIVE/END 처리 |
| 시청자     | 사이트에서 `watch.html?streamId=1` 로 시청 |

로그인/인증이 붙으면 스트림 생성·OBS 설정 조회 시 `X-User-Id` 대신 세션/토큰으로 사용자 식별하면 됩니다.
