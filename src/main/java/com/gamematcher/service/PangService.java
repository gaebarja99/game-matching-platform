package com.gamematcher.service;

import com.gamematcher.constant.PangConstants;
import com.gamematcher.entity.PangCharge;
import com.gamematcher.entity.User;
import com.gamematcher.repository.PangChargeRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PangService {

    /** 한도 없음 (실질 상한만 적용) */
    private static final int MAX_CHARGE_AMOUNT = 999_999_999;
    /** 충전 최소 1000팡 */
    private static final int MIN_CHARGE_AMOUNT = 100;

    private final UserRepository userRepository;
    private final PangChargeRepository pangChargeRepository;

    @Transactional(readOnly = true)
    public long getBalance(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0L;
        return user.getPangBalance() != null ? user.getPangBalance() : 0L;
    }

    @Transactional
    public long charge(Long userId, int pangAmount) {
        if (pangAmount < MIN_CHARGE_AMOUNT || pangAmount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("충전할 팡은 100 이상 입력해 주세요.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long priceWon = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);

        long newBalance = (user.getPangBalance() != null ? user.getPangBalance() : 0L) + pangAmount;
        user.setPangBalance(newBalance);
        // 팡 충전 시 경험치: 1000팡당 100 XP (0.1 단위로 저장 → 100 XP = 1000 tenths). 마일리지: 결제 금액(원)의 5%
        long expTenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        user.setTotalExperienceTenths(expTenths + (pangAmount * 1000L / 1000)); // 1000팡당 100 XP = 1000 tenths → 1팡당 1 tenth
        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        user.setMileage(mileage + Math.round(priceWon * 5.0 / 100.0));
        userRepository.save(user);

        saveChargeHistory(userId, pangAmount, priceWon, null, null);

        return newBalance;
    }

    @Transactional
    public long charge(Long userId, int pangAmount, String orderId, String impUid) {
        if (pangAmount < MIN_CHARGE_AMOUNT || pangAmount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("충전 팡은 100 이상 입력해 주세요.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long priceWon = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);

        long newBalance = (user.getPangBalance() != null ? user.getPangBalance() : 0L) + pangAmount;
        user.setPangBalance(newBalance);
        long expTenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        user.setTotalExperienceTenths(expTenths + (pangAmount * 1000L / 1000));
        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        user.setMileage(mileage + Math.round(priceWon * 5.0 / 100.0));
        userRepository.save(user);

        saveChargeHistory(userId, pangAmount, priceWon, orderId, impUid);
        return newBalance;
    }

    @Transactional
    public long revokeCharge(Long userId, int pangAmount) {
        if (pangAmount < 1 || pangAmount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("회수할 팡 수량이 올바르지 않습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long currentBalance = user.getPangBalance() != null ? user.getPangBalance() : 0L;
        if (currentBalance < pangAmount) {
            throw new IllegalStateException("취소 회수 실패: 잔액이 부족합니다. (current=" + currentBalance + ", required=" + pangAmount + ")");
        }

        long priceWon = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);
        long newBalance = currentBalance - pangAmount;
        user.setPangBalance(newBalance);

        long expTenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        user.setTotalExperienceTenths(Math.max(0L, expTenths - (pangAmount * 1000L / 1000)));

        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        user.setMileage(Math.max(0L, mileage - Math.round(priceWon * 5.0 / 100.0)));
        userRepository.save(user);

        saveChargeHistory(userId, -pangAmount, -priceWon, null, null);

        return newBalance;
    }

    @Transactional
    public long revokeCharge(Long userId, int pangAmount, String orderId, String impUid) {
        if (pangAmount < 1 || pangAmount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("회수할 팡 수량이 올바르지 않습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long currentBalance = user.getPangBalance() != null ? user.getPangBalance() : 0L;
        if (currentBalance < pangAmount) {
            throw new IllegalStateException("취소 회수 실패: 잔액이 부족합니다. (current=" + currentBalance + ", required=" + pangAmount + ")");
        }

        long priceWon = Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG);
        long newBalance = currentBalance - pangAmount;
        user.setPangBalance(newBalance);

        long expTenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        user.setTotalExperienceTenths(Math.max(0L, expTenths - (pangAmount * 1000L / 1000)));

        long mileage = user.getMileage() != null ? user.getMileage() : 0L;
        user.setMileage(Math.max(0L, mileage - Math.round(priceWon * 5.0 / 100.0)));
        userRepository.save(user);

        saveChargeHistory(userId, -pangAmount, -priceWon, orderId, impUid);
        return newBalance;
    }

    /** 마일리지 상품 구매로 팡 지급 (마일리지 적립 없음) */
    @Transactional
    public long grantFromMileage(Long userId, int pangAmount) {
        if (pangAmount < 1 || pangAmount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("지급할 팡 수량이 올바르지 않습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        long newBalance = (user.getPangBalance() != null ? user.getPangBalance() : 0L) + pangAmount;
        user.setPangBalance(newBalance);
        long expTenths = user.getTotalExperienceTenths() != null ? user.getTotalExperienceTenths() : 0L;
        user.setTotalExperienceTenths(expTenths + pangAmount);
        userRepository.save(user);

        saveChargeHistory(userId, pangAmount, Math.round(pangAmount * PangConstants.PRICE_WON_PER_PANG), null, null);
        return newBalance;
    }

    private void saveChargeHistory(Long userId, int pangAmount, long priceWon, String orderId, String impUid) {
        PangCharge charge = new PangCharge();
        charge.setUserId(userId);
        charge.setPangAmount(pangAmount);
        charge.setPriceWon(priceWon);
        charge.setOrderId(orderId);
        charge.setImpUid(impUid);
        pangChargeRepository.save(charge);
    }

    @Transactional(readOnly = true)
    public List<PangCharge> getChargeHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return pangChargeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getChargeHistoryCount(Long userId) {
        return pangChargeRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isRefundedCharge(Long userId, String orderId, String impUid) {
        if (userId == null) return false;
        if (orderId != null && !orderId.isBlank()) {
            return pangChargeRepository.existsByUserIdAndOrderIdAndPangAmountLessThan(userId, orderId, 0);
        }
        if (impUid != null && !impUid.isBlank()) {
            return pangChargeRepository.existsByUserIdAndImpUidAndPangAmountLessThan(userId, impUid, 0);
        }
        return false;
    }
}
