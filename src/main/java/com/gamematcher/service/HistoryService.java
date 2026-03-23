package com.gamematcher.service;

import com.gamematcher.constant.StreamStatus;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.WatchHistory;
import com.gamematcher.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    /** 시청 기록 추가(누적). 같은 방송 여러 번 시청 시 합산 */
    @Transactional
    public void addWatchTime(Long viewerId, Long streamId, long addSeconds) {
        if (viewerId == null || streamId == null || addSeconds <= 0) return;
        Optional<WatchHistory> opt = watchHistoryRepository.findByViewerIdAndStreamId(viewerId, streamId);
        WatchHistory wh;
        if (opt.isPresent()) {
            wh = opt.get();
            wh.setWatchSeconds(wh.getWatchSeconds() + addSeconds);
        } else {
            wh = new WatchHistory();
            wh.setViewerId(viewerId);
            wh.setStreamId(streamId);
            wh.setWatchSeconds(addSeconds);
        }
        watchHistoryRepository.save(wh);
    }

    /** 내 시청 기록: 스트리머별 시청 시간 (최근 순) */
    public List<Map<String, Object>> getWatchHistory(Long viewerId) {
        if (viewerId == null) return List.of();
        List<WatchHistory> list = watchHistoryRepository.findByViewerIdOrderByUpdatedAtDesc(viewerId, PageRequest.of(0, 100));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WatchHistory wh : list) {
            Optional<LiveStream> streamOpt = liveStreamRepository.findById(wh.getStreamId());
            if (streamOpt.isEmpty()) continue;
            LiveStream stream = streamOpt.get();
            String streamerName = userRepository.findById(stream.getUserId())
                    .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                    .orElse("알 수 없음");
            result.add(Map.<String, Object>of(
                    "streamId", wh.getStreamId(),
                    "streamerNickname", streamerName,
                    "streamTitle", stream.getTitle() != null ? stream.getTitle() : "",
                    "watchSeconds", wh.getWatchSeconds()
            ));
        }
        return result;
    }

    /** 내 방송 기록: 방송별 방송 시간 */
    public List<Map<String, Object>> getBroadcastHistory(Long userId) {
        if (userId == null) return List.of();
        List<LiveStream> streams = liveStreamRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (LiveStream s : streams) {
            long durationSeconds = 0;
            if (s.getStartedAt() != null) {
                if (s.getStatus() == StreamStatus.LIVE) {
                    durationSeconds = ChronoUnit.SECONDS.between(s.getStartedAt(), now);
                } else if (s.getEndedAt() != null) {
                    durationSeconds = ChronoUnit.SECONDS.between(s.getStartedAt(), s.getEndedAt());
                }
            }
            result.add(Map.<String, Object>of(
                    "streamId", s.getId(),
                    "title", s.getTitle() != null ? s.getTitle() : "",
                    "status", s.getStatus().name(),
                    "startedAt", s.getStartedAt() != null ? s.getStartedAt().toString() : "",
                    "endedAt", s.getEndedAt() != null ? s.getEndedAt().toString() : "",
                    "durationSeconds", durationSeconds
            ));
        }
        return result;
    }

    /** 내가 방송별로 받은 팡 */
    public List<Map<String, Object>> getPangByStream(Long toUserId) {
        if (toUserId == null) return List.of();
        List<Object[]> rows = donationRepository.findStreamIdAndSumByToUserId(toUserId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long streamId = (Long) row[0];
            Long sum = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            String title = liveStreamRepository.findById(streamId)
                    .map(LiveStream::getTitle)
                    .orElse("(삭제된 방송)");
            result.add(Map.<String, Object>of(
                    "streamId", streamId,
                    "title", title != null ? title : "",
                    "totalPang", sum.intValue()
            ));
        }
        return result;
    }
}
