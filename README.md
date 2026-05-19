# 🎮 GameMatcher | 게임 매칭·스트리밍 풀스택 웹 서비스

GameMatcher는 게임 팀 매칭, 실시간 채팅, 라이브 방송, 커뮤니티를 제공하는 풀스택 웹 애플리케이션입니다. <br>
Spring Boot 백엔드(REST API + WebSocket)와 React(Vite) 프론트엔드로 분리되어 있으며, OBS 연동 RTMP → HLS 방송을 지원합니다. <br>

![Project](https://img.shields.io/badge/Project-GameMatcher-orange)
![Service](https://img.shields.io/badge/Service-게임%20매칭·스트리밍·커뮤니티-blue)
![Team](https://img.shields.io/badge/Team-팀%20프로젝트-purple)
![Period](https://img.shields.io/badge/Period-3월%209일~3월%2031일-green)

# 📍 목차
[![주요 기능](https://img.shields.io/badge/주요%20기능-FF6B6B?style=for-the-badge)](#주요-기능)
[![기술 스택](https://img.shields.io/badge/기술%20스택-4DABF7?style=for-the-badge)](#기술-스택)
[![설계](https://img.shields.io/badge/설계-845EF7?style=for-the-badge)](#설계)
[![주요 화면](https://img.shields.io/badge/주요%20화면-FCC419?style=for-the-badge)](#주요-화면) <!-- 신규 추가된 배지 -->
[![역할 분담](https://img.shields.io/badge/역할%20분담-FF922B?style=for-the-badge)](#역할-분담)
[![프로젝트 구조](https://img.shields.io/badge/프로젝트%20구조-ADB5BD?style=for-the-badge)](#프로젝트-구조)

## 주요 기능

- **매칭 / 게임방**
  - 게임별 팀 모집방 생성·참가·나가기·마감·삭제
  - 방 목록 필터·검색, 랜덤 매칭 큐, 매칭 내역
  - 방별·매칭 세션 그룹 채팅(WebSocket STOMP)

- **전적 / 게임 연동**
  - LoL, Valorant, PUBG, CS2, Apex, Overwatch 등 전적 조회
  - Riot, Steam, Blizzard, Discord 등 게임 계정 연동
  - AI 기반 전적 평가·분석

- **커뮤니티**
  - 게시글 작성·조회·저장, 채널 커뮤니티
  - 댓글·신고·관리자 커뮤니티 운영

- **채팅 / 소셜**
  - 단체 채팅방, 1:1 DM, 친구·팔로우
  - 알림·푸시(Firebase)

- **라이브 / 스트리머**
  - RTMP 수신 → HLS 재생, 시청·방송 채팅, 후원
  - 스트리머 스튜디오(채널·알림·시청자·분석·수익)

- **재화 / 결제**
  - 팡(Pang), 마일리지 상점, 구독·후원·결제

- **관리자**
  - 회원·스트리머·신고·커뮤니티·매치방·매출·정산 관리

- **인증 / 보안**
  - 일반 로그인, 전화번호 인증(Firebase)
  - OAuth2(Google / Kakao / Naver), reCAPTCHA

## 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth2-4285F4?style=for-the-badge&logo=google&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?style=for-the-badge)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)
![H2](https://img.shields.io/badge/H2-개발%20DB-blue?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

### Frontend
![React](https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite%207-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![HLS.js](https://img.shields.io/badge/HLS.js-방송%20재생-E34F26?style=for-the-badge)

### Infra / Streaming
![Node.js](https://img.shields.io/badge/Node%20Media%20Server-RTMP-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-HLS%20변환-007808?style=for-the-badge)


- **로컬 개발 환경**

| 항목 | 버전 | 확인 명령 |
|------|------|-----------|
| JDK | 17 이상 | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 18 이상 | `node -v` |
| MySQL | 8 (선택) | `mysql` 프로필 사용 시 |
| FFmpeg | (선택) | RTMP → HLS 방송 사용 시 |

## 설계

<details>
<summary>📜ERD</summary>

> 
>
> <details>
> <summary>👨‍👩‍👧‍👦 매칭 & 채팅</summary>
>
>> <img width="575" height="645" alt="매칭   채팅" src="https://github.com/user-attachments/assets/9e6d6e7a-b3ab-4ba7-b3ee-afc20531504c" />
>
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 외부 계정 연동</summary>
>
>> <img width="653" height="648" alt="외부 계정 연동" src="https://github.com/user-attachments/assets/d485991d-e53e-4b5f-b325-65362aa7914a" />
>    
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 스트리밍 & 결제</summary>
>
>> <img width="615" height="647" alt="스트링밍   결제" src="https://github.com/user-attachments/assets/4a9a371a-e180-4938-bec5-9d1fe074754f" />
>
> </details>
> <details>
> <summary>👨‍👩‍👧‍👦 커뮤니티(관계도)</summary>
>
>> <img width="555" height="647" alt="커뮤니티 (관계도)" src="https://github.com/user-attachments/assets/3f3667b8-2c82-4858-8bd5-a57dd6558f65" />
>    
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 Valorant 전적</summary>
>
>> <img width="618" height="478" alt="발로란트 전적" src="https://github.com/user-attachments/assets/02f16b7b-1931-4a27-9950-d8b5bb691ff0" />
>    
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 LoL 전적</summary>
>
>> <img width="384" height="648" alt="lol 전적" src="https://github.com/user-attachments/assets/1f635499-6302-4f61-a20b-bef7ea717855" /><img width="610" height="521" alt="pubg 전적" src="https://github.com/user-attachments/assets/9d2c0459-e706-4388-ba7b-04d4cde9fd56" />
>    
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 PUBG 전적</summary>
>
>> <img width="610" height="521" alt="pubg 전적" src="https://github.com/user-attachments/assets/9cacc233-c4ff-4773-986d-f7d6e8937934" />
>    
> </details>
>
> <details>
> <summary>👨‍👩‍👧‍👦 전적 기록 & 평가</summary>
>
>> <img width="833" height="647" alt="전적 기록   평가" src="https://github.com/user-attachments/assets/bf807f95-2e8b-4c81-b5ef-e53251f998f2" />
>    
> </details>

</details>


<details>
<summary>📄 API 명세서</summary>
<br>
    
>
> <details>
> <summary>🎮 임현아 — 조건 별 실시간 매칭</summary>
>
> | 구분 | 대표 API |
> | :--- | :--- |
> | **랜덤 매칭** | `POST /api/match/queue/join` (game, tier, position, maxPlayers) |
> | **LoL 5인 매칭** | `POST /api/lol-match/queue/join` (tier, position) |
> | **팀 모집방** | `POST/GET /api/game-rooms`, `join` / `leave` / `close` |
> | **구인 필터** | `GET /api/recruit?game=&tier=&mainPosition=&...` |
> | **실시간** | WebSocket 구독 `/topic/user/{userId}`, `/topic/match/{sessionId}` |
> </details>
>
> <details>
> <summary>🤖 이도원 — AI 전적 분석</summary>
>
> | 구분 | 대표 API |
> | :--- | :--- |
> | **통합 검색** | `POST /api/search/player`, `/match-detail` |
> | **LoL AI** | `POST /api/search/lol/evaluations/match/{matchId}` |
> | **발로 AI** | `POST /api/valorant/evaluations/match/{matchId}` |
> | **PUBG AI** | `POST /api/pubg/evaluations/match/{matchId}` |
> | **배치** | `POST /api/search/batch`, `/batch/json` |
> </details>
>
> <details>
> <summary>📹 김주영 — 라이브 스트리밍</summary>
>
> | 구분 | 대표 API |
> | :--- | :--- |
> | **방송** | `POST/GET/PATCH /api/streams`, `/live`, `/end` |
> | **OBS** | `GET /api/streams/{id}/obs-setup`, `regenerate-stream-key` |
> | **채팅·시청자** | `/api/streams/{id}/chat`, `viewers`, `chat-settings` |
> | **후원** | `POST /api/donate` |
> | **스튜디오** | `/api/studio/channel/*`, `/api/studio/analytics/live` |
> | **실시간 채팅** | 전송 `/app/chat/{streamId}` → 구독 `/topic/stream/{streamId}` |
> </details>
>
> <details>
> <summary>👤 유재훈 — 계정 연동 · 통합 프로필</summary>
>
> | 구분 | 대표 API |
> | :--- | :--- |
> | **연동 통합** | `GET/DELETE /api/account-links`, OAuth `/oauth/{provider}/start` |
> | **Riot** | `POST /api/riot/account/link`, `/profile`, `/sync` |
> | **기타 게임** | `/api/steam/account/link`, `/discord/...`, `/blizzard/...`, `/valorant/sync` |
> | **프로필** | `GET/PATCH /api/users/{userId}/profile`, `PUT /api/profile`, `GET /api/profiles` |
> </details>

</details>

---

## 🖥️ 주요 화면 및 기능 (UI / UX)

<details>
<summary><b>🏃‍♂️ 임현아 — 조건별 실시간 매칭 (Click)</b></summary>
<br/>

*   **메인 및 매칭 대기열 (`/`)**
    <br/>
    <img width="1910" height="899" alt="image" src="https://github.com/user-attachments/assets/fff79b43-5b4d-4d58-84cd-82cfad6f194b" />
    <br/><br/>

*   **방 생성 모달 (`/`)**
    <br/>
    <img width="270" height="735" alt="image" src="https://github.com/user-attachments/assets/8d34641d-af5d-42b1-81cd-3f00e3762e3d" />
    <br/><br/>

*   **매칭 상태 및 성공 (`/`)**
    <br/>
    <img width="1901" height="898" alt="image" src="https://github.com/user-attachments/assets/82b52731-17e4-4108-89d7-a691c0d6e689" />
    <br/><br/>

</details>

<details>
<summary><b>🤖 이도원 — AI 전적 분석 (Click)</b></summary>
<br/>

*   **전적 검색 (`/records`)**
    <br/>
    
    <br/><br/>

*   **전적 결과 및 AI 분석 (`/records/lol/...`)**
    <br/>
    <img width="1867" height="887" alt="image" src="https://github.com/user-attachments/assets/baee1294-0c35-4842-98c8-b88d3e28b268" />

</details>

<details>
<summary><b>📺 김주영 — 라이브 스트리밍 (Click)</b></summary>
<br/>

*   **라이브 스트리밍 목록 (`/streams`)**
    <br/>
    <img width="670" height="593" alt="image" src="https://github.com/user-attachments/assets/8087ac5f-d653-471a-8443-596a9a993852" />
    <br/><br/>

*   **방송 시청 및 채팅 (`/watch/{streamId}`)**
    <br/>
    <img width="644" height="454" alt="image" src="https://github.com/user-attachments/assets/35b47cdc-2fd4-4c7f-a84e-c1c8ee7ed3b6" />
    <br/><br/>

*   **스트리머 스튜디오 (`/studio/revenue` 또는 `/studio/chat`)**
    <br/>
    <img width="654" height="613" alt="image" src="https://github.com/user-attachments/assets/910aadfc-b59e-44d2-a74a-dad70e61aa05" />

</details>

<details>
<summary><b>🔐 유재훈 — 계정 연동 및 통합 프로필 (Click)</b></summary>
<br/>

*   **외부 계정 연동 목록 (`/profile/account-links`)**
    <br/>
    <img width="1327" height="571" alt="image" src="https://github.com/user-attachments/assets/b441e36e-57fc-40a2-a3e9-c7f7a5183f01" />
    <br/><br/>

*   **연동 계정 기준 전적 조회 (`/records/lol/...`)**
    <br/>
    <img width="1897" height="895" alt="image" src="https://github.com/user-attachments/assets/7604d089-8756-45e0-abed-49f1e30920e3" />

</details>

---

## 프로젝트 구조

```
game-matching-platform-main/
├── pom.xml                         # Maven (Spring Boot)
├── package.json                    # 루트 npm 스크립트
├── README.md
│
├── src/main/java/com/gamematcher/
│   ├── GameMatcherApplication.java
│   ├── config/                     # Security, CORS, WebSocket, OAuth2
│   ├── controller/                 # REST API
│   │   ├── account/                # 게임 계정 연동 (Riot, Steam, PUBG 등)
│   │   ├── community/              # 커뮤니티·관리자
│   │   ├── profile/                # 프로필
│   │   ├── report/                 # 신고·차단
│   │   ├── user/
│   │   ├── AuthController.java
│   │   ├── GameRoomController.java
│   │   ├── MatchController.java
│   │   ├── GroupChatController.java
│   │   ├── LiveStreamController.java
│   │   └── ...
│   ├── service/                    # 비즈니스 로직
│   │   ├── auth/, lol/, valorant/, pubg/, cs2/, apex/
│   │   ├── community/, profile/, search/, ai/
│   │   └── ...
│   ├── repository/                 # JPA Repository
│   ├── entity/                     # JPA 엔티티
│   │   ├── match/                  # lol, valorant, pubg, cs2, apex
│   │   ├── community/, profile/, account/
│   │   └── ...
│   ├── dto/, constant/, exception/, mapper/, util/
│   │
├── src/main/resources/
│   ├── application.properties
│   ├── application-mysql.properties
│   ├── application-oauth.properties
│   └── static/
│
├── frontend/
│   ├── package.json
│   ├── .env.example
│   ├── vite.config.ts
│   └── src/
│       ├── api/                    # API 클라이언트
│       ├── components/
│       ├── pages/                  # Home, GameRooms, Community, Studio, Admin 등
│       ├── contexts/               # Auth, Theme, Alert
│       ├── hooks/, lib/, utils/
│       └── styles/
│
└── rtmp-server/                    # RTMP → HLS (OBS 연동, 선택)
    ├── package.json
    └── index.js
```


