# GameMatcher

> 매출 관리, 스트리머 전용 채팅창 기능이 미흡하여 보완 예정입니다.

게임 팀 매칭·방송·채팅을 위한 풀스택 웹 애플리케이션입니다.  
Spring Boot 백엔드(API + WebSocket) + React(Vite) 프론트엔드 분리 구조이며, OBS 라이브 방송 연동을 위해 RTMP 서버를 별도로 실행할 수 있습니다.

**로컬 개발**: 프론트는 **http://127.0.0.1:5173/**, API는 **http://localhost:8080** (`application.properties`·`frontend/.env` 기준). 배포 시에는 환경변수로 공인 URL을 덮어쓰면 됩니다.

---

## 다른 PC에서 처음 실행하기

다른 컴퓨터에 프로젝트를 복사·클론한 뒤 아래 순서대로 진행하면 됩니다.

### 1. 사전 설치 (필수)

| 항목 | 버전 | 비고 |
|------|------|------|
| **JDK** | 17 이상 | `java -version`으로 확인 |
| **Maven** | 3.6+ | `mvn -v`로 확인 |
| **Node.js** | 18 이상 | `node -v`, `npm -v`로 확인 |

- **Windows**: [Adoptium](https://adoptium.net/)(JDK), [Maven](https://maven.apache.org/download.cgi), [Node.js](https://nodejs.org/) 설치 후 터미널에서 경로 확인.
- **MySQL**은 선택 사항입니다. 사용하지 않으면 H2 메모리 DB로 바로 실행됩니다.

### 2. 프로젝트 루트로 이동

```bash
# 프로젝트가 있는 폴더로 이동 (폴더 이름은 환경에 따라 다를 수 있음)
cd "GameMatcher - stream&matching"
# 또는
cd GameMatcher
```

### 3. 백엔드 실행 (Spring Boot)

**H2 사용 (DB 설치 없이 실행):**

```bash
mvn spring-boot:run
```

**MySQL 사용 시:**  
MySQL 3306에서 DB `GameMatcher` 생성 후:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,oauth"
```

- 백엔드 주소: **http://localhost:8080**
- H2 콘솔(개발용): http://localhost:8080/h2-console (H2 사용 시)

이 터미널은 **계속 켜 둔 상태**로 다음 단계로 진행하세요.

### 4. 프론트엔드 설정 및 실행

**새 터미널**을 열고:

```bash
cd frontend
npm install
```

**환경 변수 설정 (다른 PC에서 필수):**

```bash
# Windows (PowerShell)
copy .env.example .env
# macOS / Linux
cp .env.example .env
```

`.env` 파일을 열어 **API 주소**를 확인·수정하세요. 저장소 기본값은 **http://localhost:8080** 입니다.

```env
VITE_API_URL=http://localhost:8080
```

Firebase(전화번호 인증)나 reCAPTCHA를 쓰려면 `.env.example` 안의 주석을 참고해 `VITE_FIREBASE_*`, `VITE_RECAPTCHA_ENTERPRISE_SITE_KEY` 등을 추가합니다.

**개발 서버 실행:**

```bash
npm run dev
```

- 접속: **http://127.0.0.1:5173/** (localhost 대신 127.0.0.1 권장 — 세션·쿠키 동작 일치)

### 5. (선택) RTMP 서버 — 방송 기능용

OBS로 라이브 방송을 올리고 브라우저에서 HLS로 보려면, **백엔드가 이미 실행 중인 상태**에서:

```bash
cd rtmp-server
npm install
npm start
```

- RTMP 주소: `rtmp://localhost:1935/live`
- HLS 재생을 위해 **FFmpeg**가 PATH에 있거나, Windows 예: `set FFMPEG_PATH=C:\ffmpeg\bin\ffmpeg.exe`

---

### 실행 순서 요약

1. **백엔드**: `mvn spring-boot:run` (루트에서)   mvn spring-boot:run "-Dspring-boot.run.profiles=oauth"

2. **프론트엔드**: `cd frontend` → `npm install` → `.env` 복사·수정 → `npm run dev`
3. **브라우저**: **http://127.0.0.1:5173/**
4. **(선택) RTMP**: `cd rtmp-server` → `npm install` → `npm start`

---

## 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [필요 환경](#필요-환경)
- [실행 방법](#실행-방법)
- [빌드 방법](#빌드-방법)
- [환경 설정](#환경-설정)
- [주요 기능](#주요-기능)

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **백엔드** | Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security, Spring WebSocket(STOMP), OAuth2(Google/Kakao/Naver) |
| **DB** | H2(개발 기본), MySQL(선택) |
| **프론트엔드** | React 19, TypeScript, Vite 7, React Router 7, STOMP/SockJS, HLS.js, Firebase(전화번호 인증) |
| **방송** | Node Media Server(RTMP 수신 → HLS 변환), FFmpeg |

면접·발표용으로 말할 순서와 넣기/빼기 요약은 [docs/기술스택-정리.md](docs/기술스택-정리.md)를 참고하면 됩니다.

---

## 프로젝트 구조

```
(프로젝트 루트)
├── pom.xml                    # Maven 빌드 설정 (Spring Boot)
├── package.json               # 루트 npm 스크립트 (frontend 위임)
├── README.md                  # 본 문서
│
├── src/main/java/com/gamematcher/
│   ├── config/                # Security, CORS, WebSocket, OAuth2 설정
│   ├── controller/            # REST API·WebSocket 컨트롤러
│   ├── service/               # 비즈니스 로직
│   ├── repository/            # JPA Repository
│   ├── entity/                # JPA 엔티티
│   └── dto/, constant/
│
├── src/main/resources/
│   ├── application.properties       # 공통 설정 (포트 8080, H2, 세션, 프론트 URL 등)
│   ├── application-mysql.properties # MySQL 프로필
│   ├── application-oauth.properties # OAuth2 (Google/Kakao/Naver)
│   └── static/                      # 정적 리소스
│
├── frontend/
│   ├── package.json
│   ├── .env.example            # 환경 변수 예시 (복사해 .env 사용)
│   ├── .env                    # 실제 설정 (git 제외, 다른 PC에서 복사 후 수정)
│   ├── vite.config.ts
│   ├── index.html
│   ├── public/
│   └── src/                    # React 앱 (api, components, pages, contexts 등)
│
└── rtmp-server/                # RTMP → HLS (OBS 연동, 선택)
    ├── package.json
    └── index.js
```

---

## 필요 환경

- **JDK 17** 이상
- **Maven** 3.6+
- **Node.js** 18+ (npm)
- **MySQL** 8 (선택, `mysql` 프로필 사용 시)
- **FFmpeg** (RTMP 서버에서 HLS 변환 시, PATH 또는 `FFMPEG_PATH` 환경 변수)

---

## 실행 방법

### 1. 백엔드 (Spring Boot) — 먼저 실행

**H2 메모리 DB 사용 (별도 DB 설치 불필요):**

```bash
# 프로젝트 루트에서
mvn spring-boot:run
```

**MySQL 사용 시:**

```bash
# MySQL 3306 포트에 DB GameMatcher 생성 후
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

- 서버: **http://localhost:8080**
- H2 콘솔: http://localhost:8080/h2-console (H2 사용 시)

### 2. 프론트엔드 (React + Vite)

```bash
cd frontend
npm install
# 다른 PC에서는 .env 없으면: copy .env.example .env (또는 cp .env.example .env)
npm run dev
```

- **접속 주소**: **http://127.0.0.1:5173/**  
  (localhost 대신 127.0.0.1 사용 권장 — 세션·쿠키 동작 일치)
- API 호출은 `VITE_API_URL`(저장소 기본 `http://localhost:8080`)로 보내며, 세션 쿠키는 `credentials: 'include'`로 전송됩니다.

### 3. RTMP 서버 (OBS 라이브 방송 → HLS 재생용, 선택)

방송 기능을 쓰려면 **Spring Boot가 먼저 떠 있어야** 스트림 키 검증이 됩니다.

```bash
cd rtmp-server
npm install
npm start
```

- RTMP 주소: **rtmp://localhost:1935/live**
- HLS 주소 예: **http://localhost:8000/live/스트림키/index.m3u8**

**FFmpeg**가 없으면 HLS 변환이 되지 않아 브라우저 재생이 안 됩니다.  
FFmpeg 설치 후 PATH에 추가하거나, Windows 예:

```cmd
set FFMPEG_PATH=C:\ffmpeg\bin\ffmpeg.exe
```

---

## 빌드 방법

### 백엔드 JAR 빌드

```bash
# 프로젝트 루트에서
mvn clean package -DskipTests
```

- 생성물: `target/shop-0.0.1-SNAPSHOT.jar`
- 실행 예: `java -jar target/shop-0.0.1-SNAPSHOT.jar`

### 프론트엔드 빌드

```bash
cd frontend
npm install
npm run build
```

- 생성물: `frontend/dist/` (정적 파일)

**프로덕션 API 주소**를 지정해 빌드하려면:

```bash
# .env.production 또는 빌드 시 (배포 기본과 동일하게 쓸 때)
VITE_API_URL=http://localhost:8080 npm run build
```

### 프론트 빌드 결과를 백엔드에서 서빙 (통합 배포)

1. `frontend`에서 `npm run build`로 `frontend/dist` 생성
2. `dist` 내용을 `src/main/resources/static/`에 복사하거나, Maven 빌드 시 `frontend/dist`를 `target/classes/static`으로 복사하도록 pom.xml에 리소스 설정 추가
3. Spring Boot JAR 실행 시 통합 접속: 로컬은 `http://localhost:8080/`, 배포는 설정한 공인 URL(nginx 등 프록시 기준)

---

## 환경 설정

### 백엔드 (application.properties / application-mysql.properties)

| 항목 | 설명 |
|------|------|
| `server.port` | API 서버 포트 (기본 8080) |
| `spring.datasource.*` | H2 또는 MySQL 접속 정보 |
| `app.frontend.url` | CORS·OAuth 리다이렉트용 프론트 URL (기본 localhost:5173·8080 등, `APP_FRONTEND_PUBLIC_URL`로 덮어쓰기) |
| `app.upload.path` | 프로필 이미지 등 업로드 경로 |
| `app.streaming.hls-base-url` | HLS 재생 기준 URL (기본 `http://127.0.0.1:8000/hls`, 환경에 맞게 변경) |

OAuth2(Google/Kakao/Naver)는 `application-oauth.properties` 참고.  
시크릿은 `application-oauth-local.properties` 또는 환경 변수(`GOOGLE_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 등)로 설정하는 것을 권장합니다.

### 프론트엔드 (.env)

| 변수 | 설명 |
|------|------|
| `VITE_API_URL` | 백엔드 API 주소 (기본 `http://localhost:8080`) |
| `VITE_FIREBASE_*` | Firebase(전화번호 인증) — 사용 시 필수 |
| `VITE_RECAPTCHA_ENTERPRISE_SITE_KEY` | reCAPTCHA Enterprise (선택) |

- **다른 PC**: `frontend/.env.example`을 복사해 `frontend/.env`로 만든 뒤 위 값들을 수정하세요.
- Firebase: Authentication에서 Phone 활성화, Authorized domains에 localhost 추가. 자세한 내용은 `frontend/.env.example` 주석 참고.

---

## 주요 기능

- **메인(Home)**: 게임별 탭, Team Searching(방 목록·필터·참가), 랜덤 매칭, 랜덤 매칭 내역, 방 만들기
- **게임방**: 방 생성·참가·나가기·마감·삭제, 방별 그룹 채팅(WebSocket)
- **랜덤 매칭**: 큐 참가/취소, 매칭 세션 채팅, 내역 조회·삭제
- **전적 검색 / 이스포츠**: 게임별 전적 조회 화면, 이스포츠 예측·보상 관련 페이지 제공
- **커뮤니티**: 게시글 작성·조회·저장, 채널 커뮤니티, 댓글·신고 기반 운영 기능
- **채팅**: 단체 채팅방 목록, 초대, 실시간 메시지, 1:1 DM, 매치 채팅
- **라이브 방송**: RTMP 수신 → HLS 재생, 시청 페이지, 실시간 방송 채팅, 후원
- **스트리머 스튜디오**: 라이브 관리, 채널 관리, 채팅 설정, 알림, 시청자 관리, 방송 분석, 수익 확인
- **프로필 / 계정 연동**: 내 정보 관리, 활동 내역, 팔로잉, 저장한 글, 구독, 광고 제거, 게임 계정 연동
- **재화 / 결제**: 팡(Pang), 마일리지 상점, 후원, 구독, 결제 및 매출 관련 기능
- **친구·알림**: 친구 요청, 알림, 푸시 토큰 관리
- **관리자 기능**: 회원 관리, 스트리머 관리, 신고 관리, 커뮤니티 관리, 매치방 관리, 매출·정산 관리
- **인증 / 보안**: 일반 로그인, 전화번호 인증, OAuth2 로그인(Google/Kakao/Naver), reCAPTCHA 연동
- **AI 보조 기능**: 챗봇 페이지 및 일부 게임 전적 평가/분석 기능

---

## 라이선스 / 기타

- OAuth2(Google/Kakao/Naver), Firebase, reCAPTCHA 등 외부 서비스 사용 시 각 플랫폼의 설정 및 도메인 등록이 필요합니다.
- 프로젝트 내부 문서·주석은 팀 협업 및 유지보수용으로 활용하시면 됩니다.
