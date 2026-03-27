-- valorant_match_ai_evaluation: 플레이어마다 1행만 허용하는 잘못된 유니크 제거
--
-- 증상 예시 (메시지에 match_id처럼 보일 수 있으나 실제 제약 대상은 플레이어 FK):
--   Duplicate entry '3' for key 'valorant_match_ai_evaluation.UK_...'
--   → '3' 은 valorant_match_player_id 값. 모델(llm_model)이 달라도 두 번째 INSERT가 막힘.
--
-- 기대 스키마(엔티티 ValorantMatchAiEvaluation 과 동일):
--   UNIQUE (valorant_match_player_id, llm_model)
--
-- 실행 전 확인:
--   SHOW INDEX FROM valorant_match_ai_evaluation WHERE Non_unique = 0;
--
-- DB 이름은 환경에 맞게 수정 (application-mysql.properties 의 DB명)
-- USE GameMatcher;

-- Hibernate 가 생성한 단일 컬럼 유니크 이름은 환경마다 다릅니다.
-- 스크린샷 예: UK_pnplkl0nq4bud7xnbe0uuvh5
-- SHOW INDEX 결과에 "Column_name = valorant_match_player_id" 만 있고 Seq_in_index = 1 인 UNIQUE 가
-- 한 개뿐인 행의 Key_name 을 아래에 넣어 DROP 하세요.

ALTER TABLE valorant_match_ai_evaluation DROP INDEX UK_pnplkl0nq4bud7xnbe0uuvh5;

-- 복합 유니크가 이미 있으면 이 줄은 실패합니다 → 그 경우 생략하면 됩니다.
ALTER TABLE valorant_match_ai_evaluation
  ADD CONSTRAINT uk_valorant_ai_eval_player_model UNIQUE (valorant_match_player_id, llm_model);
