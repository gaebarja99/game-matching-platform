# OBS Studio 사이트 스트리밍 — 상세 설정 가이드

아래 순서대로 진행하면 OBS로 송출한 방송을 GameMatcher 사이트에서 볼 수 있습니다.

---

## 1단계: nginx-rtmp 설치 및 실행

OBS는 **RTMP** 프로토콜로 영상을 보냅니다. 이걸 받아서 **HLS**(웹 브라우저 재생용)로 바꿔 주는 프로그램이 **nginx-rtmp**입니다.  
반드시 **일반 nginx**가 아니라 **nginx-rtmp 모듈이 포함된 nginx**를 써야 합니다.

### 1-1. Windows에서 설치

1. **바이너리 받기**  
   - 예: [nginx-http-flv-module 릴리스](https://github.com/illuspas/nginx-http-flv-module/releases) 에서 Windows용 빌드가 있는지 확인  
   - 또는 [nginx-rtmp-module](https://github.com/arut/nginx-rtmp-module) 문서를 보고 직접 빌드하거나, 다른 사람이 만든 **nginx + rtmp** 조합 바이너리를 사용

2. **압축 해제**  
   - 예: `C:\nginx-rtmp` 같은 폴더에 풀기

3. **설정 파일 넣기**  
   - GameMatcher 프로젝트의 `config-examples/nginx-rtmp.conf` 내용을 복사  
   - nginx 설치 폴더의 `conf/nginx.conf` 를 백업해 두고, 위 내용으로 **교체**하거나 **include**로 불러오기  
   - Windows에서는 경로가 `C:\` 이므로, 아래처럼 **경로만** Windows에 맞게 수정합니다.

**Windows용 설정 예시 (경로만 변경):**

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;

        application live {
            live on;
            on_publish http://127.0.0.1:8080/api/streams/notify/live;
            on_publish_done http://127.0.0.1:8080/api/streams/notify/end;

            hls on;
            hls_path C:/hls/live;        # Windows: C:/hls/live
            hls_fragment 2s;
            hls_playlist_length 6s;
        }
    }
}

http {
    server {
        listen 8081;
        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            root C:/hls;                 # Windows: C:/hls
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
    }
}
```

4. **HLS 폴더 미리 만들기**  
   - `C:\hls` 폴더를 만들어 둡니다.  
   - nginx-rtmp가 방송이 들어오면 그 안에 `live/스트림키/` 폴더를 만들고 `.m3u8`, `.ts` 파일을 씁니다.

5. **nginx 실행**  
   - 명령 프롬프트 또는 PowerShell에서 nginx 설치 폴더로 이동 후:  
     `nginx.exe`  
   - 포트 1935(RTMP), 8081(HTTP HLS) 사용 시 방화벽에서 허용해 두기.

### 1-2. Linux에서 설치

```bash
# 예: Ubuntu — 소스에서 빌드하는 경우
sudo apt install build-essential libpcre3-dev zlib1g-dev
git clone https://github.com/arut/nginx-rtmp-module
# nginx 소스 다운로드 후 ./configure --add-module=../nginx-rtmp-module
# make && sudo make install
```

- 설정 파일 경로는 보통 `/usr/local/nginx/conf/nginx.conf` 또는 `/etc/nginx/nginx.conf`
- `hls_path` 를 `/tmp/hls/live` 로 두면 샘플 설정과 동일 (Spring Boot 쪽 경로만 아래 2단계에서 맞추면 됨).

### 1-3. 확인

- nginx(nginx-rtmp)가 켜진 상태에서  
  - RTMP 포트 1935 리스닝  
  - (선택) HLS HTTP 포트 8081 리스닝  
- `C:\hls`(Windows) 또는 `/tmp/hls`(Linux) 폴더가 존재하는지 확인.

---

## 2단계: Spring Boot 설정 및 HLS 경로 맞추기

GameMatcher 백엔드가 **스트림 정보**를 관리하고, **HLS 파일을 웹으로 제공**할 수 있도록 설정합니다.

### 2-1. application.properties 수정

`src/main/resources/application.properties` 에서 아래 값을 환경에 맞게 설정합니다.

| 설정 항목 | 설명 | 로컬 예시 |
|-----------|------|------------|
| `app.streaming.rtmp-server-url` | OBS에 입력할 **서버 주소** (RTMP) | `rtmp://localhost/live` |
| `app.streaming.hls-base-url` | 시청 시 사용하는 **HLS 베이스 URL** | `http://localhost:8080/hls` |
| `app.streaming.hls-file-path` | nginx-rtmp가 HLS 파일을 쓰는 **폴더(상위 경로)** | Windows: `C:/hls` / Linux: `/tmp/hls` |

**Spring Boot가 HLS 파일을 직접 서빙하는 경우** (같은 PC에서 nginx-rtmp가 `C:\hls\live` 에 쓰는 경우):

```properties
app.streaming.rtmp-server-url=rtmp://localhost/live
app.streaming.hls-base-url=http://localhost:8080/hls
app.streaming.hls-file-path=C:/hls
```

- `hls-file-path` 가 설정되어 있으면 Spring Boot가 **`/hls/**`** 요청을 이 폴더로 연결합니다.  
- 따라서 재생 URL은 `http://localhost:8080/hls/live/스트림키/index.m3u8` 형태가 됩니다.

**다른 서버에서 HLS를 서빙하는 경우** (예: nginx가 8081에서 HLS 제공):

- Spring Boot에는 `hls-file-path` 를 비워 두고,
- `app.streaming.hls-base-url=http://서버주소:8081/hls` 처럼 **실제 HLS가 서빙되는 주소**를 넣습니다.

### 2-2. Spring Boot 실행

```bash
cd D:\GameMatcher
./mvnw spring-boot:run
```

- 서버가 **8080** 포트에서 떠 있는지 확인합니다.  
- nginx-rtmp의 `on_publish` / `on_publish_done` 이 `http://127.0.0.1:8080/api/streams/notify/...` 를 호출하므로, 두 프로그램이 같은 PC에 있으면 127.0.0.1 그대로 사용하면 됩니다.

---

## 3단계: 스트림 생성 및 OBS용 서버/스트림 키 확인

방송할 때 쓸 **스트림 정보**를 API로 만들고, OBS에 넣을 **서버 URL**과 **스트림 키**를 받습니다.

### 3-1. 스트림 생성 (POST /api/streams)

**요청 예시 (PowerShell):**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/streams" `
  -ContentType "application/json" `
  -Body '{"title":"테스트 방송","game":"LEAGUE_OF_LEGENDS"}'
```

**요청 예시 (curl):**

```bash
curl -X POST http://localhost:8080/api/streams \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"테스트 방송\",\"game\":\"LEAGUE_OF_LEGENDS\"}"
```

**응답 예시:**

```json
{
  "id": 1,
  "title": "테스트 방송",
  "game": "LEAGUE_OF_LEGENDS",
  "status": "CREATED",
  "playbackUrl": "http://localhost:8080/hls/live/abc123def456/index.m3u8",
  "userId": 1,
  "startedAt": null,
  "endedAt": null,
  "createdAt": "2025-03-09T12:00:00"
}
```

- 여기서 **`id`** (예: 1)를 기억합니다. 다음 단계에서 OBS 설정 정보를 조회할 때 사용합니다.

### 3-2. OBS 설정용 서버 URL·스트림 키 조회 (GET /api/streams/{id}/obs-setup)

브라우저에서 열거나 API로 조회합니다.

- URL: `http://localhost:8080/api/streams/1/obs-setup`  
  (숫자 `1`은 위에서 받은 스트림 **id**로 바꿉니다.)

**응답 예시:**

```json
{
  "streamKey": "abc123def456...",
  "serverUrl": "rtmp://localhost/live",
  "instructions": "OBS 스튜디오에서 방송 설정 → 서비스: 사용자 지정, 서버: rtmp://localhost/live, 스트림 키: abc123def456..."
}
```

- **serverUrl** → OBS의 "서버" 칸에 그대로 입력  
- **streamKey** → OBS의 "스트림 키" 칸에 그대로 입력  

이 두 값만 정확히 넣으면 OBS가 GameMatcher용 RTMP 서버(nginx-rtmp)로 송출합니다.

---

## 4단계: OBS Studio에서 방송 설정 및 방송 시작

### 4-1. OBS 설치

- [OBS 공식 사이트](https://obsproject.com/)에서 다운로드 후 설치합니다.

### 4-2. 방송 설정 화면 열기

1. OBS 실행  
2. 아래쪽 **「설정」** 버튼 클릭  
3. 왼쪽에서 **「방송」** 선택  

### 4-3. 서비스·서버·스트림 키 입력

| 항목 | 입력 값 |
|------|----------|
| **서비스** | **「사용자 지정」** (맨 아래) |
| **서버** | 3단계에서 받은 **serverUrl** (예: `rtmp://localhost/live`) |
| **스트림 키** | 3단계에서 받은 **streamKey** (예: `abc123def456...`) |

- "스트림 키 표시" 체크하면 입력한 키를 다시 확인할 수 있습니다.  
- **확인** 또는 **적용** 후 설정 창을 닫습니다.

### 4-4. 방송 시작

1. OBS 메인 화면에서 **「방송 시작」** 클릭  
2. nginx-rtmp가 RTMP를 받으면:  
   - `http://127.0.0.1:8080/api/streams/notify/live` 를 호출하고  
   - GameMatcher가 해당 스트림을 **LIVE** 상태로 바꿉니다.  
3. HLS 파일이 `C:\hls\live\스트림키\` (또는 설정한 경로)에 생성됩니다.  
4. 방송을 끌 때 **「방송 중지」**를 누르면 `notify/end` 가 호출되어 **ENDED**로 바뀝니다.

### 4-5. 문제 발생 시 확인할 것

- **연결 거부 / 타임아웃**  
  - nginx-rtmp가 1935 포트에서 실행 중인지  
  - 방화벽에서 1935 포트 허용 여부  
- **403 / 퍼블리시 거부**  
  - Spring Boot가 8080에서 떠 있는지  
  - `on_publish` 콜백 URL이 `http://127.0.0.1:8080/api/streams/notify/live` 인지  
  - OBS에 넣은 스트림 키가 API로 생성한 스트림의 키와 **완전히 동일**한지 (복사·붙여넣기 권장)

---

## 5단계: 시청자가 사이트에서 스트림 보기

### 5-1. 시청 URL

- **스트림 ID로 보기**  
  `http://사이트주소/watch.html?streamId=1`  
  - 로컬: `http://localhost:8080/watch.html?streamId=1`  
  - `1`은 3단계에서 만든 스트림의 **id**로 바꿉니다.

- **재생 URL로 직접 보기**  
  `http://localhost:8080/watch.html?url=http://localhost:8080/hls/live/스트림키/index.m3u8`  
  - 스트림 키는 API 응답의 `playbackUrl` 또는 `obs-setup` 전의 스트림 상세에서 확인할 수 있습니다.

### 5-2. 화면 설명

- **watch.html** 이 로드되면:  
  - `streamId`가 있으면 먼저 `GET /api/streams/{streamId}` 로 제목·게임·상태·재생 URL을 가져옵니다.  
  - **LIVE** 또는 **CREATED**일 때만 `playbackUrl`로 HLS 재생을 시도합니다.  
- 재생은 **hls.js**로 이루어지며, 지원 브라우저에서는 바로 영상이 재생됩니다.

### 5-3. 라이브 목록에서 고르기

- **GET /api/streams/live** 로 현재 라이브 중인 스트림 목록을 조회할 수 있습니다.  
- 각 스트림의 `id`로 `watch.html?streamId=1` 형태의 링크를 만들어 주면, 시청자가 목록에서 클릭해 들어갈 수 있습니다.

---

## 요약 체크리스트

| 순서 | 할 일 | 확인 |
|------|--------|------|
| 1 | nginx-rtmp 설치 후 `config-examples/nginx-rtmp.conf` 참고해 설정 (경로만 OS에 맞게 수정) | ☐ |
| 2 | HLS 출력 폴더 생성 (예: Windows `C:\hls`) | ☐ |
| 3 | application.properties 에 `rtmp-server-url`, `hls-base-url`, `hls-file-path` 설정 | ☐ |
| 4 | Spring Boot 실행 (8080) | ☐ |
| 5 | POST /api/streams 로 스트림 생성 → id 확인 | ☐ |
| 6 | GET /api/streams/{id}/obs-setup 으로 서버 URL·스트림 키 확인 | ☐ |
| 7 | OBS에서 서비스=사용자 지정, 서버·스트림 키 입력 후 방송 시작 | ☐ |
| 8 | 브라우저에서 watch.html?streamId=1 로 시청 | ☐ |

위 순서대로 진행하면 OBS로 송출한 방송이 사이트에서 재생됩니다.  
같은 PC가 아닌 **다른 서버**에 nginx-rtmp를 두는 경우에는, 해당 서버의 IP/도메인으로 `rtmp-server-url`과 `hls-base-url`을 바꾸고, 방화벽·보안 그룹에서 1935(RTMP), 8080(Spring), 8081(HLS 선택 시) 포트를 열어 두면 됩니다.
