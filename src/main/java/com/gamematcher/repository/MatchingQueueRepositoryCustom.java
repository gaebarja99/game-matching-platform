package com.gamematcher.repository;

import com.gamematcher.constant.LolTier;
import com.gamematcher.entity.MatchingQueue;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchingQueueRepositoryCustom {

    List<MatchingQueue> findWaitingByGameAndTierOrderByCreatedAtAsc(String gameName, LolTier tier);

    /** createdAt 이 시각 이전인 대기 행 (60초 이상 대기 등) */
    List<MatchingQueue> findWaitingByGameAndCreatedAtBeforeOrderByCreatedAtAsc(String gameName, LocalDateTime beforeInclusive);
}
