-- Valorant 팀 관련 컬럼 길이 확장 (assistant_team, winning_team 등 truncation 해결)
-- 실행: mysql -u root -p gamematcher < src/main/resources/db/fix_valorant_team_columns.sql
-- 또는 MySQL Workbench / DBeaver 등에서 gamematcher DB에 연결 후 아래 SQL 실행

USE gamematcher;

-- valorant_match_player.team
ALTER TABLE valorant_match_player MODIFY COLUMN team VARCHAR(100);

-- valorant_match_round.winning_team (Data too long 에러 원인)
ALTER TABLE valorant_match_round MODIFY COLUMN winning_team VARCHAR(100);

-- valorant_match_round_player.player_team
ALTER TABLE valorant_match_round_player MODIFY COLUMN player_team VARCHAR(100);

-- valorant_round_player_location.player_team
ALTER TABLE valorant_round_player_location MODIFY COLUMN player_team VARCHAR(100);

-- valorant_round_player_damage_event.receiver_team
ALTER TABLE valorant_round_player_damage_event MODIFY COLUMN receiver_team VARCHAR(100);

-- 킬 이벤트 관련
ALTER TABLE valorant_kill_assistant MODIFY COLUMN assistant_team VARCHAR(100);
ALTER TABLE valorant_kill_event   MODIFY COLUMN killer_team   VARCHAR(100);
ALTER TABLE valorant_kill_event   MODIFY COLUMN victim_team   VARCHAR(100);
