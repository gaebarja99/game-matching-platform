package com.gamematcher.config;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 앱(127.0.0.1:5173 등) 접속 중인 사용자 = 온라인.
 * heartbeat로 lastSeen 갱신, 일정 시간 미갱신 시 오프라인. 로그아웃 시 제거.
 */
public final class OnlineUserStore {

    private static final long ONLINE_TTL_MS = 60_000L; // 60초 미갱신 시 오프라인

    private static final ConcurrentHashMap<Long, Long> USER_LAST_SEEN = new ConcurrentHashMap<>();

    public static void add(Long userId) {
        if (userId != null) USER_LAST_SEEN.put(userId, System.currentTimeMillis());
    }

    /** 앱 접속 중일 때 주기적으로 호출 (프론트에서 25~30초마다) */
    public static void heartbeat(Long userId) {
        if (userId != null) USER_LAST_SEEN.put(userId, System.currentTimeMillis());
    }

    public static void remove(Long userId) {
        if (userId != null) USER_LAST_SEEN.remove(userId);
    }

    /** TTL 이내에 heartbeat된 사용자 ID만 온라인으로 반환 */
    public static Set<Long> getOnlineUserIds() {
        long now = System.currentTimeMillis();
        Set<Long> online = USER_LAST_SEEN.entrySet().stream()
                .filter(e -> (now - e.getValue()) < ONLINE_TTL_MS)
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
        return Collections.unmodifiableSet(online);
    }

    public static boolean isOnline(Long userId) {
        if (userId == null) return false;
        Long last = USER_LAST_SEEN.get(userId);
        return last != null && (System.currentTimeMillis() - last) < ONLINE_TTL_MS;
    }
}
