package com.gamematcher.repository.match;

import com.gamematcher.entity.SteamOwnedGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SteamOwnedGameRepository extends JpaRepository<SteamOwnedGame, Long> {

    boolean existsBySteamIdAndAppId(String steamId, String appId);

    List<SteamOwnedGame> findBySteamIdOrderByPlaytimeMinutesDesc(String steamId);
}
