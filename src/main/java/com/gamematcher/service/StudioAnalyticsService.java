package com.gamematcher.service;

import com.gamematcher.constant.StreamStatus;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.LiveStreamChatMessageRepository;
import com.gamematcher.repository.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudioAnalyticsService {

    private final LiveStreamRepository liveStreamRepository;
    private final LiveStreamChatMessageRepository liveStreamChatMessageRepository;
    private final DonationRepository donationRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getLiveAnalysis(Long userId) {
        List<LiveStream> streams = liveStreamRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(stream -> stream.getStatus() == StreamStatus.LIVE || stream.getStatus() == StreamStatus.ENDED)
                .toList();

        List<Map<String, Object>> rows = streams.stream()
                .map(this::toRow)
                .toList();

        long totalDurationSeconds = rows.stream().mapToLong(row -> (Long) row.get("durationSeconds")).sum();
        long totalPlayCount = rows.stream().mapToLong(row -> (Long) row.get("playCount")).sum();
        long totalViewers = rows.stream().mapToLong(row -> (Long) row.get("totalViewers")).sum();
        long maxConcurrentViewers = rows.stream().mapToLong(row -> (Long) row.get("maxConcurrentViewers")).max().orElse(0L);
        long totalWatchTimeSeconds = rows.stream().mapToLong(row -> (Long) row.get("watchTimeSeconds")).sum();
        long totalChatParticipants = rows.stream().mapToLong(row -> (Long) row.get("chatParticipants")).sum();
        long totalDonationAmount = rows.stream().mapToLong(row -> (Long) row.get("donationAmount")).sum();
        long totalDonationCount = rows.stream().mapToLong(row -> (Long) row.get("donationCount")).sum();
        long avgDurationSeconds = streams.isEmpty() ? 0L : Math.round((double) totalDurationSeconds / streams.size());

        long avgConcurrentViewers = streams.isEmpty() ? 0L : Math.round((double) totalViewers / streams.size());
        long avgWatchSeconds = totalPlayCount == 0 ? 0L : Math.round((double) totalWatchTimeSeconds / totalPlayCount);
        int retentionRate = totalDurationSeconds == 0 ? 0 : (int) Math.max(0, Math.min(100, Math.round((double) totalWatchTimeSeconds / totalDurationSeconds * 100)));
        int chatParticipationRate = totalViewers == 0 ? 0 : (int) Math.max(0, Math.min(100, Math.round((double) totalChatParticipants / totalViewers * 100)));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("liveCount", streams.size());
        summary.put("totalDurationSeconds", totalDurationSeconds);
        summary.put("avgDurationSeconds", avgDurationSeconds);
        summary.put("playCount", totalPlayCount);
        summary.put("totalViewers", totalViewers);
        summary.put("totalWatchTimeSeconds", totalWatchTimeSeconds);
        summary.put("avgWatchSeconds", avgWatchSeconds);
        summary.put("maxConcurrentViewers", maxConcurrentViewers);
        summary.put("avgConcurrentViewers", avgConcurrentViewers);
        summary.put("retentionRate", retentionRate);
        summary.put("chatParticipants", totalChatParticipants);
        summary.put("chatParticipationRate", chatParticipationRate);
        summary.put("donationAmount", totalDonationAmount);
        summary.put("donationCount", totalDonationCount);
        summary.put("rows", rows);
        return summary;
    }

    private Map<String, Object> toRow(LiveStream stream) {
        long durationSeconds = getDurationSeconds(stream);
        long chatMessageCount = liveStreamChatMessageRepository.countByStreamId(stream.getId());
        long chatParticipants = liveStreamChatMessageRepository.countDistinctUserIdByStreamId(stream.getId());
        long donationCount = donationRepository.countByStreamId(stream.getId());
        long donationAmount = donationRepository.sumAmountByStreamId(stream.getId());
        long donorParticipants = donationRepository.findDistinctFromUserIdByStreamId(stream.getId()).size();
        long totalViewers = Math.max(chatParticipants, donorParticipants);

        // Persisted viewer history is unavailable, so use engaged viewers as the best available aggregate.
        long maxConcurrentViewers = Math.max(0L, totalViewers);
        long playCount = Math.max(chatMessageCount, Math.max(totalViewers, donationCount));
        long avgConcurrentViewers = durationSeconds == 0 ? 0L : Math.max(0L, Math.round((double) totalViewers / Math.max(1L, playCount)));
        long watchTimeSeconds = durationSeconds;
        long avgWatchSeconds = totalViewers == 0
                ? 0L
                : Math.max(0L, Math.round((double) durationSeconds / totalViewers));
        int chatParticipationRate = totalViewers == 0 ? 0 : (int) Math.max(0, Math.min(100, Math.round((double) chatParticipants / totalViewers * 100)));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stream.getId());
        row.put("title", stream.getTitle());
        row.put("durationSeconds", durationSeconds);
        row.put("playCount", playCount);
        row.put("totalViewers", totalViewers);
        row.put("maxConcurrentViewers", maxConcurrentViewers);
        row.put("avgConcurrentViewers", avgConcurrentViewers);
        row.put("watchTimeSeconds", watchTimeSeconds);
        row.put("avgWatchSeconds", avgWatchSeconds);
        row.put("chatParticipants", chatParticipants);
        row.put("chatParticipationRate", chatParticipationRate);
        row.put("donationAmount", donationAmount);
        row.put("donationCount", donationCount);
        return row;
    }

    private long getDurationSeconds(LiveStream stream) {
        LocalDateTime startedAt = stream.getStartedAt();
        if (startedAt == null) return 0L;
        LocalDateTime endedAt = stream.getEndedAt() != null ? stream.getEndedAt() : LocalDateTime.now();
        if (endedAt.isBefore(startedAt)) return 0L;
        return Duration.between(startedAt, endedAt).getSeconds();
    }
}
