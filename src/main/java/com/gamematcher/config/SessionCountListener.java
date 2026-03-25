package com.gamematcher.config;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 현재 접속 중인 세션 수를 세어 /api/stats/online 에서 노출합니다.
 * 세션 만료 시 해당 사용자를 온라인 목록에서 제거합니다.
 */
public class SessionCountListener implements HttpSessionListener {

    private static final String SESSION_USER_ID = "userId";
    private static final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        count.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Object uid = session != null ? session.getAttribute(SESSION_USER_ID) : null;
        if (uid instanceof Long) OnlineUserStore.remove((Long) uid);
        count.decrementAndGet();
    }

    public static int getOnlineCount() {
        return Math.max(0, count.get());
    }
}
