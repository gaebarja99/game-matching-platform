package com.gamematcher.service.pubg;

import com.gamematcher.dto.pubg.PubgTelemetryEventRowDto;
import com.gamematcher.entity.match.pubg.PubgMatch;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import com.gamematcher.repository.match.PubgMatchRepository;
import com.gamematcher.repository.match.PubgTelemetryEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PUBG 텔레메트리를 통합 1테이블 엔티티({@link PubgTelemetryEvent})로 저장합니다.
 */
@Service
public class PubgTelemetrySaveService {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final PubgTelemetryEventRepository telemetryEventRepository;
    private final PubgMatchRepository pubgMatchRepository;
    private final PubgTelemetryStreamExtractor extractor;

    public PubgTelemetrySaveService(
            PubgTelemetryEventRepository telemetryEventRepository,
            PubgMatchRepository pubgMatchRepository,
            PubgTelemetryStreamExtractor extractor
    ) {
        this.telemetryEventRepository = telemetryEventRepository;
        this.pubgMatchRepository = pubgMatchRepository;
        this.extractor = extractor;
    }

    /**
     * matchId에 해당하는 pubg_match가 이미 존재한다고 가정하고,
     * 텔레메트리 JSON을 읽어 pubg_telemetry_event에 저장합니다.
     *
     * <p>중복 방지를 위해 matchId 기준으로 먼저 기존 텔레메트리 이벤트를 삭제합니다.</p>
     *
     * @param maxEvents 테스트/샘플 목적의 상한. maxEvents <= 0이면 제한 없음
     * @return 저장된 이벤트 수
     */
    @Transactional
    public int saveTelemetryForMatchId(String matchId, Path telemetryJsonPath, int maxEvents) throws IOException {
        PubgMatch match = pubgMatchRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치 없음: " + matchId));

        telemetryEventRepository.deleteByMatchMatchId(matchId);

        try (InputStream in = Files.newInputStream(telemetryJsonPath)) {
            return saveTelemetryFromStream(match, in, maxEvents);
        }
    }

    private int saveTelemetryFromStream(PubgMatch match, InputStream in, int maxEvents) throws IOException {
        List<PubgTelemetryEvent> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        AtomicInteger saved = new AtomicInteger(0);

        extractor.iterateEvents(in, maxEvents, row -> {
            batch.add(toEntity(match, row));
            if (batch.size() >= DEFAULT_BATCH_SIZE) {
                telemetryEventRepository.saveAll(batch);
                saved.addAndGet(batch.size());
                batch.clear();
            }
        });

        if (!batch.isEmpty()) {
            telemetryEventRepository.saveAll(batch);
            saved.addAndGet(batch.size());
        }

        return saved.get();
    }

    private PubgTelemetryEvent toEntity(PubgMatch match, PubgTelemetryEventRowDto row) {
        PubgTelemetryEvent e = new PubgTelemetryEvent();
        e.setMatch(match);
        e.setEventSequence(row.getEventSequence());
        e.setEventType(row.getEventType());
        e.setEventTimestamp(row.getEventTimestamp());
        e.setAccountId(row.getAccountId());
        e.setTeamId(row.getTeamId());
        e.setLocationX(row.getLocationX());
        e.setLocationY(row.getLocationY());
        e.setLocationZ(row.getLocationZ());
        e.setIsInBlueZone(row.getIsInBlueZone());
        e.setItemId(row.getItemId());
        e.setItemCategory(row.getItemCategory());
        e.setPayloadJson(row.getPayloadJson());
        return e;
    }
}

