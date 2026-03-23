package com.gamematcher.repository.account;

import com.gamematcher.entity.account.LolSummonerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolSummonerProfileRepository extends JpaRepository<LolSummonerProfile, Long> {

    boolean existsByPuuid(String puuid);

    Optional<LolSummonerProfile> findByPuuid(String puuid);
}
