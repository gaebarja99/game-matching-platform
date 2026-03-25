package com.gamematcher.service;

import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.FollowRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankService {

    private static final int TOP_N = 10;

    private final FollowRepository followRepository;
    private final DonationRepository donationRepository;
    private final DonationService donationService;
    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final StreamViewerCountService streamViewerCountService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowRank() {
        List<Long> userIds = followRepository.findTopUserIdsByFollowerCount(PageRequest.of(0, TOP_N));
        return buildRankList(userIds, (id, u) -> Map.<String, Object>of(
                "userId", id,
                "displayName", u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername(),
                "count", followRepository.countByFollowingId(id)
        ));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPangRank() {
        List<Object[]> rows = donationRepository.findTopUserIdsByTotalDonation(PageRequest.of(0, TOP_N));
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            Long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            String displayName = userRepository.findById(userId)
                    .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                    .orElse("—");
            result.add(Map.of("rank", rank++, "userId", userId, "displayName", displayName, "totalPang", total));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getViewerRank() {
        List<LiveStream> all = liveStreamRepository.findByStatusAndEndedAtIsNullOrderByStartedAtDesc(
                com.gamematcher.constant.StreamStatus.LIVE);
        List<LiveStream> live = all.size() > TOP_N ? all.subList(0, TOP_N) : all;
        if (live.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = live.stream().map(LiveStream::getUserId).distinct().toList();
        Map<Long, String> names = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (LiveStream s : live) {
            int viewerCount = streamViewerCountService.getViewerCount(s.getId());
            result.add(Map.<String, Object>of(
                    "rank", rank++,
                    "streamId", s.getId(),
                    "title", s.getTitle() != null ? s.getTitle() : "",
                    "displayName", names.getOrDefault(s.getUserId(), "—"),
                    "viewerCount", viewerCount
            ));
        }
        return result;
    }

    /** 해당 방송에 후원한 적 있는 사용자 ID 목록 (채팅창 후원자 닉네임 색상용) */
    @Transactional(readOnly = true)
    public List<Long> getStreamDonorUserIds(Long streamId) {
        if (streamId == null) return List.of();
        return donationRepository.findDistinctFromUserIdByStreamId(streamId);
    }

    /** 주간 후원 랭킹: 해당 방송에 이번 주(월요일 0시~) 후원한 금액 순 상위 10명. 연속후원 일수 포함 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeeklyDonationRankByStream(Long streamId) {
        if (streamId == null) return List.of();
        Long toUserId = liveStreamRepository.findById(streamId).map(LiveStream::getUserId).orElse(null);
        if (toUserId == null) return List.of();
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        List<Object[]> rows = donationRepository.findWeeklyTopDonorsByStream(streamId, weekStart, PageRequest.of(0, TOP_N));
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            Long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            String displayName = userRepository.findById(userId)
                    .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                    .orElse("—");
            int consecutiveDays = donationService.getConsecutiveDonationDays(userId, toUserId);
            Map<String, Object> map = new java.util.LinkedHashMap<>(Map.of("rank", rank++, "userId", userId, "displayName", displayName, "totalPang", total));
            if (consecutiveDays >= 1) map.put("consecutiveDonationDays", consecutiveDays);
            result.add(map);
        }
        return result;
    }

    private List<Map<String, Object>> buildRankList(List<Long> userIds, java.util.function.BiFunction<Long, User, Map<String, Object>> mapper) {
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Long id : userIds) {
            User u = userRepository.findById(id).orElse(null);
            if (u == null) continue;
            Map<String, Object> map = new java.util.LinkedHashMap<>(mapper.apply(id, u));
            map.put("rank", rank++);
            result.add(map);
        }
        return result;
    }
}
