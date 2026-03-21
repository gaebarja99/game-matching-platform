package com.gamematcher.repository.match;

import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PubgTelemetryEventRepository extends JpaRepository<PubgTelemetryEvent, Long> {

    void deleteByMatchMatchId(String matchId);

    List<PubgTelemetryEvent> findByMatchMatchIdAndAccountIdOrderByEventTimestampAsc(
            String matchId,
            String accountId
    );

    List<PubgTelemetryEvent> findTop1000ByMatchMatchIdOrderByEventSequenceAsc(String matchId);
}

