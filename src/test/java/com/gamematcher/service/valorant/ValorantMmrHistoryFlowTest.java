package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantMmrHistoryApiResponse;
import com.gamematcher.entity.match.valorant.ValorantMmrHistoryRecord;
import com.gamematcher.repository.match.ValorantMmrHistoryRecordRepository;
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
 * Valorant MMR History JSON → DTO → Entity(DB) 흐름 검증 통합 테스트
 * puuid는 API URL에서 조회되므로 테스트에서 sample의 name/tag에 해당하는 puuid 사용
 */
@SpringBootTest
@ActiveProfiles("lifetimeflow-test")
@Transactional
@Rollback
@DisplayName("Valorant MMR History JSON→DTO→Entity 흐름 테스트")
class ValorantMmrHistoryFlowTest {

    private static final String SAMPLE_PUUID = "a43bf7f7-c60a-54b6-99fc-60517d1e13e8"; // WelcometotheShow#1111

    private final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};

    @Autowired
    private ValorantMmrHistoryService mmrHistoryService;

    @Autowired
    private ValorantMmrHistoryRecordRepository repository;

    @Test
    @DisplayName("valorant_mmr_history_sample.json → DTO → DB 저장 후 조회 시 데이터가 정상 반영된다")
    void jsonToDtoToEntity_flow_succeeds() throws Exception {
        // 1. JSON 로드
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_mmr_history_sample.json"));

        // 2. JSON → DTO
        ValorantMmrHistoryApiResponse response = objectMapper.readValue(json, ValorantMmrHistoryApiResponse.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getName()).isEqualTo("WelcometotheShow");
        assertThat(response.getTag()).isEqualTo("1111");
        assertThat(response.getData()).isNotEmpty();

        ValorantMmrHistoryApiResponse.MmrHistoryItem firstItem = response.getData().get(0);
        assertThat(firstItem.getMatchId()).isEqualTo("a96e686a-ee79-41e9-b557-f5e557ef085a");
        assertThat(firstItem.getCurrenttier()).isEqualTo(27);
        assertThat(firstItem.getCurrentTierPatched()).isEqualTo("Radiant");
        assertThat(firstItem.getElo()).isEqualTo(2697);
        assertThat(firstItem.getMap()).isNotNull();
        assertThat(firstItem.getMap().getName()).isEqualTo("Corrode");

        // 3. DTO → Entity(DB) 저장 (puuid는 API URL 컨텍스트에서 전달)
        int saved = mmrHistoryService.saveRecords(SAMPLE_PUUID, response, 3); // 처음 3건만 저장
        assertThat(saved).isEqualTo(3);

        // 4. DB에서 조회하여 검증
        ValorantMmrHistoryRecord found = repository.findByPuuidAndMatchId(SAMPLE_PUUID, "a96e686a-ee79-41e9-b557-f5e557ef085a")
                .orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getPuuid()).isEqualTo(SAMPLE_PUUID);
        assertThat(found.getMatchId()).isEqualTo("a96e686a-ee79-41e9-b557-f5e557ef085a");
        assertThat(found.getCurrentTier()).isEqualTo(27);
        assertThat(found.getCurrentTierPatched()).isEqualTo("Radiant");
        assertThat(found.getMapName()).isEqualTo("Corrode");
        assertThat(found.getMapId()).isEqualTo("1c18ab1f-420d-0d8b-71d0-77ad3c439115");
        assertThat(found.getElo()).isEqualTo(2697);
        assertThat(found.getMmrChangeToLastGame()).isEqualTo(17);
        assertThat(found.getDateRaw()).isEqualTo(1773254279L);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("saveRecords 전체 파이프라인이 정상 동작한다")
    void fullPipeline_saveRecords_succeeds() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_mmr_history_sample.json"));
        ValorantMmrHistoryApiResponse response = objectMapper.readValue(json, ValorantMmrHistoryApiResponse.class);

        int saved = mmrHistoryService.saveRecords(SAMPLE_PUUID, response, 0); // 전체 저장
        assertThat(saved).isEqualTo(response.getData().size());
        assertThat(repository.count()).isEqualTo(response.getData().size());

        // 두 번째 매치 검증
        ValorantMmrHistoryRecord record = repository.findByPuuidAndMatchId(
                SAMPLE_PUUID,
                "1116db66-663e-459e-9096-b4739f8a3c48"
        ).orElse(null);
        assertThat(record).isNotNull();
        assertThat(record.getMapName()).isEqualTo("Pearl");
        assertThat(record.getMmrChangeToLastGame()).isEqualTo(-19);
        assertThat(record.getElo()).isEqualTo(2680);
    }
}
