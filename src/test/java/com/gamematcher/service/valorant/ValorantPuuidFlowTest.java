package com.gamematcher.service.valorant;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.valorant.ValorantPuuidApiResponse;
import com.gamematcher.entity.account.ValorantAccount;
import com.gamematcher.repository.account.ValorantAccountRepository;
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
 * Valorant PUUID JSON → DTO → Entity(DB) 흐름 검증 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("lifetimeflow-test")
@Transactional
@Rollback
@DisplayName("Valorant PUUID JSON→DTO→Entity 흐름 테스트")
class ValorantPuuidFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};

    @Autowired
    private ValorantAccountService valorantAccountService;

    @Autowired
    private ValorantAccountRepository repository;

    @Test
    @DisplayName("valorant_puuid_sample.json → DTO → DB 저장 후 조회 시 데이터가 정상 반영된다")
    void jsonToDtoToEntity_flow_succeeds() throws Exception {
        // 1. JSON 로드
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_puuid_sample.json"));

        // 2. JSON → DTO
        ValorantPuuidApiResponse response = objectMapper.readValue(json, ValorantPuuidApiResponse.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(response.getData().getName()).isEqualTo("WelcometotheShow");
        assertThat(response.getData().getTag()).isEqualTo("1111");
        assertThat(response.getData().getRegion()).isEqualTo("ap");
        assertThat(response.getData().getAccountLevel()).isEqualTo(515);
        assertThat(response.getData().getCard()).isNotNull();
        assertThat(response.getData().getCard().getId()).isEqualTo("a7e78a56-4557-383e-72a1-95ae97bcf2c1");

        // 3. DTO → Entity(DB) 저장
        ValorantAccount saved = valorantAccountService.saveAccount(response);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();

        // 4. DB에서 조회하여 검증
        ValorantAccount found = repository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getPuuid()).isEqualTo("a43bf7f7-c60a-54b6-99fc-60517d1e13e8");
        assertThat(found.getRegion()).isEqualTo("ap");
        assertThat(found.getAccountLevel()).isEqualTo(515);
        assertThat(found.getName()).isEqualTo("WelcometotheShow");
        assertThat(found.getTag()).isEqualTo("1111");
        assertThat(found.getCardId()).isEqualTo("a7e78a56-4557-383e-72a1-95ae97bcf2c1");
        assertThat(found.getCardSmall()).contains("smallart.png");
        assertThat(found.getCardLarge()).contains("largeart.png");
        assertThat(found.getCardWide()).contains("wideart.png");
        assertThat(found.getLastUpdate()).isEqualTo("0 minutes ago");
        assertThat(found.getLastUpdateRaw()).isEqualTo(1773207667L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 puuid 재저장 시 업데이트된다")
    void saveAccount_duplicatePuuid_updates() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_puuid_sample.json"));
        ValorantPuuidApiResponse response = objectMapper.readValue(json, ValorantPuuidApiResponse.class);

        ValorantAccount first = valorantAccountService.saveAccount(response);
        assertThat(first).isNotNull();

        // 같은 데이터로 다시 저장 (업데이트)
        ValorantAccount second = valorantAccountService.saveAccount(response);
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(repository.count()).isEqualTo(1);

        ValorantAccount found = repository.findByPuuid("a43bf7f7-c60a-54b6-99fc-60517d1e13e8").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("WelcometotheShow");
    }
}
