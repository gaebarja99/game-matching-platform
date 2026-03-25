package com.gamematcher.service.valorant;

import com.gamematcher.dto.ai.evaluation.ValorantPlayerMatchStatsDTO;
import com.gamematcher.dto.ai.evaluation.ValorantRoundStatsDTO;
import com.gamematcher.mapper.ValorantMatchMapper;
import com.gamematcher.mapper.ValorantMatchStatsMapper;
import com.gamematcher.mapper.ValorantRoundStatsMapper;
import com.gamematcher.service.ai.score.KillContextExtractor;
import com.gamematcher.service.ai.score.RoundScoreInputBuilder;
import com.gamematcher.service.ai.score.ValorantRoundScoreEngine;
import com.gamematcher.service.ai.score.ValorantScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON → DTO → Entity → 스탯 변환 흐름 검증.
 * 데이터를 꺼내와서 스탯으로 변환하는 작업이 제대로 되는지 검사.
 */
@DisplayName("Valorant 매치 데이터 추출 → 스탯 변환 흐름")
class ValorantMatchStatsFlowTest {

    private ValorantMatchJsonService jsonService;
    private ValorantMatchMapper matchMapper;
    private ValorantMatchStatsMapper statsMapper;

    @BeforeEach
    void setUp() {
        jsonService = new ValorantMatchJsonService();
        matchMapper = new ValorantMatchMapper();
        var roundStatsMapper = new ValorantRoundStatsMapper();
        var scoreService = new ValorantScoreService(
                new KillContextExtractor(),
                new ValorantRoundScoreEngine(),
                new RoundScoreInputBuilder()
        );
        statsMapper = new ValorantMatchStatsMapper(roundStatsMapper, scoreService);
    }

    @Test
    @DisplayName("JSON → DTO → Entity → PlayerMatchStats 정상 변환")
    void jsonToEntityToPlayerMatchStats_flow_succeeds() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"));

        // 1. JSON → DTO (data가 객체/배열 모두 지원)
        var matchDto = jsonService.parseFirstMatch(json);
        assertThat(matchDto).isNotNull();
        assertThat(matchDto.getMetadata()).isNotNull();
        assertThat(matchDto.getPlayers()).isNotNull();
        assertThat(matchDto.getRounds()).isNotEmpty();
        assertThat(matchDto.getKills()).isNotEmpty();

        // 2. DTO → Entity
        var entity = matchMapper.toEntity(matchDto);
        assertThat(entity).isNotNull();
        assertThat(entity.getMatchId()).isEqualTo(matchDto.getMetadata().getMatchId());
        assertThat(entity.getPlayers()).hasSize(matchDto.getPlayers().getAllPlayers().size());
        assertThat(entity.getRounds()).hasSize(matchDto.getRounds().size());
        assertThat(entity.getKillEvents()).hasSize(matchDto.getKills().size());

        // 3. Entity → 플레이어별 스탯 DTO
        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);
        assertThat(playerStats).isNotEmpty();
        assertThat(playerStats).hasSize(entity.getPlayers().size());

        // 4. 라운드 스탯 및 damageToEliminated, myDamagePerKill 검증
        for (ValorantPlayerMatchStatsDTO ps : playerStats) {
            assertThat(ps.getPlayerPuuid()).isNotBlank();
            assertThat(ps.getMatchStats()).isNotNull();
            assertThat(ps.getMatchStats().getKills()).isGreaterThanOrEqualTo(0);
            assertThat(ps.getMatchStats().getDeaths()).isGreaterThanOrEqualTo(0);
            assertThat(ps.getRoundStats()).isNotNull();

            for (ValorantRoundStatsDTO rs : ps.getRoundStats()) {
                assertThat(rs.getRoundIndex()).isGreaterThanOrEqualTo(0);
                assertThat(rs.getPlayerPuuid()).isEqualTo(ps.getPlayerPuuid());
                assertThat(rs.getDamageToEliminated()).isNotNull();
                assertThat(rs.getMyDamagePerKill()).isNotNull();
                assertThat(rs.getRoundContributionScore()).as("roundContributionScore는 점수 엔진이 설정").isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    @DisplayName("라운드 스탯에 damageToEliminated, myDamagePerKill이 채워진다")
    void roundStats_containDamageMaps() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"));
        var matchDto = jsonService.parseFirstMatch(json);
        var entity = matchMapper.toEntity(matchDto);
        List<ValorantPlayerMatchStatsDTO> playerStats = statsMapper.toPlayerMatchStatsDtos(entity);

        // 킬이 있는 라운드에서 damageToEliminated 또는 myDamagePerKill이 비어있지 않은 항목이 있어야 함
        boolean hasNonEmptyDamageMap = false;
        for (ValorantPlayerMatchStatsDTO ps : playerStats) {
            for (ValorantRoundStatsDTO rs : ps.getRoundStats()) {
                if ((rs.getDamageToEliminated() != null && !rs.getDamageToEliminated().isEmpty())
                        || (rs.getMyDamagePerKill() != null && !rs.getMyDamagePerKill().isEmpty())) {
                    hasNonEmptyDamageMap = true;
                    break;
                }
            }
        }
        assertThat(hasNonEmptyDamageMap).as("킬/데미지가 있는 라운드에서 damageToEliminated 또는 myDamagePerKill이 채워져야 함").isTrue();
    }
}
