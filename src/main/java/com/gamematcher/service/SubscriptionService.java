package com.gamematcher.service;

import com.gamematcher.dto.subscription.SubscriberItemDto;
import com.gamematcher.entity.Subscription;
import com.gamematcher.repository.SubscriptionRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final long SUBSCRIPTION_PRICE_WON = 4_900L;
    private static final int SUBSCRIPTION_DURATION_DAYS = 30;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public long getSubscriptionPriceWon() {
        return SUBSCRIPTION_PRICE_WON;
    }

    public boolean isSubscribed(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null || subscriberId.equals(streamerId)) {
            return false;
        }
        return subscriptionRepository.findByStreamerIdAndSubscriberId(streamerId, subscriberId)
                .filter(this::isActive)
                .isPresent();
    }

    public LocalDateTime getSubscriptionExpiresAt(Long subscriberId, Long streamerId) {
        if (subscriberId == null || streamerId == null || subscriberId.equals(streamerId)) {
            return null;
        }
        return subscriptionRepository.findByStreamerIdAndSubscriberId(streamerId, subscriberId)
                .map(this::resolveExpiresAt)
                .orElse(null);
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

        Optional<Subscription> existingOpt = subscriptionRepository.findByStreamerIdAndSubscriberId(streamerId, subscriberId);
        if (existingOpt.isPresent()) {
            Subscription existing = existingOpt.get();
            if (isActive(existing)) {
                return;
            }
            subscriptionRepository.delete(existing);
        }

        Subscription sub = new Subscription();
        sub.setStreamerId(streamerId);
        sub.setSubscriberId(subscriberId);
        subscriptionRepository.save(sub);

        if (rewardMileage) {
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
        Optional<Subscription> existingOpt = subscriptionRepository.findByStreamerIdAndSubscriberId(streamerId, subscriberId);
        if (existingOpt.isEmpty()) {
            return;
        }
        if (isActive(existingOpt.get())) {
            throw new IllegalStateException("구독은 이용 기간이 끝날 때까지 유지되며 중도 취소할 수 없습니다.");
        }
        subscriptionRepository.delete(existingOpt.get());
    }

    public long getSubscriberCount(Long streamerId) {
        if (streamerId == null) return 0;
        return subscriptionRepository.findByStreamerIdOrderByCreatedAtDesc(streamerId).stream()
                .filter(this::isActive)
                .count();
    }

    public List<Long> getSubscriberIds(Long streamerId) {
        if (streamerId == null) return List.of();
        return subscriptionRepository.findByStreamerIdOrderByCreatedAtDesc(streamerId).stream()
                .filter(this::isActive)
                .map(Subscription::getSubscriberId)
                .collect(Collectors.toList());
    }

    public List<SubscriberItemDto> getSubscriberList(Long streamerId) {
        if (streamerId == null) return List.of();
        return subscriptionRepository.findByStreamerIdOrderByCreatedAtDesc(streamerId).stream()
                .filter(this::isActive)
                .map(s -> userRepository.findById(s.getSubscriberId())
                        .map(u -> {
                            String nickname = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
                            return new SubscriberItemDto(u.getId(), nickname, u.getLoginId(), u.getProfileImageUrl(), s.getCreatedAt());
                        })
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private boolean isActive(Subscription subscription) {
        return resolveExpiresAt(subscription).isAfter(LocalDateTime.now());
    }

    private LocalDateTime resolveExpiresAt(Subscription subscription) {
        LocalDateTime createdAt = subscription.getCreatedAt() != null ? subscription.getCreatedAt() : LocalDateTime.MIN;
        return createdAt.plusDays(SUBSCRIPTION_DURATION_DAYS);
    }
}
