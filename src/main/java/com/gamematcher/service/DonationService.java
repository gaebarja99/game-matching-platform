package com.gamematcher.service;

import com.gamematcher.dto.donation.DonationResponse;
import com.gamematcher.entity.Donation;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.User;
import com.gamematcher.repository.DonationRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    /** 후원 최소 1000팡 */
    private static final int MIN_AMOUNT = 1_000;
    /** 한도 없음 (실질 상한만 적용) */
    private static final int MAX_AMOUNT = 999_999_999;

    /** 1~9999: 팡, 10000~99999: 슈퍼팡, 100000+: 메가팡 */
    public static String getPangTier(int amount) {
        if (amount >= 100_000) return "메가팡";
        if (amount >= 10_000) return "슈퍼팡";
        return "팡";
    }

    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final DonationRepository donationRepository;

    @Transactional
    public DonationResponse donate(Long fromUserId, Long streamId, int amount, String message) {
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("후원할 팡은 1,000 이상 입력해 주세요.");
        }
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
        long balance = fromUser.getPangBalance() != null ? fromUser.getPangBalance() : 0L;
        if (balance < amount) {
            throw new IllegalArgumentException("팡 잔액이 부족합니다. 프로필에서 팡을 충전해 주세요.");
        }
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        Long toUserId = stream.getUserId();
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("본인 방송에는 후원할 수 없습니다.");
        }

        fromUser.setPangBalance(balance - amount);
        userRepository.save(fromUser);

        Donation donation = new Donation();
        donation.setFromUserId(fromUserId);
        donation.setStreamId(streamId);
        donation.setToUserId(toUserId);
        donation.setAmount(amount);
        donation.setMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        donationRepository.save(donation);

        int consecutiveDays = computeConsecutiveDonationDays(fromUserId, toUserId, donation.getCreatedAt() != null ? donation.getCreatedAt().toLocalDate() : LocalDate.now());

        String donorName = fromUser.getNickname() != null && !fromUser.getNickname().isBlank()
                ? fromUser.getNickname() : fromUser.getUsername();
        String savedMessage = donation.getMessage();
        String donorProfileImageUrl = fromUser.getProfileImageUrl();
        DonationResponse resp = new DonationResponse(
                "후원해 주셔서 감사합니다!",
                donorName,
                amount,
                getPangTier(amount),
                savedMessage != null && !savedMessage.isBlank() ? savedMessage : null,
                donorProfileImageUrl,
                consecutiveDays >= 1 ? consecutiveDays : null
        );
        return resp;
    }

    /** 해당 스트리머에게 현재 연속 후원 일수 (오늘 포함, 서버 날짜 기준). 주간 랭킹 등에서 사용 */
    @Transactional(readOnly = true)
    public int getConsecutiveDonationDays(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null) return 0;
        return computeConsecutiveDonationDays(fromUserId, toUserId, LocalDate.now());
    }

    /** 해당 스트리머에게 오늘 포함 연속으로 후원한 일수 계산 (서버 날짜 기준) */
    private int computeConsecutiveDonationDays(Long fromUserId, Long toUserId, LocalDate today) {
        List<Donation> list = donationRepository.findByFromUserIdAndToUserIdOrderByCreatedAtDesc(
                fromUserId, toUserId, PageRequest.of(0, 365));
        List<LocalDate> sortedDates = list.stream()
                .map(d -> d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : null)
                .filter(d -> d != null)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        int count = 0;
        LocalDate expected = today;
        for (LocalDate d : sortedDates) {
            if (d.equals(expected)) {
                count++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<Donation> getMyDonations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return donationRepository.findByFromUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getMyDonationsCount(Long userId) {
        return donationRepository.countByFromUserId(userId);
    }
}
