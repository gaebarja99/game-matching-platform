package com.gamematcher.service;

import com.gamematcher.constant.StreamerTier;
import com.gamematcher.entity.PangWithdrawal;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.PangWithdrawalRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final DonationRepository donationRepository;
    private final PangWithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;

    /**
     * 내 수익 요약: 받은 총 팡, 이미 환전한 팡, 환전 가능 팡, 적용 수수료율
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMyRevenue(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Map.of("totalReceivedPang", 0L, "totalWithdrawnPang", 0L, "withdrawablePang", 0L, "commissionPercent", 30);
        }
        long totalReceived = donationRepository.sumAmountByToUserId(userId);
        long totalWithdrawn = withdrawalRepository.sumAmountPangByUserId(userId);
        long withdrawable = Math.max(0, totalReceived - totalWithdrawn);
        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;
        return Map.of(
                "totalReceivedPang", totalReceived,
                "totalWithdrawnPang", totalWithdrawn,
                "withdrawablePang", withdrawable,
                "commissionPercent", commissionPercent,
                "streamerTier", user.getStreamerTier() != null ? user.getStreamerTier().name() : "GENERAL"
        );
    }

    /** 정산 최소 금액 (팡) */
    public static final long MIN_WITHDRAW_PANG = 100_000L;
    /** 정산 단위 (팡) */
    public static final long WITHDRAW_STEP_PANG = 10L;

    /**
     * 환전 신청. 수수료 차감 후 기록. (실제 송금/정산은 별도 프로세스 가정)
     * 최소 10만 팡부터 10단위로 신청 가능.
     */
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
        long totalReceived = donationRepository.sumAmountByToUserId(userId);
        long totalWithdrawn = withdrawalRepository.sumAmountPangByUserId(userId);
        long withdrawable = Math.max(0, totalReceived - totalWithdrawn);
        if (amountPang > withdrawable) {
            throw new IllegalArgumentException("환전 가능 팡이 부족합니다. (가능: " + withdrawable + " 팡)");
        }
        int commissionPercent = user.getStreamerTier() == StreamerTier.PARTNER ? 20 : 30;
        long commissionPang = amountPang * commissionPercent / 100;
        long netPang = amountPang - commissionPang;

        PangWithdrawal w = new PangWithdrawal();
        w.setUserId(userId);
        w.setAmountPang(amountPang);
        w.setCommissionPercent(commissionPercent);
        w.setCommissionPang(commissionPang);
        w.setNetPang(netPang);
        withdrawalRepository.save(w);

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
}
