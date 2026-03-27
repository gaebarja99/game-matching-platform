package com.gamematcher.repository.match;

import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantMatchPlayerRepository extends JpaRepository<ValorantMatchPlayer, Long> {

    Optional<ValorantMatchPlayer> findByMatch_MatchIdAndPuuid(String matchId, String puuid);

    /** 전적 검색 API puuid와 DB 저장값 대소문자 불일치 허용 (Riot/Henrik 혼용 시) */
    Optional<ValorantMatchPlayer> findByMatch_MatchIdAndPuuidIgnoreCase(String matchId, String puuid);
}
