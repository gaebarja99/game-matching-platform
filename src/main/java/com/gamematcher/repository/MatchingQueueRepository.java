package com.gamematcher.repository;

import com.gamematcher.entity.MatchingQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingQueueRepository extends JpaRepository<MatchingQueue, Long>, MatchingQueueRepositoryCustom {

    boolean existsByUserId(Long userId);

    Optional<MatchingQueue> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
