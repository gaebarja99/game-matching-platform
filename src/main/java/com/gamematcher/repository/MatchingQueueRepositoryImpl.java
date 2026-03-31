package com.gamematcher.repository;

import com.gamematcher.constant.LolTier;
import com.gamematcher.entity.MatchingQueue;
import com.gamematcher.entity.QMatchingQueue;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MatchingQueueRepositoryImpl implements MatchingQueueRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MatchingQueueRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MatchingQueue> findWaitingByGameAndTierOrderByCreatedAtAsc(String gameName, LolTier tier) {
        QMatchingQueue q = QMatchingQueue.matchingQueue;
        return queryFactory
                .selectFrom(q)
                .where(
                        q.gameName.eq(gameName)
                                .and(q.matched.eq(false))
                                .and(q.tier.eq(tier))
                )
                .orderBy(q.createdAt.asc())
                .fetch();
    }

    @Override
    public List<MatchingQueue> findWaitingByGameAndCreatedAtBeforeOrderByCreatedAtAsc(String gameName, LocalDateTime beforeInclusive) {
        QMatchingQueue q = QMatchingQueue.matchingQueue;
        return queryFactory
                .selectFrom(q)
                .where(
                        q.gameName.eq(gameName)
                                .and(q.matched.eq(false))
                                .and(q.createdAt.loe(beforeInclusive))
                )
                .orderBy(q.createdAt.asc())
                .fetch();
    }
}
