package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolMatchParticipantRepository extends JpaRepository<LolMatchParticipant, Long> {

    Optional<LolMatchParticipant> findByMatch_MatchIdAndPuuidIgnoreCase(String matchId, String puuid);

    Optional<LolMatchParticipant> findByMatch_MatchIdAndPuuid(String matchId, String puuid);
}
