package com.gamematcher.service;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.PaymentOrderKind;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.entity.PangWithdrawal;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.repository.PangWithdrawalRepository;
import com.gamematcher.repository.PaymentOrderRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueService {

    public static final long MIN_DONATION_WITHDRAW_PANG = 100_000L;
    public static final long MIN_SUBSCRIPTION_WITHDRAW_PANG = 10L;
    public static final long WITHDRAW_STEP_PANG = 10L;

    private final DonationRepository donationRepository;
    private final PangWithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MileagePurchaseRepository mileagePurchaseRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getMyRevenue(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Map.ofEntries(
                    Map.entry("donationPang", 0L),
                    Map.entry("donationWithdrawablePang", 0L),
                    Map.entry("subscriptionPang", 0L),
                    Map.entry("subscriptionWithdrawablePang", 0L),
                    Map.entry("subscriptionRevenueWon", 0L),
                    Map.entry("totalReceivedPang", 0L),
                    Map.entry("settlementTargetPang", 0L),
                    Map.entry("totalWithdrawnPang", 0L),
                    Map.entry("withdrawablePang", 0L),
                    Map.entry("requestablePang", 0L),
                    Map.entry("commissionPercent", 30),
                    Map.entry("withdrawalHistory", List.of())
            );
        }

        long donationPang = donationRepository.sumAmountByToUserId(userId);
        long subscriptionRevenueWon =
                paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, userId)
                        + mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, userId);
        long subscriptionPang = wonToPang(subscriptionRevenueWon);
        RevenueBalance balance = calculateRevenueBalance(userId, donationPang, subscriptionPang);
        long totalReceived = donationPang + subscriptionPang;
        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;

        List<Map<String, Object>> withdrawalHistory = withdrawalRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .stream()
                .map(withdrawal -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", withdrawal.getId());
                    row.put("amountPang", safeLong(withdrawal.getAmountPang()));
                    row.put("commissionPercent", withdrawal.getCommissionPercent() != null ? withdrawal.getCommissionPercent() : 0);
                    row.put("commissionPang", safeLong(withdrawal.getCommissionPang()));
                    row.put("netPang", safeLong(withdrawal.getNetPang()));
                    row.put("settlementWon", pangToWon(safeLong(withdrawal.getNetPang())));
                    row.put("createdAt", withdrawal.getCreatedAt());
                    return row;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("donationPang", donationPang);
        response.put("donationWithdrawablePang", balance.donationWithdrawablePang());
        response.put("subscriptionPang", subscriptionPang);
        response.put("subscriptionWithdrawablePang", balance.subscriptionWithdrawablePang());
        response.put("subscriptionRevenueWon", subscriptionRevenueWon);
        response.put("totalReceivedPang", totalReceived);
        response.put("settlementTargetPang", balance.requestablePang());
        response.put("totalWithdrawnPang", balance.totalWithdrawnPang());
        response.put("withdrawablePang", balance.withdrawablePang());
        response.put("requestablePang", balance.requestablePang());
        response.put("commissionPercent", commissionPercent);
        response.put("streamerTier", user.getStreamerTier() != null ? user.getStreamerTier().name() : "GENERAL");
        response.put("withdrawalHistory", withdrawalHistory);
        return response;
    }

    @Transactional
    public Map<String, Object> withdraw(Long userId, long amountPang) {
        if (amountPang % WITHDRAW_STEP_PANG != 0) {
            throw new IllegalArgumentException("정산은 10팡 단위로만 가능합니다.");
        }
        if (amountPang < MIN_SUBSCRIPTION_WITHDRAW_PANG) {
            throw new IllegalArgumentException("정산은 최소 10팡부터 가능합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long donationPang = donationRepository.sumAmountByToUserId(userId);
        long subscriptionRevenueWon =
                paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, userId)
                        + mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, userId);
        long subscriptionPang = wonToPang(subscriptionRevenueWon);
        RevenueBalance balance = calculateRevenueBalance(userId, donationPang, subscriptionPang);
        long requestablePang = balance.requestablePang();

        if (amountPang > requestablePang) {
            throw new IllegalArgumentException("환전 가능 금액이 부족합니다. (가능: " + requestablePang + "팡)");
        }

        long subscriptionPangUsed = Math.min(amountPang, balance.requestableSubscriptionPang());
        long donationPangUsed = amountPang - subscriptionPangUsed;

        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;
        long commissionPang = amountPang * commissionPercent / 100;
        long netPang = amountPang - commissionPang;

        PangWithdrawal withdrawal = new PangWithdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmountPang(amountPang);
        withdrawal.setCommissionPercent(commissionPercent);
        withdrawal.setCommissionPang(commissionPang);
        withdrawal.setDonationPangUsed(donationPangUsed);
        withdrawal.setSubscriptionPangUsed(subscriptionPangUsed);
        withdrawal.setNetPang(netPang);
        withdrawalRepository.save(withdrawal);

        return Map.of(
                "message", "환전 요청이 접수되었습니다.",
                "amountPang", amountPang,
                "commissionPercent", commissionPercent,
                "commissionPang", commissionPang,
                "netPang", netPang,
                "donationPangUsed", donationPangUsed,
                "subscriptionPangUsed", subscriptionPangUsed,
                "withdrawablePang", Math.max(0L, balance.withdrawablePang() - amountPang),
                "requestablePang", Math.max(0L, requestablePang - amountPang)
        );
    }

    private RevenueBalance calculateRevenueBalance(Long userId, long donationPang, long subscriptionPang) {
        List<PangWithdrawal> withdrawals = withdrawalRepository.findByUserIdOrderByCreatedAtAsc(userId);

        long donationWithdrawn = 0L;
        long subscriptionWithdrawn = 0L;
        long totalWithdrawn = 0L;

        for (PangWithdrawal withdrawal : withdrawals) {
            long amountPang = safeLong(withdrawal.getAmountPang());
            totalWithdrawn += amountPang;

            Long donationUsed = withdrawal.getDonationPangUsed();
            Long subscriptionUsed = withdrawal.getSubscriptionPangUsed();
            boolean hasSourceSplit = donationUsed != null || subscriptionUsed != null;

            if (hasSourceSplit) {
                donationWithdrawn += safeLong(donationUsed);
                subscriptionWithdrawn += safeLong(subscriptionUsed);
            } else {
                donationWithdrawn += amountPang;
            }
        }

        long donationWithdrawable = Math.max(0L, donationPang - donationWithdrawn);
        long subscriptionWithdrawable = Math.max(0L, subscriptionPang - subscriptionWithdrawn);
        long requestableDonationPang = toDonationRequestablePang(donationWithdrawable);
        long requestableSubscriptionPang = toSubscriptionRequestablePang(subscriptionWithdrawable);

        return new RevenueBalance(
                donationWithdrawable,
                subscriptionWithdrawable,
                requestableDonationPang,
                requestableSubscriptionPang,
                requestableDonationPang + requestableSubscriptionPang,
                donationWithdrawable + subscriptionWithdrawable,
                totalWithdrawn
        );
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private long wonToPang(long won) {
        if (won <= 0) {
            return 0L;
        }
        return Math.round(won / 1.2d);
    }

    private long pangToWon(long pang) {
        if (pang <= 0) {
            return 0L;
        }
        return Math.round(pang * 1.2d);
    }

    private long toDonationRequestablePang(long donationWithdrawablePang) {
        if (donationWithdrawablePang < MIN_DONATION_WITHDRAW_PANG) {
            return 0L;
        }
        return donationWithdrawablePang - (donationWithdrawablePang % WITHDRAW_STEP_PANG);
    }

    private long toSubscriptionRequestablePang(long subscriptionWithdrawablePang) {
        if (subscriptionWithdrawablePang < MIN_SUBSCRIPTION_WITHDRAW_PANG) {
            return 0L;
        }
        return subscriptionWithdrawablePang - (subscriptionWithdrawablePang % WITHDRAW_STEP_PANG);
    }

    private record RevenueBalance(
            long donationWithdrawablePang,
            long subscriptionWithdrawablePang,
            long requestableDonationPang,
            long requestableSubscriptionPang,
            long requestablePang,
            long withdrawablePang,
            long totalWithdrawnPang
    ) {
    }
}
