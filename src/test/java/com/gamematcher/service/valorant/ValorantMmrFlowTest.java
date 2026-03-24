package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMmrApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrRecord;
import com.gamematcher.repository.match.ValorantMmrRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valorant MMR JSON → DTO → Entity(DB) 흐름 검증 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("lifetimeflow-test")
@Transactional
@Rollback
@DisplayName("Valorant MMR JSON→DTO→Entity 흐름 테스트")
class ValorantMmrFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};

    @Autowired
    private ValorantMmrService valorantMmrService;

    @Autowired
    private ValorantMmrRecordRepository repository;

    @Test
    @DisplayName("valorant_mmr_sample.json → DTO → DB 저장 후 조회 시 데이터가 정상 반영된다")
    void jsonToDtoToEntity_flow_succeeds() throws Exception {
        // 1. JSON 로드
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_mmr_sample.json"));

        // 2. JSON → DTO
        ValorantMmrApiResponse response = objectMapper.readValue(json, ValorantMmrApiResponse.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(response.getData().getName()).isEqualTo("WelcometotheShow");
        assertThat(response.getData().getTag()).isEqualTo("1111");
        assertThat(response.getData().getCurrentData()).isNotNull();
        assertThat(response.getData().getCurrentData().getCurrenttier()).isEqualTo(27);
        assertThat(response.getData().getCurrentData().getCurrentTierPatched()).isEqualTo("Radiant");
        assertThat(response.getData().getCurrentData().getElo()).isEqualTo(2697);
        assertThat(response.getData().getHighestRank()).isNotNull();
        assertThat(response.getData().getHighestRank().getTier()).isEqualTo(27);
        assertThat(response.getData().getBySeason()).isNotNull();
        assertThat(response.getData().getBySeason()).isNotEmpty();

        // 3. DTO → Entity(DB) 저장
        ValorantMmrRecord saved = valorantMmrService.saveMmr(response);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();

        // 4. DB에서 조회하여 검증
        ValorantMmrRecord found = repository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(found.getName()).isEqualTo("WelcometotheShow");
        assertThat(found.getTag()).isEqualTo("1111");
        assertThat(found.getCurrentTier()).isEqualTo(27);
        assertThat(found.getCurrentTierPatched()).isEqualTo("Radiant");
        assertThat(found.getRankingInTier()).isEqualTo(597);
        assertThat(found.getMmrChangeToLastGame()).isEqualTo(17);
        assertThat(found.getElo()).isEqualTo(2697);
        assertThat(found.getHighestTier()).isEqualTo(27);
        assertThat(found.getHighestTierPatched()).isEqualTo("Radiant");
        assertThat(found.getHighestSeason()).isEqualTo("e5a1");
        assertThat(found.getBySeasonJson()).isNotNull();
        assertThat(found.getBySeasonJson()).contains("e1a1");
        assertThat(found.getBySeasonJson()).contains("e3a2");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 puuid 재저장 시 업데이트된다")
    void saveMmr_duplicatePuuid_updates() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_mmr_sample.json"));
        ValorantMmrApiResponse response = objectMapper.readValue(json, ValorantMmrApiResponse.class);

        ValorantMmrRecord first = valorantMmrService.saveMmr(response);
        assertThat(first).isNotNull();

        ValorantMmrRecord second = valorantMmrService.saveMmr(response);
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(repository.count()).isEqualTo(1);

        ValorantMmrRecord found = repository.findByPuuid("a43bf7f7-c60a-54b6-99fc-60517d1e13e8").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCurrentTierPatched()).isEqualTo("Radiant");
    }
}
