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

    private final DonationRepository donationRepository;
    private final PangWithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MileagePurchaseRepository mileagePurchaseRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getMyRevenue(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Map.of(
                    "donationPang", 0L,
                    "donationWithdrawablePang", 0L,
                    "subscriptionPang", 0L,
                    "subscriptionRevenueWon", 0L,
                    "totalReceivedPang", 0L,
                    "settlementTargetPang", 0L,
                    "totalWithdrawnPang", 0L,
                    "withdrawablePang", 0L,
                    "commissionPercent", 30,
                    "withdrawalHistory", List.of()
            );
        }

        long donationPang = donationRepository.sumAmountByToUserId(userId);
        long subscriptionRevenueWon = paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, userId)
                + mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, userId);
        long subscriptionPang = wonToPang(subscriptionRevenueWon);
        long totalReceived = donationPang + subscriptionPang;
        long totalWithdrawn = withdrawalRepository.sumAmountPangByUserId(userId);
        long donationWithdrawable = Math.max(0, donationPang - totalWithdrawn);
        long withdrawable = Math.max(0, totalReceived - totalWithdrawn);
        long settlementTargetPang = donationWithdrawable + subscriptionPang;
        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;

        List<Map<String, Object>> withdrawalHistory = withdrawalRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .stream()
                .map(withdrawal -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", withdrawal.getId());
                    row.put("amountPang", withdrawal.getAmountPang() != null ? withdrawal.getAmountPang() : 0L);
                    row.put("commissionPercent", withdrawal.getCommissionPercent() != null ? withdrawal.getCommissionPercent() : 0);
                    row.put("commissionPang", withdrawal.getCommissionPang() != null ? withdrawal.getCommissionPang() : 0L);
                    row.put("netPang", withdrawal.getNetPang() != null ? withdrawal.getNetPang() : 0L);
                    row.put("settlementWon", pangToWon(withdrawal.getNetPang() != null ? withdrawal.getNetPang() : 0L));
                    row.put("createdAt", withdrawal.getCreatedAt());
                    return row;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("donationPang", donationPang);
        response.put("donationWithdrawablePang", donationWithdrawable);
        response.put("subscriptionPang", subscriptionPang);
        response.put("subscriptionRevenueWon", subscriptionRevenueWon);
        response.put("totalReceivedPang", totalReceived);
        response.put("settlementTargetPang", settlementTargetPang);
        response.put("totalWithdrawnPang", totalWithdrawn);
        response.put("withdrawablePang", withdrawable);
        response.put("commissionPercent", commissionPercent);
        response.put("streamerTier", user.getStreamerTier() != null ? user.getStreamerTier().name() : "GENERAL");
        response.put("withdrawalHistory", withdrawalHistory);
        return response;
    }

    public static final long MIN_WITHDRAW_PANG = 100_000L;
    public static final long WITHDRAW_STEP_PANG = 10L;

    @Transactional
    public Map<String, Object> withdraw(Long userId, long amountPang) {
        if (amountPang < MIN_WITHDRAW_PANG) {
            throw new IllegalArgumentException("정산은 최소 " + MIN_WITHDRAW_PANG + "팡부터 가능합니다.");
        }
        if (amountPang % WITHDRAW_STEP_PANG != 0) {
            throw new IllegalArgumentException("정산은 " + WITHDRAW_STEP_PANG + "팡 단위로만 가능합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long donationPang = donationRepository.sumAmountByToUserId(userId);
        long subscriptionRevenueWon = paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, userId)
                + mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, userId);
        long subscriptionPang = wonToPang(subscriptionRevenueWon);
        long totalReceived = donationPang + subscriptionPang;
        long totalWithdrawn = withdrawalRepository.sumAmountPangByUserId(userId);
        long withdrawable = Math.max(0, totalReceived - totalWithdrawn);

        if (amountPang > withdrawable) {
            throw new IllegalArgumentException("환전 가능 팡이 부족합니다. (가능: " + withdrawable + " 팡)");
        }

        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;
        long commissionPang = amountPang * commissionPercent / 100;
        long netPang = amountPang - commissionPang;

        PangWithdrawal withdrawal = new PangWithdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmountPang(amountPang);
        withdrawal.setCommissionPercent(commissionPercent);
        withdrawal.setCommissionPang(commissionPang);
        withdrawal.setNetPang(netPang);
        withdrawalRepository.save(withdrawal);

        long newWithdrawn = totalWithdrawn + amountPang;
        long newWithdrawable = Math.max(0, totalReceived - newWithdrawn);
        return Map.of(
                "message", "환전 신청이 접수되었습니다.",
                "amountPang", amountPang,
                "commissionPercent", commissionPercent,
                "commissionPang", commissionPang,
                "netPang", netPang,
                "withdrawablePang", newWithdrawable
        );
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
}
