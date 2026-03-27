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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    private static final long USER_ID = 7L;

    @Mock DonationRepository donationRepository;
    @Mock PangWithdrawalRepository withdrawalRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentOrderRepository paymentOrderRepository;
    @Mock MileagePurchaseRepository mileagePurchaseRepository;

    @InjectMocks
    RevenueService revenueService;

    @Test
    void getMyRevenue_returns_requestable_total_across_donation_and_subscription() {
        User user = new User();
        user.setId(USER_ID);
        user.setStreamerTier(StreamerTier.PARTNER);

        PangWithdrawal legacyWithdrawal = new PangWithdrawal();
        legacyWithdrawal.setAmountPang(7_473_610L);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(donationRepository.sumAmountByToUserId(USER_ID)).thenReturn(16_379_164L);
        when(paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, USER_ID)).thenReturn(8_200L);
        when(mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, USER_ID)).thenReturn(0L);
        when(withdrawalRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(legacyWithdrawal));
        when(withdrawalRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(List.of());

        Map<String, Object> revenue = revenueService.getMyRevenue(USER_ID);

        assertThat(revenue.get("donationWithdrawablePang")).isEqualTo(8_905_554L);
        assertThat(revenue.get("subscriptionWithdrawablePang")).isEqualTo(6_833L);
        assertThat(revenue.get("withdrawablePang")).isEqualTo(8_912_387L);
        assertThat(revenue.get("requestablePang")).isEqualTo(8_912_380L);
        assertThat(revenue.get("settlementTargetPang")).isEqualTo(8_912_380L);
    }

    @Test
    void getMyRevenue_zeroes_requestable_amount_when_only_sub_ten_unit_remainder_is_left() {
        User user = new User();
        user.setId(USER_ID);
        user.setStreamerTier(StreamerTier.PARTNER);

        PangWithdrawal legacyWithdrawal = new PangWithdrawal();
        legacyWithdrawal.setAmountPang(7_473_610L);

        PangWithdrawal trackedWithdrawal = new PangWithdrawal();
        trackedWithdrawal.setAmountPang(8_912_380L);
        trackedWithdrawal.setDonationPangUsed(8_905_554L);
        trackedWithdrawal.setSubscriptionPangUsed(6_826L);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(donationRepository.sumAmountByToUserId(USER_ID)).thenReturn(16_379_164L);
        when(paymentOrderRepository.sumCompletedAmountWonByKindAndTargetUserId(PaymentOrderKind.SUBSCRIPTION, USER_ID)).thenReturn(8_200L);
        when(mileagePurchaseRepository.sumMileageCostByTypeAndTargetUserId(MileagePurchaseType.SUBSCRIPTION_TICKET, USER_ID)).thenReturn(0L);
        when(withdrawalRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(legacyWithdrawal, trackedWithdrawal));
        when(withdrawalRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(List.of());

        Map<String, Object> revenue = revenueService.getMyRevenue(USER_ID);

        assertThat(revenue.get("donationWithdrawablePang")).isEqualTo(0L);
        assertThat(revenue.get("subscriptionWithdrawablePang")).isEqualTo(7L);
        assertThat(revenue.get("withdrawablePang")).isEqualTo(7L);
        assertThat(revenue.get("requestablePang")).isEqualTo(0L);
        assertThat(revenue.get("settlementTargetPang")).isEqualTo(0L);
    }
}
