package com.gamematcher.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스트림별 동시 시청자 수 (메모리 저장).
 * 사용자(계정) 단위로 집계하여 같은 아이디로 여러 탭 접속 시 1명으로 카운트.
 */
@Service
public class StreamViewerCountService {

    /** streamId -> (userId -> true) 로 해당 스트림을 시청 중인 사용자 집합 */
    private final Map<Long, ConcurrentHashMap<Long, Boolean>> viewersByStream = new ConcurrentHashMap<>();

    public void join(Long streamId, Long userId) {
        if (streamId == null || userId == null) return;
        viewersByStream.computeIfAbsent(streamId, k -> new ConcurrentHashMap<>()).put(userId, Boolean.TRUE);
    }

    public void leave(Long streamId, Long userId) {
        if (streamId == null || userId == null) return;
        ConcurrentHashMap<Long, Boolean> set = viewersByStream.get(streamId);
        if (set != null) {
            set.remove(userId);
            if (set.isEmpty()) viewersByStream.remove(streamId);
        }
    }

    public int getViewerCount(Long streamId) {
        if (streamId == null) return 0;
        Map<Long, Boolean> set = viewersByStream.get(streamId);
        return set != null ? set.size() : 0;
    }
}
