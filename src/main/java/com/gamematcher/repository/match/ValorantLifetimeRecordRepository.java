package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantLifetimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantLifetimeRecordRepository extends JpaRepository<ValorantLifetimeRecord, Long> {

    boolean existsByMatchIdAndPuuid(String matchId, String puuid);

    Optional<ValorantLifetimeRecord> findByMatchIdAndPuuid(String matchId, String puuid);
}
