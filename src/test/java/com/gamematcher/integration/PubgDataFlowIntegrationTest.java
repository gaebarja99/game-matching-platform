package com.gamematcher.integration;

import com.gamematcher.dto.pubg.*;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.entity.match.pubg.PubgSeason;
import com.gamematcher.mapper.PubgMatchMapper;
import com.gamematcher.mapper.PubgSeasonMapper;
import com.gamematcher.service.pubg.PubgJsonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUBG 데이터 흐름 통합 테스트: JSON → DTO → Entity
 * 매치/플레이어/텔레메트리 API 응답이 전체 파이프라인에서 올바르게 변환되는지 검증
 */
@DisplayName("PUBG JSON→DTO→Entity 흐름 통합 테스트")
class PubgDataFlowIntegrationTest {

    private final PubgJsonService jsonService = new PubgJsonService();
    private final PubgMatchMapper mapper = new PubgMatchMapper();
    private final PubgSeasonMapper seasonMapper = new PubgSeasonMapper();

    @Nested
    @DisplayName("매치 API: JSON → DTO → Entity")
    class MatchApiFlow {

        @Test
        @DisplayName("전체 흐름: pubg_match_sample.json → PubgMatchApiResponse → PubgMatch")
        void json_to_dto_to_entity_전체흐름_검증() throws Exception {
            // 1) JSON 로드
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json"));
            assertThat(json).isNotBlank();

            // 2) JSON → DTO (PubgJsonService)
            PubgMatchApiResponse dto = jsonService.parseMatchResponse(json);
            assertThat(dto).isNotNull();
            assertThat(dto.getData()).isNotNull();
            assertThat(dto.getData().getId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
            assertThat(dto.getIncluded()).hasSize(5); // asset(1) + participant(2) + roster(2)

            // 3) DTO → Entity (PubgMatchMapper)
            PubgMatch entity = mapper.toEntity(dto);
            assertThat(entity).isNotNull();

            // 4) 매치 메타데이터 검증
            assertThat(entity.getMatchId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
            assertThat(entity.getGameMode()).isEqualTo("tdm");
            assertThat(entity.getMapName()).isEqualTo("Tiger_Main");
            assertThat(entity.getTitleId()).isEqualTo("bluehole-pubg");
            assertThat(entity.getShardId()).isEqualTo("steam");
            assertThat(entity.getDuration()).isEqualTo(303);
            assertThat(entity.getCreatedAt()).isEqualTo("2026-03-16T15:42:19Z");
            assertThat(entity.getIsCustomMatch()).isTrue();
            assertThat(entity.getSeasonState()).isEqualTo("progress");
            assertThat(entity.getMatchType()).isEqualTo("custom");

            // 5) 참가자(Entity) 검증 - participant만 변환 (asset, roster 제외)
            assertThat(entity.getParticipants()).hasSize(2);

            PubgMatchParticipant winner = entity.getParticipants().stream()
                    .filter(p -> "HeZ1HeZ1_-".equals(p.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(winner.getParticipantId()).isEqualTo("a563605e-f390-40a7-aeea-d7cb1445fa2e");
            assertThat(winner.getPlayerId()).isEqualTo("account.a87ceb7c124d486a95375069d712f7b8");
            assertThat(winner.getKills()).isEqualTo(11);
            assertThat(winner.getHeadshotKills()).isEqualTo(10);
            assertThat(winner.getAssists()).isEqualTo(0);
            assertThat(winner.getDamageDealt()).isEqualTo(1223.7267);
            assertThat(winner.getDbnos()).isEqualTo(0);
            assertThat(winner.getTimeSurvived()).isEqualTo(303);
            assertThat(winner.getWalkDistance()).isEqualTo(1539.2612);
            assertThat(winner.getWinPlace()).isEqualTo(1);
            assertThat(winner.isWin()).isTrue();

            PubgMatchParticipant second = entity.getParticipants().stream()
                    .filter(p -> "8ink-".equals(p.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(second.getPlayerId()).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
            assertThat(second.getKills()).isEqualTo(4);
            assertThat(second.getDamageDealt()).isEqualTo(695.6375);
            assertThat(second.getWinPlace()).isEqualTo(2);
            assertThat(second.isWin()).isFalse();
        }

        @Test
        @DisplayName("매치 JSON → DTO 단계 검증")
        void match_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json"));
            PubgMatchApiResponse dto = jsonService.parseMatchResponse(json);

            assertThat(dto.getData().getAttributes().getGameMode()).isEqualTo("tdm");
            assertThat(dto.getIncluded().stream()
                    .filter(PubgParticipantIncludedDto.class::isInstance)
                    .count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("플레이어 API: JSON → DTO")
    class PlayerApiFlow {

        @Test
        @DisplayName("플레이어 JSON → PubgPlayerApiResponse (매치 ID 목록 포함)")
        void player_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_player_sample.json"));
            PubgPlayerApiResponse dto = jsonService.parsePlayerResponse(json);

            assertThat(dto.getData()).hasSize(1);
            assertThat(dto.getData().get(0).getId()).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
            assertThat(dto.getData().get(0).getAttributes().getName()).isEqualTo("8ink-");
            assertThat(dto.getData().get(0).getRelationships().getMatchIds()).hasSize(10);
            assertThat(dto.getData().get(0).getRelationships().getMatchIds().get(0).getId())
                    .isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
        }
    }

    @Nested
    @DisplayName("시즌 API: JSON → DTO → Entity")
    class SeasonsApiFlow {

        @Test
        @DisplayName("전체 흐름: pubg_seasons_sample.json → PubgSeasonsApiResponse → List<PubgSeason>")
        void json_to_dto_to_entity_전체흐름_검증() throws Exception {
            // 1) JSON 로드
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_seasons_sample.json"));
            assertThat(json).isNotBlank();

            // 2) JSON → DTO (PubgJsonService)
            PubgSeasonsApiResponse dto = jsonService.parseSeasonsResponse(json);
            assertThat(dto).isNotNull();
            assertThat(dto.getData()).isNotEmpty();

            PubgSeasonDataDto firstDto = dto.getData().get(0);
            assertThat(firstDto.getType()).isEqualTo("season");
            assertThat(firstDto.getId()).isEqualTo("division.bro.official.pc-2018-41");
            assertThat(firstDto.getAttributes().getIsCurrentSeason()).isFalse();
            assertThat(firstDto.getAttributes().getIsOffseason()).isFalse();

            PubgSeasonDataDto currentSeasonDto = dto.getData().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAttributes().getIsCurrentSeason()))
                    .findFirst()
                    .orElseThrow();
            assertThat(currentSeasonDto.getId()).isEqualTo("division.bro.official.pc-2018-40");

            // 3) DTO → Entity (PubgSeasonMapper)
            List<PubgSeason> entities = seasonMapper.toEntities(dto, "steam");
            assertThat(entities).isNotEmpty();
            assertThat(entities).hasSize(dto.getData().size());

            // 4) 첫 번째 시즌 엔티티 검증
            PubgSeason firstEntity = entities.get(0);
            assertThat(firstEntity.getPlatform()).isEqualTo("steam");
            assertThat(firstEntity.getSeasonId()).isEqualTo("division.bro.official.pc-2018-41");
            assertThat(firstEntity.getIsCurrentSeason()).isFalse();
            assertThat(firstEntity.getIsOffseason()).isFalse();

            // 5) 현재 시즌 엔티티 검증
            PubgSeason currentSeasonEntity = entities.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsCurrentSeason()))
                    .findFirst()
                    .orElseThrow();
            assertThat(currentSeasonEntity.getPlatform()).isEqualTo("steam");
            assertThat(currentSeasonEntity.getSeasonId()).isEqualTo("division.bro.official.pc-2018-40");
            assertThat(currentSeasonEntity.getIsCurrentSeason()).isTrue();
            assertThat(currentSeasonEntity.getIsOffseason()).isFalse();
        }

        @Test
        @DisplayName("시즌 JSON → DTO 단계 검증")
        void seasons_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_seasons_sample.json"));
            PubgSeasonsApiResponse dto = jsonService.parseSeasonsResponse(json);

            assertThat(dto.getData()).isNotEmpty();
            assertThat(dto.getLinks()).isNotNull();
            assertThat(dto.getLinks().getSelf()).contains("seasons");
        }
    }

    @Nested
    @DisplayName("텔레메트리 API: JSON → DTO")
    class TelemetryApiFlow {

        @Test
        @DisplayName("텔레메트리 JSON → PubgTelemetryEventDto 리스트")
        void telemetry_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_included_sample.json"));
            var events = jsonService.parseTelemetryResponse(json);

            assertThat(events).isNotEmpty();
            assertThat(events.get(0).getType()).isEqualTo("LogMatchDefinition");
            assertThat(events.get(0).getTimestamp()).startsWith("2026-03-16");
            assertThat(events.get(0).getAdditional()).containsKey("MatchId");

            var loginEvent = events.stream()
                    .filter(e -> "LogPlayerLogin".equals(e.getType()))
                    .findFirst()
                    .orElseThrow();
            assertThat(loginEvent.getAdditional()).containsKey("accountId");
        }
    }

    @Nested
    @DisplayName("PubgPlayerMatchRecordDto → Entity")
    class PlayerMatchRecordToEntity {

        @Test
        @DisplayName("PubgPlayerMatchRecordDto → PubgMatchParticipant 변환")
        void playerMatchRecord_to_participantEntity_검증() {
            PubgMatch match = new PubgMatch();
            match.setMatchId("test-match-id");
            match.setGameMode("squad");

            PubgPlayerMatchRecordDto record = PubgPlayerMatchRecordDto.builder()
                    .matchId("test-match-id")
                    .playerId("account.abc123")
                    .playerName("TestPlayer")
                    .participantId("participant-uuid")
                    .kills(5)
                    .assists(2)
                    .damageDealt(1200.5)
                    .winPlace(1)
                    .build();

            var entities = mapper.toParticipantEntities(match, java.util.List.of(record));

            assertThat(entities).hasSize(1);
            PubgMatchParticipant entity = entities.get(0);
            assertThat(entity.getMatch()).isSameAs(match);
            assertThat(entity.getPlayerId()).isEqualTo("account.abc123");
            assertThat(entity.getName()).isEqualTo("TestPlayer");
            assertThat(entity.getKills()).isEqualTo(5);
            assertThat(entity.getAssists()).isEqualTo(2);
            assertThat(entity.getDamageDealt()).isEqualTo(1200.5);
            assertThat(entity.getWinPlace()).isEqualTo(1);
            assertThat(entity.isWin()).isTrue();
        }
    }
}
