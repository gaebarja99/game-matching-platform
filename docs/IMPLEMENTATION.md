# GameMatcher 프로젝트 구현 현황

## 📌 프로젝트 개요

- **스택**: Spring Boot 3.2, Java 17, JPA, MySQL
- **목적**: 게임 계정 연동 및 매치 기록 동기화 플랫폼
- **지원 게임**: LoL, TFT, Valorant, Steam, Discord, Blizzard

---

## 🗂 구현된 기능 목록

### 1. 공통 (Common)

| 구분 | 파일 | 설명 |
|------|------|------|
| **엔티티** | `User` | 사용자 (loginId, username, email, role, status) |
| | `MatchSummary` | 매치 요약 (puuid, matchId, KDA, 승패 등) |
| | `SocialLogin` | 소셜 로그인 연동 |
| | `Report` | 신고 (신고자, 피신고자, 사유, 상태) |
| | `BlockedUser` | 차단 관계 |
| **상수** | `UserStatus` | ACTIVE, INACTIVE, SUSPENDED, DELETED |
| | `Role` | USER, ADMIN |
| | `ReportReason` | 스팸, 괴롭힘, 부적절한 콘텐츠 등 |
| | `ReportStatus` | 접수대기, 검토중, 처리완료, 기각 |
| **예외** | `GameApiException` | API 오류 처리 |
| | `GlobalExceptionHandler` | 전역 예외 + 검증 예외 처리 |

---

### 2. Riot (LoL / TFT)

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 계정 조회 | POST | `/api/riot/account` | gameName+tagLine → puuid |
| 계정 연동 | POST | `/api/riot/account/link` | 유저-Riot 계정 DB 저장 |
| 소환사 조회 | POST | `/api/riot/summoner` | puuid → 소환사 정보 |
| 프로필 조회 | POST | `/api/riot/profile` | 게임 프로필 |
| 매치 목록 | POST | `/api/riot/matches` | 최근 매치 ID 목록 |
| 매치 상세 | POST | `/api/riot/match/detail` | 단일 매치 상세 |
| 최근 매치 | POST | `/api/riot/matches/detail` | 최근 5경기 상세 |
| 통계 조회 | POST | `/api/riot/stats` | 최근 통계 |
| LoL 동기화 | POST | `/api/riot/sync` | LoL 최근 5경기 DB 저장 |
| TFT 동기화 | POST | `/api/riot/tft/sync` | TFT 최근 5경기 DB 저장 |

---

### 3. Valorant

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 매치 동기화 | POST | `/api/valorant/sync` | Henrik API로 Valorant 매치 DB 저장 |

---

### 4. Steam

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 계정 연동 | POST | `/api/steam/account/link` | Steam 계정 연동 |

---

### 5. Discord

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 계정 연동 | POST | `/api/discord/account/link` | Discord 계정 연동 |

---

### 6. Blizzard

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 계정 연동 | POST | `/api/blizzard/account/link` | Battle.net 계정 연동 |

---

### 7. 신고/차단

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 신고 등록 | POST | `/api/users/{userId}/reports` | 사용자 신고 |
| 내 신고 목록 | GET | `/api/users/{userId}/reports` | 내가 한 신고 목록 |
| 신고 상세 | GET | `/api/users/{userId}/reports/{reportId}` | 신고 상세 |
| 신고 취소 | DELETE | `/api/users/{userId}/reports/{reportId}` | 대기중 신고 취소 |
| 사용자 차단 | POST | `/api/users/{userId}/blocks` | 사용자 차단 |
| 차단 해제 | DELETE | `/api/users/{userId}/blocks/{blockedUserId}` | 차단 해제 |
| 차단 목록 | GET | `/api/users/{userId}/blocks` | 내 차단 목록 |
| 차단 여부 | GET | `/api/users/{userId}/blocks/check/{targetUserId}` | 차단 여부 확인 |

---

### 8. 관리자 (신고 처리)

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| 신고 목록 | GET | `/api/admin/reports` | 상태별 신고 목록 (?status=PENDING) |
| 신고 처리 | PATCH | `/api/admin/reports/{reportId}` | 상태 변경 (?status=RESOLVED&adminNote=) |

**권한**: `admin.api.key` 설정 시 `X-Admin-Key` 헤더 필수

---

## 🛡 추가 권장 사항 구현 상태

### 1. ✅ 관리자 권한 (적용됨)
- `AdminAuthFilter`: `/api/admin/*` 요청 시 `X-Admin-Key` 검증
- `admin.api.key` 비어있으면 개발 모드(검증 생략)
- 운영: `ADMIN_API_KEY` 환경변수 또는 `admin.api.key` 설정

### 2. ✅ 차단자 제외 유틸리티 (적용됨)
- `BlockService.filterBlockedUserIds(blockerId, userIds)`: 차단한 사용자 제외
- **사용 예** (매칭/목록 조회 시):
  ```java
  List<Long> filteredIds = blockService.filterBlockedUserIds(currentUserId, candidateUserIds);
  ```

### 3. ⏳ 인증 연동 (준비 중)
- **현재**: `userId`를 path/body로 전달 (인증 없음)
- **권장**: Spring Security + JWT 도입
  1. `spring-boot-starter-security` 의존성 추가
  2. JWT 필터로 토큰 검증 후 SecurityContext에 사용자 저장
  3. `@AuthenticationPrincipal` 또는 `SecurityContextHolder`에서 userId 추출
  4. 신고/차단 API path의 `userId`를 세션 사용자로 대체

---

## 📁 디렉터리 구조

```
src/main/java/com/gamematcher/
├── config/           # 설정 (RiotApiProperties, AdminAuthFilter, FilterConfig)
├── constant/         # 상수 (Role, UserStatus, ReportReason, ReportStatus, GameType...)
├── controller/       # API 컨트롤러
│   ├── riot/
│   ├── valorant/
│   ├── steam/
│   ├── discord/
│   ├── blizzard/
│   └── report/       # ReportBlockController, ReportAdminController
├── dto/              # 요청/응답 DTO
├── entity/           # JPA 엔티티
│   ├── common/       # User, MatchSummary, Report, BlockedUser...
│   ├── riot/         # RiotAccount, TftMatch
│   ├── steam/
│   ├── discord/
│   ├── blizzard/
│   └── valorant/
├── exception/        # GameApiException, GlobalExceptionHandler
├── repository/       # JPA Repository
└── service/          # 비즈니스 로직
```

---

## ⚠ 미구현/향후 과제

| 항목 | 상태 |
|------|------|
| 회원가입/로그인 API | 미구현 |
| Spring Security 인증 | 미구현 |
| 유저 CRUD API | 미구현 |
| 신고 처리 후 제재(정지) 연동 | `UserStatus.SUSPENDED` 존재, API 미연동 |
| 매칭 기능 | 미구현 (계정 연동·매치 동기화 중심) |
| 프론트엔드 | 미구현 |
