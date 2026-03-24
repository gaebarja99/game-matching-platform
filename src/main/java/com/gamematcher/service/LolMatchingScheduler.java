package com.gamematcher.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LoL 매칭 대기열을 주기적으로 스캔하여 5인 팀을 결성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LolMatchingScheduler {

    private final LolMatchingService lolMatchingService;

    @Scheduled(fixedDelay = 3000)
    public void tick() {
        try {
            lolMatchingService.processQueue();
        } catch (Exception e) {
            log.warn("LoL 매칭 처리 중 오류: {}", e.getMessage());
        }
    }
}
