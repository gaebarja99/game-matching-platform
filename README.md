# GameMatcher

게임 매칭, 라이브 방송, 채팅, 후원, 커뮤니티 기능을 제공하는 스트리밍 플랫폼 프로젝트입니다.
백엔드는 Spring Boot, 프론트엔드는 React + Vite 기반이며, OBS 연동을 위한 RTMP 서버를 별도로 사용합니다.

프론트 개발 서버: `http://127.0.0.1:5173/`
백엔드 기본 주소: `http://localhost:8080/`

---

## 2026-03-25 작업 내역

### 오늘 추가 작업

- 내 채널 커뮤니티 전용 탭 구성 및 전역 커뮤니티/채널 커뮤니티 동선 정리
- `Watch.tsx` 정상화 후 쪽지 제거, 귓속말 대상 선택 UI 정리
- 귓속말을 현재 방송 채팅 참여자 기준으로만 보낼 수 있도록 프론트/백엔드 연결
- 시청 페이지 하단의 불필요한 공유/설정 버튼 제거 및 지우개 버튼 추가
- 지우개 버튼 클릭 시 입력창뿐 아니라 현재 채팅 기록도 화면에서 즉시 비우도록 변경
- `/watch`와 `/streams` 최근 방송 카드에서 파트너 배지가 동일하게 보이도록 프론트/백엔드 응답 정리
- 관리자 커뮤니티 페이지 한글 깨짐 복구
- 전역 커뮤니티 페이지 한글 깨짐 및 목록/인기글 블록 구조 복구
- 프로필 드롭다운에 관리자 전용 `관리` 메뉴 추가
- 프로필의 `팔로잉 채널`을 프로필 내부 경로로 연결해 좌측 사이드바가 유지되도록 수정
- 프로필 좌측 사이드바의 `설정` 메뉴 제거
- 관리자 방송 관리 액션 순서를 `경고 → 최근 숨김/최근 복구 → 강제 종료`로 정리
- 관리자 방송 관리 액션 컬럼 폭 조정으로 버튼 배치가 덜 뒤섞이도록 수정
- 관리자 커뮤니티 관리 페이지의 `좋아요` 표기를 `추천수` 기준으로 정리

### 이전 작업 정리

- 관리자 센터 기능 확장
- 방송 관리, 커뮤니티 관리, 매칭방 관리, 신고 관리, 회원 관리, 정산 관리, 매출 관리 추가
- 관리자 전용 `/admin/*` 라우트 및 프로필 메뉴 연동
- 방송 경고, 강제 종료, 최근 방송 숨김/복구 기능 추가
- 구독/광고제거/후원 관련 결제 및 매출 집계 로직 보강
- 라이브 방송 분석, 팬 집계, 시청자 관리 기능 구현
- 후원 오버레이 및 채팅 설정을 스튜디오 설정으로 통합
- 스튜디오 메뉴 구조 정리 및 불필요한 메뉴 제거
- 최근 시청 기록 페이지 추가
- 채널/권한 관리, 채널 정보, 팬 기능, 후원 순위 반영
- 친구/메시지/채팅 UX 개선
- 카테고리 페이지 및 스트림 카드 UI 정리

---

## 실행 방법

### 1. 백엔드 실행

```bash
mvn spring-boot:run
```

MySQL 프로필을 사용하는 경우:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql,oauth"
```

### 2. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

환경 파일이 없으면:

```bash
copy .env.example .env
```

기본 예시:

```env
VITE_API_URL=http://localhost:8080
```

### 3. RTMP 서버 실행 (선택)

라이브 방송을 OBS와 연동해 테스트하려면:

```bash
cd rtmp-server
npm install
npm start
```

기본 RTMP 주소:

```text
rtmp://localhost:1935/live
```

---

## 기술 스택

- 백엔드: Java 17, Spring Boot, Spring Data JPA, Spring Security, WebSocket(STOMP)
- 프론트엔드: React, TypeScript, Vite, React Router, SockJS, HLS.js
- 데이터베이스: H2 / MySQL
- 방송: RTMP 서버, FFmpeg, OBS 연동

---

## 주요 기능

- 게임 매칭 및 그룹 채팅
- 라이브 방송 시청과 실시간 채팅
- 팡 후원, 후원 랭킹, 후원 오버레이
- 스튜디오 설정, 알림 설정, 방송 분석
- 커뮤니티, 채널 커뮤니티, 최근 시청 기록
- 관리자 센터, 신고/정산/매출 관리

---

## 프로젝트 구조

```text
.
├─ src/main/java/com/gamematcher     # Spring Boot 백엔드
├─ src/main/resources                # 설정 / 정적 리소스
├─ frontend                          # React + Vite 프론트엔드
├─ rtmp-server                       # RTMP / HLS 서버
└─ README.md
```