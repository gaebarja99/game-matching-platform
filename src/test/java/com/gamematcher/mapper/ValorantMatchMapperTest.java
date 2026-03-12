package com.gamematcher.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.entity.match.valorant.ValorantMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValorantMatchMapper 테스트")
class ValorantMatchMapperTest {

    private ValorantMatchMapper mapper;
    private ValorantMatchDetailDto sourceDto;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ValorantMatchMapper();
        String jsonPath = "src/test/resources/samples/valorant/valorant_match_sample.json";
        String json = Files.readString(Paths.get(jsonPath));
        ValorantMatchApiResponse response = new ObjectMapper().readValue(json, ValorantMatchApiResponse.class);
        sourceDto = response.getData().get(0);
    }

    @Nested
    @DisplayName("toEntity - DTO → Entity 변환")
    class ToEntityTest {

        @Test
        @DisplayName("null DTO가 주어지면 null을 반환한다")
        void toEntity_null_returnsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("매치 상세 DTO를 엔티티로 변환한다")
        void toEntity_validDto_convertsToEntity() {
            ValorantMatch entity = mapper.toEntity(sourceDto);

            assertThat(entity).isNotNull();
            // metadata
            assertThat(entity.getMatchId()).isEqualTo(sourceDto.getMetadata().getMatchId());
            assertThat(entity.getMap()).isEqualTo(sourceDto.getMetadata().getMap());
            assertThat(entity.getMode()).isEqualTo(sourceDto.getMetadata().getMode());
            assertThat(entity.getGameLength()).isEqualTo(sourceDto.getMetadata().getGameLength());
            assertThat(entity.getRoundsPlayed()).isEqualTo(sourceDto.getMetadata().getRoundsPlayed());

            // teams
            assertThat(entity.getRedRoundsWon()).isNotNull();
            assertThat(entity.getBlueRoundsWon()).isNotNull();

            // players, rounds, kills
            assertThat(entity.getPlayers()).hasSize(sourceDto.getPlayers().getAllPlayers().size());
            assertThat(entity.getRounds()).hasSize(sourceDto.getRounds().size());
            assertThat(entity.getKillEvents()).hasSize(sourceDto.getKills().size());
        }

        @Test
        @DisplayName("플레이어 정보가 올바르게 매핑된다")
        void toEntity_playerData_mappedCorrectly() {
            ValorantMatch entity = mapper.toEntity(sourceDto);
            var firstSourcePlayer = sourceDto.getPlayers().getAllPlayers().get(0);

            var firstPlayer = entity.getPlayers().stream()
                    .filter(p -> firstSourcePlayer.getName().equals(p.getName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(firstPlayer.getPuuid()).isEqualTo(firstSourcePlayer.getPuuid());
            assertThat(firstPlayer.getTag()).isEqualTo(firstSourcePlayer.getTag());
            assertThat(firstPlayer.getAgent()).isEqualTo(firstSourcePlayer.getAgent());
            assertThat(firstPlayer.getCurrentTierPatched()).isEqualTo(firstSourcePlayer.getCurrentTierPatched());
        }

        @Test
        @DisplayName("킬 이벤트가 올바르게 매핑된다")
        void toEntity_killEvents_mappedCorrectly() {
            ValorantMatch entity = mapper.toEntity(sourceDto);

            assertThat(entity.getKillEvents()).isNotEmpty();
            var firstKill = entity.getKillEvents().get(0);
            assertThat(firstKill.getKillerPuuid()).isNotNull();
            assertThat(firstKill.getVictimPuuid()).isNotNull();
            assertThat(firstKill.getRoundNumber()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toDto - Entity → DTO 변환")
    class ToDtoTest {

        @Test
        @DisplayName("null 엔티티가 주어지면 null을 반환한다")
        void toDto_null_returnsNull() {
            assertThat(mapper.toDto(null)).isNull();
        }

        @Test
        @DisplayName("엔티티를 DTO로 변환한다")
        void toDto_validEntity_convertsToDto() {
            ValorantMatch entity = mapper.toEntity(sourceDto);
            ValorantMatchDetailDto dto = mapper.toDto(entity);

            assertThat(dto).isNotNull();
            assertThat(dto.isAvailable()).isTrue();

            // metadata
            assertThat(dto.getMetadata()).isNotNull();
            assertThat(dto.getMetadata().getMatchId()).isEqualTo(sourceDto.getMetadata().getMatchId());
            assertThat(dto.getMetadata().getMap()).isEqualTo(sourceDto.getMetadata().getMap());

            // teams, players, rounds, kills
            assertThat(dto.getTeams()).isNotNull();
            assertThat(dto.getPlayers()).isNotNull();
            assertThat(dto.getPlayers().getAllPlayers()).hasSize(sourceDto.getPlayers().getAllPlayers().size());
            assertThat(dto.getRounds()).hasSize(sourceDto.getRounds().size());
            assertThat(dto.getKills()).hasSize(sourceDto.getKills().size());
        }
    }

    @Nested
    @DisplayName("round-trip - DTO → Entity → DTO")
    class RoundTripTest {

        @Test
        @DisplayName("변환 후 핵심 데이터가 유지된다")
        void roundTrip_criticalData_preserved() {
            ValorantMatch entity = mapper.toEntity(sourceDto);
            ValorantMatchDetailDto resultDto = mapper.toDto(entity);

            // metadata
            assertThat(resultDto.getMetadata().getMatchId()).isEqualTo(sourceDto.getMetadata().getMatchId());
            assertThat(resultDto.getMetadata().getMap()).isEqualTo(sourceDto.getMetadata().getMap());
            assertThat(resultDto.getMetadata().getMode()).isEqualTo(sourceDto.getMetadata().getMode());
            assertThat(resultDto.getMetadata().getRoundsPlayed()).isEqualTo(sourceDto.getMetadata().getRoundsPlayed());

            // counts
            assertThat(resultDto.getPlayers().getAllPlayers()).hasSize(sourceDto.getPlayers().getAllPlayers().size());
            assertThat(resultDto.getRounds()).hasSize(sourceDto.getRounds().size());
            assertThat(resultDto.getKills()).hasSize(sourceDto.getKills().size());

            // teams
            assertThat(resultDto.getTeams().getRed().getRoundsWon())
                    .isEqualTo(sourceDto.getTeams().getRed().getRoundsWon());
            assertThat(resultDto.getTeams().getBlue().getRoundsWon())
                    .isEqualTo(sourceDto.getTeams().getBlue().getRoundsWon());
        }

        @Test
        @DisplayName("플레이어 puuid와 팀 정보가 유지된다")
        void roundTrip_playerIdentities_preserved() {
            ValorantMatch entity = mapper.toEntity(sourceDto);
            ValorantMatchDetailDto resultDto = mapper.toDto(entity);

            var sourcePuuds = sourceDto.getPlayers().getAllPlayers().stream()
                    .map(p -> p.getPuuid() + "|" + p.getTeam())
                    .toList();
            var resultPuuds = resultDto.getPlayers().getAllPlayers().stream()
                    .map(p -> p.getPuuid() + "|" + p.getTeam())
                    .toList();

            assertThat(resultPuuds).containsExactlyInAnyOrderElementsOf(sourcePuuds);
        }

        @Test
        @DisplayName("라운드 수와 킬 이벤트 수가 유지된다")
        void roundTrip_roundsAndKills_preserved() {
            ValorantMatch entity = mapper.toEntity(sourceDto);
            ValorantMatchDetailDto resultDto = mapper.toDto(entity);

            assertThat(resultDto.getRounds().size()).isEqualTo(sourceDto.getRounds().size());
            assertThat(resultDto.getKills().size()).isEqualTo(sourceDto.getKills().size());
        }
    }
}
