package com.gamematcher.repository;

import com.gamematcher.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    Optional<WatchHistory> findByViewerIdAndStreamId(Long viewerId, Long streamId);

    List<WatchHistory> findByViewerIdOrderByUpdatedAtDesc(Long viewerId, org.springframework.data.domain.Pageable pageable);
}
