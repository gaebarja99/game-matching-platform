package com.gamematcher.service;

import com.gamematcher.constant.LolPosition;
import com.gamematcher.constant.LolTier;
import com.gamematcher.entity.MatchSession;
import com.gamematcher.entity.MatchSessionMember;
import com.gamematcher.entity.MatchingQueue;
import com.gamematcher.exception.AlreadyInMatchingQueueException;
import com.gamematcher.repository.MatchSessionMemberRepository;
import com.gamematcher.repository.MatchSessionRepository;
import com.gamematcher.repository.MatchingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LoL 5인 랜덤 매칭: 동일 티어 우선, 60초 초과 시 ±1 티어 범위 허용(Querydsl로 조회).
 * 포지션은 라인별 1명을 우선하며, 동일 라인 희망이 겹치면 먼저 신청한 사람(FCFS)이 해당 라인을 가집니다.
 */
@Service
@RequiredArgsConstructor
public class LolMatchingService {

    public static final String GAME_LOL = "LEAGUE_OF_LEGENDS";
    private static final int TEAM_SIZE = 5;
    private static final int MAX_TIER_SPREAD = 2; // ±1단계 → 최대 3티어 (ordinal 차이 2)
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchSessionRepository matchSessionRepository;
    private final MatchSessionMemberRepository matchSessionMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void enqueue(Long userId, LolTier tier, LolPosition position) {
        if (userId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (matchingQueueRepository.existsByUserId(userId)) {
            throw new AlreadyInMatchingQueueException("이미 매칭 대기열에 참가 중입니다.");
        }
        MatchingQueue row = new MatchingQueue();
        row.setUserId(userId);
        row.setGameName(GAME_LOL);
        row.setTier(tier);
        row.setPosition(position != null ? position : LolPosition.FLEX);
        row.setMatched(false);
        matchingQueueRepository.save(row);
    }

    @Transactional
    public boolean leaveQueue(Long userId) {
        if (userId == null) return false;
        if (!matchingQueueRepository.existsByUserId(userId)) return false;
        matchingQueueRepository.deleteByUserId(userId);
        return true;
    }

    public boolean isInQueue(Long userId) {
        return userId != null && matchingQueueRepository.existsByUserId(userId);
    }

    /**
     * 스케줄러에서 주기 호출: 동일 티어 5인 매칭 → 이후 60초 이상 대기자에 한해 ±1 티어 범위 매칭.
     */
    @Transactional
    public void processQueue() {
        int guard = 0;
        while (guard++ < 30 && tryMatchExactSameTier()) {
            // 반복하여 같은 티어로 가능한 팀을 모두 결성
        }
        int guard2 = 0;
        while (guard2++ < 30 && tryMatchRelaxedTier()) {
            // ±1 티어 윈도우
        }
    }

    /** @return 팀을 하나라도 결성했으면 true */
    private boolean tryMatchExactSameTier() {
        for (LolTier tier : LolTier.values()) {
            List<MatchingQueue> list = matchingQueueRepository.findWaitingByGameAndTierOrderByCreatedAtAsc(GAME_LOL, tier);
            if (list.size() < TEAM_SIZE) continue;
            List<MatchingQueue> five = new ArrayList<>(list.subList(0, TEAM_SIZE));
            Optional<Map<Long, LolPosition>> assign = assignPositions(five);
            if (assign.isEmpty()) continue;
            finalizeMatch(five, assign.get());
            return true;
        }
        return false;
    }

    private boolean tryMatchRelaxedTier() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(60);
        List<MatchingQueue> pool = matchingQueueRepository.findWaitingByGameAndCreatedAtBeforeOrderByCreatedAtAsc(GAME_LOL, cutoff);
        if (pool.size() < TEAM_SIZE) return false;

        for (int i = 0; i <= pool.size() - TEAM_SIZE; i++) {
            List<MatchingQueue> five = new ArrayList<>(pool.subList(i, i + TEAM_SIZE));
            int minO = five.stream().map(MatchingQueue::getTier).mapToInt(Enum::ordinal).min().orElse(0);
            int maxO = five.stream().map(MatchingQueue::getTier).mapToInt(Enum::ordinal).max().orElse(0);
            if (maxO - minO > MAX_TIER_SPREAD) continue;

            Optional<Map<Long, LolPosition>> assign = assignPositions(five);
            if (assign.isEmpty()) continue;
            finalizeMatch(five, assign.get());
            return true;
        }
        return false;
    }

    /**
     * 포지션 배정: (1) 각 라인에 대해 해당 라인을 고른 사람 중 가장 빠른 순번 1명
     * (2) FLEX는 빈 라인에 FCFS (3) 남은 사람(중복 희망자)은 빈 라인에 FCFS
     */
    Optional<Map<Long, LolPosition>> assignPositions(List<MatchingQueue> five) {
        if (five.size() != TEAM_SIZE) return Optional.empty();
        List<MatchingQueue> sorted = five.stream()
                .sorted(Comparator.comparing(MatchingQueue::getCreatedAt))
                .collect(Collectors.toList());

        Map<Long, LolPosition> assign = new LinkedHashMap<>();
        Set<Long> assignedUser = new HashSet<>();
        Set<LolPosition> takenLanes = new HashSet<>();

        for (LolPosition slot : LolPosition.LANES) {
            sorted.stream()
                    .filter(p -> !assignedUser.contains(p.getUserId()))
                    .filter(p -> p.getPosition() == slot)
                    .min(Comparator.comparing(MatchingQueue::getCreatedAt))
                    .ifPresent(p -> {
                        assign.put(p.getUserId(), slot);
                        assignedUser.add(p.getUserId());
                        takenLanes.add(slot);
                    });
        }

        for (MatchingQueue p : sorted) {
            if (assignedUser.contains(p.getUserId())) continue;
            if (p.getPosition() != LolPosition.FLEX) continue;
            for (LolPosition slot : LolPosition.LANES) {
                if (!takenLanes.contains(slot)) {
                    assign.put(p.getUserId(), slot);
                    assignedUser.add(p.getUserId());
                    takenLanes.add(slot);
                    break;
                }
            }
        }

        for (MatchingQueue p : sorted) {
            if (assignedUser.contains(p.getUserId())) continue;
            for (LolPosition slot : LolPosition.LANES) {
                if (!takenLanes.contains(slot)) {
                    assign.put(p.getUserId(), slot);
                    assignedUser.add(p.getUserId());
                    takenLanes.add(slot);
                    break;
                }
            }
        }

        if (assignedUser.size() != TEAM_SIZE) return Optional.empty();
        return Optional.of(assign);
    }

    private void finalizeMatch(List<MatchingQueue> five, Map<Long, LolPosition> assign) {
        MatchSession session = new MatchSession();
        session.setGame(GAME_LOL);
        session = matchSessionRepository.save(session);

        List<Long> userIds = new ArrayList<>();
        Long sessionId = session.getId();
        for (MatchingQueue q : five) {
            Long uid = q.getUserId();
            LolPosition lane = assign.get(uid);
            MatchSessionMember m = new MatchSessionMember();
            m.setSessionId(sessionId);
            m.setUserId(uid);
            m.setAssignedLane(lane != null ? lane.name() : null);
            matchSessionMemberRepository.save(m);
            userIds.add(uid);
        }
        matchingQueueRepository.deleteAll(five);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MATCH_COMPLETE_LOL");
        payload.put("sessionId", session.getId());
        payload.put("game", session.getGame());
        payload.put("memberUserIds", userIds);
        Map<String, String> lanesByUser = new LinkedHashMap<>();
        assign.forEach((uid, lane) -> lanesByUser.put(String.valueOf(uid), lane.name()));
        payload.put("assignedLanes", lanesByUser);

        for (Long uid : userIds) {
            messagingTemplate.convertAndSend("/topic/user/" + uid, payload);
        }
    }

    /** 폴링용: 대기 중이면 대기 시작 시각 등 */
    public Map<String, Object> queueStatus(Long userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (userId == null) {
            map.put("inQueue", false);
            return map;
        }
        return matchingQueueRepository.findByUserId(userId)
                .map(q -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("inQueue", true);
                    m.put("gameName", q.getGameName());
                    m.put("tier", q.getTier().name());
                    m.put("position", q.getPosition().name());
                    m.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().format(ISO) : "");
                    return m;
                })
                .orElseGet(() -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("inQueue", false);
                    return m;
                });
    }
}
