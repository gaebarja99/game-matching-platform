package com.gamematcher.service.pubg;

import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import com.gamematcher.mapper.PubgMatchMapper;
import com.gamematcher.repository.match.PubgMatchRepository;
import com.gamematcher.repository.match.PubgTelemetryEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PubgTelemetrySaveServiceTest {

    private static final Path MATCH_JSON = Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json");
    private static final Path TELEMETRY_JSON = Paths.get("src/test/resources/samples/pubg/pubg_match_included_sample.json");

    @Autowired PubgJsonService jsonService;
    @Autowired PubgMatchMapper matchMapper;
    @Autowired PubgMatchRepository matchRepository;
    @Autowired PubgTelemetryEventRepository telemetryEventRepository;
    @Autowired PubgTelemetrySaveService saveService;

    @Test
    @DisplayName("스트리밍 PUBG 텔레메트리 → 통합 1테이블 저장")
    void save_first_200_events() throws Exception {
        String matchJson = Files.readString(MATCH_JSON);
        PubgMatchApiResponse matchDto = jsonService.parseMatchResponse(matchJson);

        PubgMatch matchEntity = matchMapper.toEntity(matchDto);
        if (matchEntity == null) {
            throw new IllegalStateException("PubgMatchMapper가 null을 반환했습니다.");
        }
        matchRepository.save(matchEntity);

        int saved = saveService.saveTelemetryForMatchId(
                matchEntity.getMatchId(),
                TELEMETRY_JSON,
                200
        );

        assertThat(saved).isGreaterThan(0);
        assertThat(saved).isLessThanOrEqualTo(200);

        List<PubgTelemetryEvent> events = telemetryEventRepository
                .findTop1000ByMatchMatchIdOrderByEventSequenceAsc(matchEntity.getMatchId());
        assertThat(events).isNotEmpty();

        // 배열 0번째 이벤트는 LogMatchDefinition이어야 함
        assertThat(events.get(0).getEventType()).isEqualTo("LogMatchDefinition");

        boolean hasEquip = events.stream().anyMatch(e -> "LogItemEquip".equals(e.getEventType()));
        assertThat(hasEquip).isTrue();

        PubgTelemetryEvent equip = events.stream()
                .filter(e -> "LogItemEquip".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertThat(equip.getAccountId()).isNotBlank();
        assertThat(equip.getItemId()).isNotBlank();
        assertThat(equip.getItemCategory()).isNotBlank();
    }
}

