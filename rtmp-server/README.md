# RTMP 서버 (Node-Media-Server)

OBS가 보내는 RTMP를 받아 HLS로 변환합니다.  
참고: [node-media-server 이용한 라이브스트리밍 구현](https://lemonpie313.tistory.com/239)

## 필요 조건

- **Node.js** (v16 이상)
- **FFmpeg** (필수 – 없으면 OBS는 연결되지만 **브라우저에서 재생 안 됨**)
  - [FFmpeg 다운로드](https://ffmpeg.org/download.html) → Windows는 "essentials_build" 압축 해제
  - 압축 푼 폴더 안 `bin` 경로를 **시스템 PATH**에 추가 (예: `C:\ffmpeg\bin`)
  - 확인: 새 CMD에서 `ffmpeg -version` 입력 후 버전이 나오면 성공
  - PATH에 안 넣고 쓰려면: `set FFMPEG_PATH=C:\ffmpeg\bin\ffmpeg.exe` 후 `npm start`

## 실행 방법

1. **Spring Boot 먼저 실행** (포트 8080)
   ```bash
   cd D:\GameMatcher
   .\mvnw spring-boot:run
   ```

2. **RTMP 서버 실행**
   ```bash
   cd D:\GameMatcher\rtmp-server
   npm install
   npm start
   ```

3. **application.properties** 에서 HLS 주소를 이 서버(8000)로 맞춤
   ```properties
   app.streaming.rtmp-server-url=rtmp://localhost/live
   app.streaming.hls-base-url=http://localhost:8000
   ```

## OBS 설정

- **서비스**: 사용자 지정
- **서버**: `rtmp://localhost/live`
- **스트림 키**: 방송 등록 페이지에서 발급받은 키

## 주소 정리

| 용도           | 주소 |
|----------------|------|
| OBS 서버       | rtmp://localhost:1935/live |
| HLS 재생(m3u8) | http://localhost:8000/live/스트림키/index.m3u8 |
| 사이트/API     | http://localhost:8080 |

## OBS는 되는데 브라우저에서 재생이 안 될 때

1. **RTMP 서버 터미널**에서 `[OK] FFmpeg 사용 가능`이 나오는지 확인.
2. **`ffmpeg startup failed`** 가 나왔다면 → FFmpeg가 PATH에 없거나 경로가 잘못된 상태입니다.
   - FFmpeg 설치 후 **bin 폴더를 PATH에 추가**하고, **RTMP 서버를 다시 실행**하세요.
3. OBS로 방송 시작한 뒤, 브라우저에서 아래 주소를 직접 열어보세요.  
   `http://localhost:8000/live/여기에스트림키/index.m3u8`  
   (스트림 키는 방송 등록 결과에 나온 값)
   - 여기서도 재생/다운로드가 안 되면 → FFmpeg가 동작하지 않는 것이므로 PATH/설치를 다시 확인하세요.
