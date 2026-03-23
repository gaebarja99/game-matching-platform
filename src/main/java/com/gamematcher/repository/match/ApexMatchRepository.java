package com.gamematcher.repository.match;

import com.gamematcher.entity.match.apex.ApexMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApexMatchRepository extends JpaRepository<ApexMatch, Long> {
    boolean existsByUidAndMatchId(String uid, String matchId);
}
