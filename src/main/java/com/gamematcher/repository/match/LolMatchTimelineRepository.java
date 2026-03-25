package com.gamematcher.repository.match;

import com.gamematcher.entity.match.lol.LolMatchTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolMatchTimelineRepository extends JpaRepository<LolMatchTimeline, Long> {
}
