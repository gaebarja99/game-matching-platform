package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMmrRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantMmrRecordRepository extends JpaRepository<ValorantMmrRecord, Long> {

    boolean existsByPuuid(String puuid);

    Optional<ValorantMmrRecord> findByPuuid(String puuid);
}
