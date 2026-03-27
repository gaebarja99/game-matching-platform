package com.gamematcher.integration;

import com.gamematcher.dto.pubg.*;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import com.gamematcher.entity.match.pubg.PubgPlayerRank;
import com.gamematcher.entity.match.pubg.PubgSeason;
import com.gamematcher.mapper.PubgMatchMapper;
import com.gamematcher.mapper.PubgRankMapper;
import com.gamematcher.mapper.PubgSeasonMapper;
import com.gamematcher.service.pubg.PubgJsonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUBG 데이터 흐름 통합 테스트: JSON → DTO → Entity
 * 매치/플레이어/텔레메트리 API 응답이 전체 파이프라인에서 올바르게 변환되는지 검증
 */
@DisplayName("PUBG JSON→DTO→Entity 흐름 통합 테스트")
class PubgDataFlowIntegrationTest {

    private static final String SAMPLES_BASE = "src/test/resources/samples/pubg";

    private final PubgJsonService jsonService = new PubgJsonService();
    private final PubgMatchMapper mapper = new PubgMatchMapper();
    private final PubgSeasonMapper seasonMapper = new PubgSeasonMapper();
    private final PubgRankMapper rankMapper = new PubgRankMapper();

    @Test
    @DisplayName("전체 흐름 검증: 모든 PUBG 샘플 JSON → DTO → Entity 정상 동작")
    void 전체_JSON_DTO_Entity_흐름_검증() throws Exception {
        // 1) 매치: JSON → DTO → Entity
        String matchJson = Files.readString(Paths.get(SAMPLES_BASE, "pubg_match_sample.json"));
        PubgMatchApiResponse matchDto = jsonService.parseMatchResponse(matchJson);
        PubgMatch matchEntity = mapper.toEntity(matchDto);
        assertThat(matchEntity).isNotNull();
        assertThat(matchEntity.getMatchId()).isEqualTo(matchDto.getData().getId());
        assertThat(matchEntity.getGameMode()).isEqualTo(matchDto.getData().getAttributes().getGameMode());
        assertThat(matchEntity.getParticipants()).isNotEmpty();

        // 2) 플레이어: JSON → DTO
        String playerJson = Files.readString(Paths.get(SAMPLES_BASE, "pubg_player_sample.json"));
        PubgPlayerApiResponse playerDto = jsonService.parsePlayerResponse(playerJson);
        assertThat(playerDto.getData()).hasSize(1);
        assertThat(playerDto.getData().get(0).getRelationships().getMatchIds()).isNotEmpty();

        // 3) 시즌: JSON → DTO → Entity
        String seasonsJson = Files.readString(Paths.get(SAMPLES_BASE, "pubg_seasons_sample.json"));
        PubgSeasonsApiResponse seasonsDto = jsonService.parseSeasonsResponse(seasonsJson);
        List<PubgSeason> seasonEntities = seasonMapper.toEntities(seasonsDto, "steam");
        assertThat(seasonEntities).isNotEmpty();
        assertThat(seasonEntities).hasSize(seasonsDto.getData().size());

        // 4) 랭크: JSON → DTO → Entity
        String rankJson = Files.readString(Paths.get(SAMPLES_BASE, "pubg_season_rank_sample.json"));
        PubgRankedPlayerStatsApiResponse rankDto = jsonService.parseRankedPlayerStatsResponse(rankJson);
        String playerId = rankDto.getData().getRelationships().getPlayer().getData().getId();
        String seasonId = rankDto.getData().getRelationships().getSeason().getData().getId();
        List<PubgPlayerRank> rankEntities = rankMapper.toEntities(rankDto, playerId, seasonId, "steam");
        assertThat(rankEntities).hasSize(2);
        assertThat(rankMapper.toTierDisplayString(rankDto)).isEqualTo("Survivor 1");

        // 5) 텔레메트리: JSON → DTO
        String telemetryJson = Files.readString(Paths.get(SAMPLES_BASE, "pubg_match_included_sample.json"));
        var telemetryEvents = jsonService.parseTelemetryResponse(telemetryJson);
        assertThat(telemetryEvents).isNotEmpty();
        assertThat(telemetryEvents.get(0).getType()).isEqualTo("LogMatchDefinition");
    }

    @Nested
    @DisplayName("매치 API: JSON → DTO → Entity")
    class MatchApiFlow {

        @Test
        @DisplayName("전체 흐름: pubg_match_sample.json → PubgMatchApiResponse → PubgMatch")
        void json_to_dto_to_entity_전체흐름_검증() throws Exception {
            // 1) JSON 로드
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_match_sample.json"));
            assertThat(json).isNotBlank();

            // 2) JSON → DTO (PubgJsonService)
            PubgMatchApiResponse dto = jsonService.parseMatchResponse(json);
            assertThat(dto).isNotNull();
            assertThat(dto.getData()).isNotNull();
            assertThat(dto.getIncluded()).isNotEmpty();

            // 3) DTO → Entity (PubgMatchMapper)
            PubgMatch entity = mapper.toEntity(dto);
            assertThat(entity).isNotNull();

            // 4) 매치 메타데이터 검증 (DTO와 일치)
            assertThat(entity.getMatchId()).isEqualTo(dto.getData().getId());
            assertThat(entity.getGameMode()).isEqualTo(dto.getData().getAttributes().getGameMode());
            assertThat(entity.getMapName()).isEqualTo(dto.getData().getAttributes().getMapName());
            assertThat(entity.getTitleId()).isEqualTo(dto.getData().getAttributes().getTitleId());
            assertThat(entity.getShardId()).isEqualTo(dto.getData().getAttributes().getShardId());
            assertThat(entity.getDuration()).isEqualTo(dto.getData().getAttributes().getDuration());
            assertThat(entity.getCreatedAt()).isEqualTo(dto.getData().getAttributes().getCreatedAt());
            assertThat(entity.getIsCustomMatch()).isEqualTo(dto.getData().getAttributes().getIsCustomMatch());
            assertThat(entity.getSeasonState()).isEqualTo(dto.getData().getAttributes().getSeasonState());
            assertThat(entity.getMatchType()).isEqualTo(dto.getData().getAttributes().getMatchType());

            // 5) 참가자(Entity) 검증 - participant만 변환 (asset, roster 제외)
            long participantCount = dto.getIncluded().stream()
                    .filter(PubgParticipantIncludedDto.class::isInstance)
                    .count();
            assertThat(entity.getParticipants()).hasSize((int) participantCount);

            // 1등 참가자 검증 (winPlace=1)
            PubgMatchParticipant winner = entity.getParticipants().stream()
                    .filter(p -> Integer.valueOf(1).equals(p.getWinPlace()))
                    .findFirst()
                    .orElseThrow();
            assertThat(winner.isWin()).isTrue();
            assertThat(winner.getParticipantId()).isNotBlank();
            assertThat(winner.getPlayerId()).isNotBlank();
            assertThat(winner.getName()).isNotBlank();
        }

        @Test
        @DisplayName("매치 JSON → DTO 단계 검증")
        void match_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_match_sample.json"));
            PubgMatchApiResponse dto = jsonService.parseMatchResponse(json);

            assertThat(dto.getData().getAttributes().getGameMode()).isNotBlank();
            assertThat(dto.getIncluded().stream()
                    .filter(PubgParticipantIncludedDto.class::isInstance)
                    .count()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("플레이어 API: JSON → DTO")
    class PlayerApiFlow {

        @Test
        @DisplayName("플레이어 JSON → PubgPlayerApiResponse (매치 ID 목록 포함)")
        void player_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_player_sample.json"));
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
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_seasons_sample.json"));
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
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_seasons_sample.json"));
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
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_match_included_sample.json"));
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
    @DisplayName("랭크 API: JSON → DTO → Entity")
    class RankApiFlow {

        @Test
        @DisplayName("전체 흐름: pubg_season_rank_sample.json → PubgRankedPlayerStatsApiResponse → PubgPlayerRank")
        void json_to_dto_to_entity_전체흐름_검증() throws Exception {
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_season_rank_sample.json"));
            assertThat(json).isNotBlank();

            PubgRankedPlayerStatsApiResponse dto = jsonService.parseRankedPlayerStatsResponse(json);
            assertThat(dto).isNotNull();
            assertThat(dto.getData()).isNotNull();
            assertThat(dto.getData().getType()).isEqualTo("rankedplayerstats");
            assertThat(dto.getData().getAttributes().getRankedGameModeStats()).containsKeys("squad", "squad-fpp");

            PubgRankedGameModeStatsDto squadFpp = dto.getData().getAttributes().getRankedGameModeStats().get("squad-fpp");
            assertThat(squadFpp.getCurrentTier().getTier()).isEqualTo("Survivor");
            assertThat(squadFpp.getCurrentTier().getSubTier()).isEqualTo("1");
            assertThat(squadFpp.getCurrentRankPoint()).isEqualTo(6235);
            assertThat(squadFpp.getWins()).isEqualTo(71);
            assertThat(squadFpp.getKills()).isEqualTo(644);

            String playerId = dto.getData().getRelationships().getPlayer().getData().getId();
            String seasonId = dto.getData().getRelationships().getSeason().getData().getId();
            assertThat(playerId).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
            assertThat(seasonId).isEqualTo("division.bro.official.pc-2018-40");

            String tierDisplay = rankMapper.toTierDisplayString(dto);
            assertThat(tierDisplay).isEqualTo("Survivor 1");

            List<PubgPlayerRank> entities = rankMapper.toEntities(dto, playerId, seasonId, "steam");
            assertThat(entities).hasSize(2);
            PubgPlayerRank squadFppEntity = entities.stream()
                    .filter(e -> "squad-fpp".equals(e.getGameMode()))
                    .findFirst().orElseThrow();
            assertThat(squadFppEntity.getPlayerId()).isEqualTo(playerId);
            assertThat(squadFppEntity.getSeasonId()).isEqualTo(seasonId);
            assertThat(squadFppEntity.getPlatform()).isEqualTo("steam");
            assertThat(squadFppEntity.getCurrentTier()).isEqualTo("Survivor");
            assertThat(squadFppEntity.getSubTier()).isEqualTo("1");
            assertThat(squadFppEntity.getCurrentRankPoint()).isEqualTo(6235);
            assertThat(squadFppEntity.getWins()).isEqualTo(71);
            assertThat(squadFppEntity.getKills()).isEqualTo(644);
        }

        @Test
        @DisplayName("랭크 JSON → DTO 단계 검증")
        void rank_json_to_dto_검증() throws Exception {
            String json = Files.readString(Paths.get(SAMPLES_BASE, "pubg_season_rank_sample.json"));
            PubgRankedPlayerStatsApiResponse dto = jsonService.parseRankedPlayerStatsResponse(json);

            assertThat(dto.getData().getAttributes().getRankedGameModeStats().get("squad").getRoundsPlayed())
                    .isEqualTo(97);
            assertThat(dto.getLinks()).isNotNull();
            assertThat(dto.getLinks().getSelf()).contains("ranked");
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
