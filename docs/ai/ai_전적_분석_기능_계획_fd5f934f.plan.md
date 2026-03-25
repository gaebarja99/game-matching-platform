---
name: AI 전적 분석 기능 계획
overview: Maven + Spring Boot 기반 GameMatcher 프로젝트에 다중 게임 지원 전적 데이터를 LLM·규칙 기반으로 분석하여 점수와 자연어 평가를 생성하는 AI 모듈 구현 계획입니다.
todos:
  - id: phase1-init
    content: GameMatcher 프로젝트 기본 설정 정리 (패키지/이름 통일 등)
    status: completed
  - id: phase2-domain
    content: Game, MatchRecord, MatchRecordParticipant, MatchRecordEvaluation 엔티티 및 Repository 구현
    status: completed
  - id: phase3-rule-score
    content: 규칙 기반 점수·등급 엔진 (게임별 독립 엔진) 구현
    status: pending
  - id: phase4-llm
    content: LLM API 연동 및 자연어 평가 서비스 구현
    status: pending
  - id: phase5-integrate
    content: AI 파이프라인 통합 및 매치 종료 시 평가 트리거 연결
    status: pending
  - id: phase6-api
    content: REST API 및 OpenAPI 문서화, 팀 모듈 연동 포인트 정의
    status: pending
isProject: false
---

# AI 전적 분석·평가 기능 구현 계획

## 0. 실행 개요 (AI 구현 가이드)

| Phase | 의존성 | 목표 |
|-------|--------|------|
| 1 | - | 프로젝트 기본 설정 정리 |
| 2 | 1 | 도메인 엔티티 + DTO/Repository (JSON 변환 구조 선행) |
| 3 | 2 | 게임별 독립 점수 엔진으로 규칙 기반 점수·등급 |
| 4 | 2, 3 | LLM API + JSON 응답 파싱 + Fallback |
| 5 | 2, 3, 4 | AIEvaluationService 통합, 비동기 트리거 |
| 6 | 5 | Controller, OpenAPI, 팀 연동 포인트 |

**패키지 경로**: `com.gamematcher` ( entity.ai.evaluation | repository.ai.evaluation | dto.ai.evaluation | constant.ai.evaluation | service.ai | controller )

---

## 1. 핵심 제약 사항 (반드시 준수)

### ✓ 필수

- **Phase 2 선행**: `rawStats`용 DTO 구조(`BaseStatsDTO`, 게임별 DTO, `StatsConverter`)를 엔티티보다 먼저 확립. 이 구조 없이 진행 시 추후 전면 수정 필요.
- **게임별 독립 엔진**: 각 게임마다 전용 점수 엔진(`ValorantScoreEngine`, `LolScoreEngine` 등)을 분리. 공통 인터페이스 강제 없음. 엔진은 해당 게임 DTO를 직접 입력으로 받아 점수 반환. `RuleBasedScoreService` 단일 클래스 + if-else/switch 분기 **금지**.
- **LLM Response Format 강제**: 프롬프트에서 `{"summary":"...","detailedComment":"..."}` 형태로 응답 형식 명시 + **한국어 응답 명시** 후 파싱 → DTO 매핑.
- **LLM Fallback**: LLM 실패(타임아웃, 할당량 등) 시 규칙 기반 점수·등급만이라도 반환. `summary`/`detailedComment`는 null 또는 기본 문구.

### ✗ 금지

- `rawStats`를 단순 JSON 문자열로만 저장하고 매번 파싱하는 방식
- 게임별 로직을 한 클래스에서 if-else/switch로 분기하는 방식
- LLM에 "평가해줘"만 요청하고 응답 형식을 강제하지 않는 방식
- LLM 실패 시 전체 실패로 두는 방식 (Fallback 없음)
- `@Column(columnDefinition = "clob")` 단독 사용 (H2 전용) → `@Lob @Column` 사용

---

## 2. 기술 스택 및 패키지 구조

| 구분 | 기술 |
|------|------|
| 빌드 | Maven 3.9+ |
| 백엔드 | Spring Boot 3.x, Spring Web, Spring Data JPA |
| DB | H2 (개발) / PostgreSQL 또는 MySQL (운영) |
| AI - LLM | OpenAI API 또는 Anthropic Claude API |
| AI - 점수 | 규칙 기반 (KDA, 승률, 트렌드 등) |
| 문서화 | SpringDoc OpenAPI (Swagger) |

**기존**: `User`, `SocialLogin` 엔티티, `GameList` enum (LEAGUE_OF_LEGENDS, VALORANT, OVERWATCH 등)

| 패키지/경로 | 역할 |
|-------------|------|
| `entity/` | User, SocialLogin (기존) |
| `entity/ai/evaluation/` | Game, MatchRecord, MatchRecordParticipant, MatchRecordEvaluation |
| `repository/`, `repository/ai/evaluation/` | UserRepository(기존), GameRepository, MatchRecordRepository 등 |
| `dto/ai/evaluation/` | BaseStatsDTO, ValorantStatsDTO, LolStatsDTO, GenericStatsDTO, StatsConverter |
| `constant/ai/evaluation/` | Grade, EvaluationStatus, MatchResult |
| `service/ai/score/` | ValorantScoreEngine, LolScoreEngine, GenericScoreEngine, ScoreEngineRouter |
| `service/ai/` | RuleBasedScoreService, LlmEvaluationService, AIEvaluationService |
| `controller/` | EvaluationController (`/api/evaluations/**`) |

---

## 3. Phase 1: 프로젝트 기본 설정 정리

- **상태**: Maven + Spring Boot 3.2 (Web, JPA, H2, Lombok, DevTools) 프로젝트 존재
- **작업**: 패키지/이름 통일 등 기본 설정 정리

---

## 4. Phase 2: 도메인 및 저장소

### 선행: DTO 구조 (반드시 먼저)

| 파일/클래스 | 역할 |
|-------------|------|
| `com.gamematcher.dto.ai.evaluation.BaseStatsDTO` | KDA, 승리 여부 등 공통 필드 추상화 |
| `ValorantStatsDTO`, `LolStatsDTO` 등 | Jackson `@JsonTypeInfo`/`@JsonSubTypes`로 `GameList` enum과 매핑 |
| `StatsConverter` 또는 `ObjectMapper` | `rawStats` JSON ↔ DTO 변환 일원화 |

#### rawStats 구현 패턴 (권장 vs 금지)

**금지**: 엔티티에 JSON 문자열 저장 후 서비스에서 매번 `objectMapper.readValue(rawStats, Map.class)` 같은 ad-hoc 파싱. 타입 안전성 없음, 파싱 비용 매번 발생.

**권장**:

1. **BaseStatsDTO** - `@JsonTypeInfo(property = "game")`, `@JsonSubTypes`로 게임별 DTO 다형성
2. **StatsConverter** - `toDto(String rawStats, String gameCode) → BaseStatsDTO`, `toJson(BaseStatsDTO dto) → String`. ObjectMapper는 JsonSubTypes 설정된 것 사용
3. **엔티티** - `rawStats`는 `@Lob @Column`으로 DB에 대용량 텍스트(TEXT/CLOB) 저장. JSON 타입 컬럼이 필요한 경우 `@JdbcTypeCode(SqlTypes.JSON)` 사용
4. **서비스** - `statsConverter.toDto(participant.getRawStats(), game.getCode())`로 한 번 변환 후 (`participant` = MatchRecordParticipant) `BaseStatsDTO`(또는 ValorantStatsDTO 등)로 타입 안전하게 사용. 파싱은 Converter 내부에서만 발생

### 엔티티

| 엔티티 | 패키지 | 핵심 필드 |
|--------|--------|-----------|
| Game | `com.gamematcher.entity.ai.evaluation` | id, name, code, statsSchema |
| MatchRecord | `com.gamematcher.entity.ai.evaluation` | id, gameId FK, matchId, result (MatchResult enum), playedAt, rawData (JSON) |
| MatchRecordParticipant | `com.gamematcher.entity.ai.evaluation` | id, matchRecordId FK, userId FK→User, role, rawStats (`@Lob`) |
| MatchRecordEvaluation | `com.gamematcher.entity.ai.evaluation` | id, participantId FK, status, score, grade, summary, detailedComment (`@Lob`), **createdAt**, evaluatedAt |

- `MatchRecordParticipant.userId` → `User.id` FK
- `Game.code` → `GameList` enum 값
- `rawData`: `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "clob"` (H2 호환)
- `rawStats`, `detailedComment`: `@Lob @Column` (H2/PostgreSQL/MySQL 모두 호환, `columnDefinition = "clob"` 단독 사용 금지)
- `MatchRecord.result`: `MatchResult` enum (constant.ai.evaluation), `@Enumerated(EnumType.STRING)`
- `MatchRecordEvaluation.status`: `EvaluationStatus` enum (constant.ai.evaluation)
- `MatchRecordEvaluation.createdAt`: 평가 요청 시각 (PENDING 진입 시 설정, 소요 시간 추적용)

### Repository

- `GameRepository`, `MatchRecordRepository`, `MatchRecordParticipantRepository`, `MatchRecordEvaluationRepository`

### 검증

- 더미 게임/매치 데이터로 CRUD 검증

---

## 5. Phase 3: 규칙 기반 점수 엔진 (게임별 독립 엔진)

### 구조

| 구성 요소 | 패키지 | 역할 |
|-----------|--------|------|
| ValorantScoreEngine | `com.gamematcher.service.ai.score` | `ValorantStatsDTO` 직접 입력 → 점수 반환. 발로란트 전용 로직(KDA, 라운드 승률, 헤드샷율 등) |
| LolScoreEngine | `com.gamematcher.service.ai.score` | `LolStatsDTO` 직접 입력 → 점수 반환. LoL 전용 로직(KDA, CS, 딜량, 시야 등) |
| GenericScoreEngine | `com.gamematcher.service.ai.score` | `BaseStatsDTO` 입력 → 점수 반환. **명시적으로 등록한 게임만** (PUBG, APEX 등). Fallback 아님. 하위 DTO 캐스팅 금지 → ClassCastException 방지 |
| ScoreEngineRouter | `com.gamematcher.service.ai.score` | 개별 엔진 직접 주입 + `switch` 명시적 라우팅. 시그니처: `Optional<Integer> calculate(String gameCode, BaseStatsDTO stats)`. 등급 변환은 상위 서비스 책임 |
| RuleBasedScoreService | `com.gamematcher.service.ai` | `ScoreEngineRouter.calculate()` 호출 → `Optional<Integer>` 수신 → `Grade.fromScore(score)` 변환. **Phase 3에서 함께 구현** |

> **주의**: 점수 엔진은 점수 계산 책임만 가짐. LLM 프롬프트 구성(`buildPrompt()`)은 Phase 4의 `LlmEvaluationService` 내부 또는 별도 `PromptBuilder`로 분리.

### 설계 원칙 (게임별 독립 엔진)

| 원칙 | 설명 |
|------|------|
| **독립성** | 공통 인터페이스 강제 없음. 각 엔진은 해당 게임 DTO를 직접 입력으로 받아 `int calculate(...)` 반환 |
| **스탯·가중치 분리** | 게임마다 스탯 종류·중요도가 다름. Valorant는 헤드샷율·라운드 승률, LoL은 CS·시야 등 각자 독립 설계 |
| **점수 원값 반환** | 클램핑 없음. 극단적 성과(매우 잘함/못함)는 음수·큰 값으로 그대로 표시 → 유저 직관적 피드백 |
| **엔진 없으면 미계산** | 등록된 점수 엔진이 없는 게임은 점수 계산하지 않음. Fallback으로 GenericScoreEngine 사용 **금지** |
| **라우팅** | `ScoreEngineRouter`는 `Map` 대신 개별 엔진 직접 주입 + `switch` 문으로 명시적 라우팅. 타입 안정성 확보. 새 게임 추가 시 라우터 수정 불가피 |
| **등급 변환** | 라우터는 점수(`Optional<Integer>`)만 반환. `RuleBasedScoreService`에서 `Grade.fromScore(score)` 호출하여 책임 분리 |

---

### 설계 상세

#### ValorantScoreEngine

- **입력**: `ValorantStatsDTO` 직접 (kills, deaths, assists, **win**, roundsWon, roundsPlayed, headshots, totalShots)
  - `win` (승리 여부) **필수**: 승리 시 추가 보너스 부여
  - KDA = `(kills + assists) / max(deaths, 1)` (0 나누기 방지)
- **매직 넘버 상수화**: 기준점(100), KDA 임계값, 라운드 승률·헤드샷율 임계값 등 `private static final` 상수 분리
- **예시 로직**: `BASE_SCORE + (KDA 보너스/패널티) + (라운드 승률 보너스) + (헤드샷율 보너스) + (승리 보너스)`

#### LolScoreEngine

- **입력**: `LolStatsDTO` 직접 (kills, deaths, assists, **win**, gold, minionsKilled, damageDealt, visionScore, gameDurationMinutes)
  - `win` (승리 여부) **필수**: 승리 시 추가 보너스 부여
  - KDA = `(kills + assists) / max(deaths, 1)` (0 나누기 방지)
- **독립 로직**: LoL 전용 가중치·임계값 (분당 CS = `minionsKilled / gameDurationMinutes`, 딜량, 시야 점수 등)

#### GenericScoreEngine (선택적)

- **입력**: `BaseStatsDTO` (최상위 부모 타입). **하위 DTO로 캐스팅 금지** → ClassCastException 방지.
- **역할**: PUBG, APEX 등에 **명시적으로 등록**했을 때만 사용. 라우터 `switch`에 해당 게임 코드 case를 직접 추가하여 등록. 공통 필드(KDA 등)만으로 점수 계산.
- **Fallback 아님**: 미지원/오타 게임 코드 시 호출하지 않음.

#### ScoreEngineRouter

- **메서드 시그니처**: `Optional<Integer> calculate(String gameCode, BaseStatsDTO stats)`
- **역할**: `gameCode`로 엔진 선택. `StatsConverter.toDto()`로 변환된 DTO를 해당 엔진에 전달.
- **엔진 없으면 미계산**: 등록된 엔진이 없거나 `GameList.valueOf(gameCode)` 예외 시 **점수 계산 안 함** (`Optional.empty()` 반환).
- **라우터 수정**: 새 게임 추가 시 게임별 분기·타입 전달을 위해 라우터 코드 수정 불가피.
- **타입 검증**: Java 16+ 패턴 매칭 `instanceof` 사용 → `ClassCastException` 방지.
  ```java
  case VALORANT -> stats instanceof ValorantStatsDTO v
      ? Optional.of(valorantEngine.calculate(v))
      : Optional.empty();
  ```
  `stats`는 `StatsConverter.toDto(rawStats, gameCode)`로 **동일한 gameCode**로 변환된 DTO여야 함.
- **GenericScoreEngine 주입**: `Optional<GenericScoreEngine>` 생성자 파라미터로 주입 (Spring Boot 3.x 권장 방식. `@Autowired(required = false)` 사용 금지). 빈이 없어도 앱 기동 가능.

> **GameList 점수 미지원**: `OTHERS`, `OVERWATCH`, `COUNTER_STRIKE_2` 등 전용 엔진이 없는 게임은 점수 계산하지 않음 (`Optional.empty()`). `default` 분기에서 처리.

---

### 점수·등급 규칙

- **입력**: K/D/A, 승리 여부, 킬 참여율, 게임 시간 등 (게임별 DTO)
- **산출**: 점수(원값, 음수·대값 허용), 등급(S/A/B/C/D)
- **점수 방식**: **기준점 100**을 두고 전적(KDA, 게임별 지표)에 따라 가감. 100 = 평균 수준. 극단적 성과는 그대로 반영(음수·큰 값 허용).
- **등급 기준**: S 150점 이상, A 120~149점, B 90~119점, C 70~89점, D 69점 이하(음수 포함)
  - `Grade.fromScore(int score)`: `score >= 150` → S, `score >= 120` → A, `score >= 90` → B, `score >= 70` → C, 나머지 → D
- **예시 로직**: `BASE_SCORE(100) + (KDA 보너스/패널티) + (승리 보너스) + (게임별 특수 보너스)` → 등급 매핑.
- **단위 테스트 작성**: ValorantScoreEngine, LolScoreEngine, ScoreEngineRouter 각각 작성. KDA 극단값(deaths=0, all-zero stats), 승리/패배 분기, 0 나누기 방지 검증 포함

---

## 6. Phase 4: LLM 통합

### 작업

- `pom.xml`: OpenAI / Anthropic API 클라이언트 의존성
- `LlmEvaluationService`: **프롬프트 구성(PromptBuilder 역할 포함)** → API 호출 → **JSON 파싱** → DTO 매핑
- **Response Format**: 프롬프트에 아래 두 가지를 반드시 명시
  - 응답 형식: `{"summary":"...","detailedComment":"..."}`
  - 응답 언어: **한국어로 작성** 명시
- **Fallback**: LLM 실패 시 규칙 기반 점수·등급만 반환, `summary`/`detailedComment`는 null 또는 기본 문구
- API 키: `application.properties` 또는 `AI_API_KEY` 환경 변수
- rate limit, retry, 타임아웃 처리

---

## 7. Phase 5: AI 평가 통합 서비스

### 작업

- `RuleBasedScoreService`: `ScoreEngineRouter.calculate()` 호출 → `Optional<Integer>` 수신 → `present` 시 `Grade.fromScore(score)` 변환.
- `AIEvaluationService`: `RuleBasedScoreService`로 점수·등급 획득 후 LLM 코멘트 조합 → `MatchRecordEvaluation` 저장
- 매치 종료 시 자동 평가 트리거 또는 수동 트리거
- `@Async` 사용 시 `status`: PENDING → IN_PROGRESS → COMPLETED/FAILED
- `MatchRecordEvaluation` 엔티티 저장

---

## 8. Phase 6: API 및 연동

### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/evaluations/match-record/{matchRecordId}` | 매치 기록 참가자별 AI 평가 조회 |
| POST | `/api/evaluations/match-record/{matchRecordId}/trigger` | AI 평가 생성/재생성 트리거 |
| GET | `/api/evaluations/user/{userId}` | 유저별 최근 평가 목록 (페이지네이션) |

- SpringDoc OpenAPI 문서화
- 팀 연동: 매칭 모듈이 `POST /api/evaluations/match-record/{matchRecordId}/trigger` 호출 또는 이벤트 발행

---

## 9. 환경 설정

- `ai.llm.provider`, `ai.llm.api-key`, `ai.llm.model` (LLM API)
- `ai.rule.enabled`, `ai.rule.llm-for-grades` (규칙 기반·LLM 적용 등급)

---

## 10. 주의사항

- **비용**: LLM 호출 비용 누적 → 등급 필터링·캐싱·일일 한도 권장
- **다중 게임**: `rawStats` 스키마 문서화, 게임 추가 시 `Game` + `GameList` + 전용 ScoreEngine 확장. **전용 엔진 없으면 점수 계산 안 함** (Fallback 없음). GenericScoreEngine은 PUBG, APEX 등에 명시적으로 등록했을 때만 사용
- **비동기**: `@Async` 또는 메시지 큐로 응답 지연 최소화
- **팀 연동**: 매치 기록은 매칭 모듈 등록·관리, 평가 트리거만 본 모듈 API로 연동
- **DB 컬럼 타입**: 대용량 텍스트 필드는 `@Lob @Column` 사용. `columnDefinition = "clob"` 단독 사용 시 H2 전용이므로 운영 DB 전환 시 문제 발생

---

## 부록 A: AI 파이프라인 플로우

- **입력**: 매치 종료 데이터
- **파이프라인**: 규칙 기반 점수 계산 → LLM 프롬프트 빌더 → LLM API 호출 → 응답 파싱
- **출력**: 점수(기준점 100 가감), 등급(S/A/B/C/D), 한 줄 요약(한국어), 상세 코멘트(한국어)
