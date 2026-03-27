-- PUBG AI 평가: 모델별 다중 행 (발로/롤과 동일 계열)
-- 이미 pubg_match_ai_evaluation 이 있고 participant 단일 유니크만 있는 DB에서만 수동 실행하세요.
-- 인덱스 이름은 SHOW INDEX FROM pubg_match_ai_evaluation; 로 확인 후 조정합니다.

-- ALTER TABLE pubg_match_ai_evaluation ADD COLUMN llm_model VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'LLM 모델 ID' AFTER pubg_match_participant_id;
-- UPDATE pubg_match_ai_evaluation SET llm_model = '' WHERE llm_model IS NULL;
-- ALTER TABLE pubg_match_ai_evaluation DROP INDEX <기존_participant_단일_유니크_이름>;
-- ALTER TABLE pubg_match_ai_evaluation ADD CONSTRAINT uk_pubg_ai_eval_participant_model UNIQUE (pubg_match_participant_id, llm_model);
