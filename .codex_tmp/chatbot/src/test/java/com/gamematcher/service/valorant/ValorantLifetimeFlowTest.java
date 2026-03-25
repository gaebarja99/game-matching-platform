package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantLifetimeApiResponse;
import com.gamematcher.dto.valorant.ValorantLifetimeDataItem;
import com.gamematcher.entity.match.valorant.ValorantLifetimeRecord;
import com.gamematcher.repository.match.ValorantLifetimeRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON → DTO → Entity(DB) 흐름 검증 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("lifetimeflow-test")
@Transactional
@Rollback
@DisplayName("Valorant Lifetime JSON→DTO→Entity 흐름 테스트")
class ValorantLifetimeFlowTest {

    @Autowired
    private ValorantLifetimeJsonService jsonService;

    @Autowired
    private ValorantLifetimeService lifetimeService;

    @Autowired
    private ValorantLifetimeRecordRepository repository;

    @Test
    @DisplayName("valorant_lifetime_sample.json → DTO → DB 저장 후 조회 시 데이터가 정상 반영된다")
    void jsonToDtoToEntity_flow_succeeds() throws Exception {
        // 1. JSON 로드
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_lifetime_sample.json"));

        // 2. JSON → DTO
        ValorantLifetimeApiResponse response = jsonService.parseApiResponse(json);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isNotEmpty();

        List<ValorantLifetimeDataItem> dtos = jsonService.parseDataFromApiResponse(json);
        assertThat(dtos).hasSize(1);

        ValorantLifetimeDataItem dto = dtos.get(0);
        assertThat(dto.getMeta()).isNotNull();
        assertThat(dto.getMeta().getId()).isEqualTo("a96e686a-ee79-41e9-b557-f5e557ef085a");
        assertThat(dto.getStats().getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(dto.getStats().getName()).isEqualTo("WelcometotheShow");
        assertThat(dto.getStats().getKills()).isEqualTo(24);

        // 3. DTO → Entity(DB) 저장
        ValorantLifetimeRecord saved = lifetimeService.saveRecord(dto);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();

        // 4. DB에서 조회하여 검증
        ValorantLifetimeRecord found = repository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getMatchId()).isEqualTo("a96e686a-ee79-41e9-b557-f5e557ef085a");
        assertThat(found.getMapName()).isEqualTo("Corrode");
        assertThat(found.getMode()).isEqualTo("Competitive");
        assertThat(found.getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(found.getName()).isEqualTo("WelcometotheShow");
        assertThat(found.getTag()).isEqualTo("1111");
        assertThat(found.getCharacterName()).isEqualTo("Jett");
        assertThat(found.getKills()).isEqualTo(24);
        assertThat(found.getDeaths()).isEqualTo(15);
        assertThat(found.getAssists()).isEqualTo(5);
        assertThat(found.getRedRounds()).isEqualTo(10);
        assertThat(found.getBlueRounds()).isEqualTo(13);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("parseDataFromApiResponse → saveRecords 전체 파이프라인이 정상 동작한다")
    void fullPipeline_parseAndSaveRecords_succeeds() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_lifetime_sample.json"));

        List<ValorantLifetimeDataItem> dtos = jsonService.parseDataFromApiResponse(json);
        int saved = lifetimeService.saveRecords(dtos, 0);

        assertThat(saved).isEqualTo(1);
        assertThat(repository.count()).isGreaterThanOrEqualTo(1);

        ValorantLifetimeRecord record = repository.findByMatchIdAndPuuid(
                "a96e686a-ee79-41e9-b557-f5e557ef085a",
                "a43bf7f7-c60a-54b6-99fc-60517d1e13e8"
        ).orElse(null);
        assertThat(record).isNotNull();
        assertThat(record.getDamageMade()).isEqualTo(4520);
        assertThat(record.getDamageReceived()).isEqualTo(2684);
    }
}
