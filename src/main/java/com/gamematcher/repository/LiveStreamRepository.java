package com.gamematcher.repository;

import com.gamematcher.constant.StreamStatus;
import com.gamematcher.entity.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

    Optional<LiveStream> findByStreamKey(String streamKey);

    List<LiveStream> findByStatusOrderByStartedAtDesc(StreamStatus status);

    /** 진행 중인 라이브만 (status=LIVE 이어도 endedAt이 있으면 제외 — 데이터 불일치 방어) */
    List<LiveStream> findByStatusAndEndedAtIsNullOrderByStartedAtDesc(StreamStatus status);

    List<LiveStream> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 계정당 하나의 스트림(채널)을 유지할 때 사용. 가장 먼저 만든 스트림 1개 */
    Optional<LiveStream> findFirstByUserIdOrderByIdAsc(Long userId);

    List<LiveStream> findTop20ByOrderByCreatedAtDesc();

    /** 스트리밍을 한 번이라도 시작한 방송만 (LIVE 또는 ENDED) - 최근 방송 목록용 */
    List<LiveStream> findTop20ByStatusInOrderByStartedAtDesc(List<StreamStatus> status);

    /** 특정 사용자들의 라이브 방송 */
    List<LiveStream> findByUserIdInAndStatusOrderByStartedAtDesc(List<Long> userIds, StreamStatus status);

    List<LiveStream> findByUserIdInAndStatusAndEndedAtIsNullOrderByStartedAtDesc(List<Long> userIds, StreamStatus status);

    /** 특정 사용자들의 최근 방송 (LIVE/ENDED, startedAt 기준) */
    List<LiveStream> findTop20ByUserIdInAndStatusInOrderByStartedAtDesc(List<Long> userIds, List<StreamStatus> status);
}
