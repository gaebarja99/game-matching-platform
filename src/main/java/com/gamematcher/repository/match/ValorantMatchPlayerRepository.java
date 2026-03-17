package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantMatchPlayerRepository extends JpaRepository<ValorantMatchPlayer, Long> {

    Optional<ValorantMatchPlayer> findByMatch_MatchIdAndPuuid(String matchId, String puuid);
}
