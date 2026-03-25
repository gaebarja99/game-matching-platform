package com.gamematcher.service;

import com.gamematcher.dto.subscription.SubscriberItemDto;
import com.gamematcher.entity.Subscription;
import com.gamematcher.repository.SubscriptionRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    /** 정기구독 월 요금 (원). 구독 결제 시 마일리지 10% 적립 */
    private static final long SUBSCRIPTION_PRICE_WON = 4_900L;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public long getSubscriptionPriceWon() {
        return SUBSCRIPTION_PRICE_WON;
    }

    public boolean isSubscribed(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null || subscriberId.equals(streamerId)) {
            return false;
        }
        return subscriptionRepository.existsByStreamerIdAndSubscriberId(streamerId, subscriberId);
    }

    @Transactional
    public void subscribe(Long subscriberId, Long streamerId) {
        grantSubscription(subscriberId, streamerId, false);
    }

    @Transactional
    public void grantSubscription(Long subscriberId, Long streamerId, boolean rewardMileage) {
        if (subscriberId == null || streamerId == null || subscriberId.equals(streamerId)) {
            return;
        }
        if (subscriptionRepository.existsByStreamerIdAndSubscriberId(streamerId, subscriberId)) {
            return;
        }
        Subscription sub = new Subscription();
        sub.setStreamerId(streamerId);
        sub.setSubscriberId(subscriberId);
        subscriptionRepository.save(sub);

        if (rewardMileage) {
            // 구독 결제 시 GameMatcher 마일리지 10% 적립
            userRepository.findById(subscriberId).ifPresent(subscriber -> {
                long current = subscriber.getMileage() != null ? subscriber.getMileage() : 0L;
                subscriber.setMileage(current + Math.round(SUBSCRIPTION_PRICE_WON * 10.0 / 100.0));
                userRepository.save(subscriber);
            });
        }
    }

    @Transactional
    public void unsubscribe(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null) {
            return;
        }
        subscriptionRepository.deleteByStreamerIdAndSubscriberId(streamerId, subscriberId);
    }

    public long getSubscriberCount(Long streamerId) {
        if (streamerId == null) return 0;
        return subscriptionRepository.countByStreamerId(streamerId);
    }

    /** 해당 스트리머를 구독한 사용자 ID 목록 (채팅창 구독 뱃지용) */
    public List<Long> getSubscriberIds(Long streamerId) {
        if (streamerId == null) return List.of();
        return subscriptionRepository.findByStreamerIdOrderByCreatedAtDesc(streamerId).stream()
                .map(Subscription::getSubscriberId)
                .collect(Collectors.toList());
    }

    /** 스트리머 채널의 구독자 목록, 최신순 */
    public List<SubscriberItemDto> getSubscriberList(Long streamerId) {
        if (streamerId == null) return List.of();
        return subscriptionRepository.findByStreamerIdOrderByCreatedAtDesc(streamerId).stream()
                .map(s -> userRepository.findById(s.getSubscriberId())
                        .map(u -> {
                            String nickname = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
                            return new SubscriberItemDto(u.getId(), nickname, u.getLoginId(), s.getCreatedAt());
                        })
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
