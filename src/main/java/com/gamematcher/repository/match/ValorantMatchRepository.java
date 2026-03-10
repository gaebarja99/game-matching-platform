package com.gamematcher.repository.match;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamematcher.entity.match.valorant.ValorantMatch;

public interface ValorantMatchRepository extends JpaRepository<ValorantMatch, Long> {

    boolean existsByPuuidAndMatchId(String puuid, String matchId);
}
