package com.gamematcher.service;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.PangConstants;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.entity.User;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MileageShopService {

    private static final int MIN_PANG = 1_000;
    private static final int MAX_PANG = 999_999_999;
    private static final long SUBSCRIPTION_TICKET_COST = 4_900L;
    private static final long AD_FREE_30_DAYS_COST = 8_900L;

    private final UserRepository userRepository;
    private final PangService pangService;
    private final SubscriptionService subscriptionService;
    private final MileagePurchaseRepository mileagePurchaseRepository;

    @Transactional
    public Result buyPang(Long userId, int pangAmount) {
        if (pangAmount < MIN_PANG || pangAmount > MAX_PANG) {
            throw new IllegalArgumentException("구매할 팡은 1,000 이상 입력해 주세요.");
        }
        long mileageCost = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        if (mileage < mileageCost) {
            throw new IllegalArgumentException("마일리지가 부족합니다.");
        }
        user.setMileage(mileage - mileageCost);
        userRepository.save(user);

        long newPangBalance = pangService.grantFromMileage(userId, pangAmount);
        MileagePurchase p = new MileagePurchase();
        p.setUserId(userId);
        p.setType(MileagePurchaseType.PANG);
        p.setMileageCost(mileageCost);
        p.setPangAmount(pangAmount);
        mileagePurchaseRepository.save(p);
        return new Result(user.getMileage(), newPangBalance, "마일리지로 팡을 구매했습니다.");
    }

    @Transactional
    public Result buySubscriptionTicket(Long userId, Long streamerId) {
        if (streamerId == null) throw new IllegalArgumentException("구독 대상이 필요합니다.");
        if (userId.equals(streamerId)) throw new IllegalArgumentException("본인 채널은 구독할 수 없습니다.");
        if (subscriptionService.isSubscribed(userId, streamerId)) {
            throw new IllegalArgumentException("이미 구독 중입니다.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        if (mileage < SUBSCRIPTION_TICKET_COST) throw new IllegalArgumentException("마일리지가 부족합니다.");
        user.setMileage(mileage - SUBSCRIPTION_TICKET_COST);
        userRepository.save(user);
        subscriptionService.grantSubscription(userId, streamerId, false);

        MileagePurchase p = new MileagePurchase();
        p.setUserId(userId);
        p.setType(MileagePurchaseType.SUBSCRIPTION_TICKET);
        p.setMileageCost(SUBSCRIPTION_TICKET_COST);
        p.setTargetUserId(streamerId);
        mileagePurchaseRepository.save(p);
        return new Result(user.getMileage(), null, "마일리지로 구독권을 구매했습니다.");
    }

    @Transactional
    public Result buyAdFree30Days(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        if (mileage < AD_FREE_30_DAYS_COST) throw new IllegalArgumentException("마일리지가 부족합니다.");
        user.setMileage(mileage - AD_FREE_30_DAYS_COST);
        LocalDateTime base = user.getAdFreeUntil() != null && user.getAdFreeUntil().isAfter(LocalDateTime.now())
                ? user.getAdFreeUntil()
                : LocalDateTime.now();
        user.setAdFreeUntil(base.plusDays(30));
        userRepository.save(user);

        MileagePurchase p = new MileagePurchase();
        p.setUserId(userId);
        p.setType(MileagePurchaseType.AD_FREE_30_DAYS);
        p.setMileageCost(AD_FREE_30_DAYS_COST);
        mileagePurchaseRepository.save(p);
        return new Result(user.getMileage(), null, "광고 제거 30일권을 구매했습니다.");
    }

    @Transactional(readOnly = true)
    public List<MileagePurchase> getHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return mileagePurchaseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getHistoryCount(Long userId) {
        return mileagePurchaseRepository.countByUserId(userId);
    }

    public long getSubscriptionTicketCost() {
        return SUBSCRIPTION_TICKET_COST;
    }

    public long getAdFree30DaysCost() {
        return AD_FREE_30_DAYS_COST;
    }

    public record Result(Long mileageBalance, Long pangBalance, String message) {}
}
