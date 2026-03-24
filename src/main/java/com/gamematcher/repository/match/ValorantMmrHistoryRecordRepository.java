package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMmrHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantMmrHistoryRecordRepository extends JpaRepository<ValorantMmrHistoryRecord, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);

    Optional<ValorantMmrHistoryRecord> findByPuuidAndMatchId(String puuid, String matchId);
}
