package com.gamematcher.controller;

import com.gamematcher.constant.MileagePurchaseType;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.MileagePurchase;
import com.gamematcher.entity.User;
import com.gamematcher.repository.MileagePurchaseRepository;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.NotificationService;
import com.gamematcher.service.PangService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/pang")
@RequiredArgsConstructor
public class AdminMileageGiftController {

    private static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final PangService pangService;
    private final NotificationService notificationService;
    private final MileagePurchaseRepository mileagePurchaseRepository;

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    private Long currentAdminId(HttpSession session) {
        return (Long) session.getAttribute(SESSION_USER_ID);
    }

    private long grantMileage(User user, long mileageAmount) {
        long nextMileage = (user.getMileage() != null ? user.getMileage() : 0L) + mileageAmount;
        user.setMileage(nextMileage);
        userRepository.save(user);

        MileagePurchase history = new MileagePurchase();
        history.setUserId(user.getId());
        history.setType(MileagePurchaseType.ADMIN_GIFT);
        history.setMileageCost(mileageAmount);
        mileagePurchaseRepository.save(history);

        return nextMileage;
    }

    @PostMapping("/gift-flex")
    public ResponseEntity<?> gift(@RequestBody Map<String, Object> body, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "\uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."));
        }

        String loginId = Objects.toString(body.get("loginId"), "").trim();
        String customMessage = Objects.toString(body.get("message"), "").trim();
        boolean sendAsMileage = Boolean.parseBoolean(Objects.toString(body.get("sendAsMileage"), "false"));
        if (loginId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "\uC544\uC774\uB514\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694."));
        }

        long rewardAmount;
        try {
            rewardAmount = Long.parseLong(Objects.toString(body.get("pangAmount"), "0"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "\uC9C0\uAE09\uD560 \uC218\uB7C9\uC744 \uC815\uD655\uD788 \uC785\uB825\uD574 \uC8FC\uC138\uC694."));
        }
        if (rewardAmount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "\uC9C0\uAE09\uD560 \uC218\uB7C9\uC740 1 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4."));
        }

        Long adminUserId = currentAdminId(session);

        if ("/all".equalsIgnoreCase(loginId)) {
            List<User> targetUsers = userRepository.findAll().stream()
                    .filter(user -> user.getStatus() != UserStatus.DELETED)
                    .collect(Collectors.toList());

            if (targetUsers.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "\uC9C0\uAE09\uD560 \uD68C\uC6D0\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."));
            }

            for (User targetUser : targetUsers) {
                if (sendAsMileage) {
                    grantMileage(targetUser, rewardAmount);
                    notificationService.createForAdminMileageGift(targetUser.getId(), adminUserId, rewardAmount, customMessage);
                } else {
                    pangService.grantFromMileage(targetUser.getId(), Math.toIntExact(rewardAmount));
                    notificationService.createForAdminPangGift(targetUser.getId(), adminUserId, Math.toIntExact(rewardAmount), customMessage);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", sendAsMileage
                            ? "\uC804\uCCB4 \uD68C\uC6D0\uC5D0\uAC8C \uB9C8\uC77C\uB9AC\uC9C0\uB97C \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4."
                            : "\uC804\uCCB4 \uD68C\uC6D0\uC5D0\uAC8C \uD31D\uC744 \uC9C0\uAE09\uD588\uC2B5\uB2C8\uB2E4.",
                    "recipientCount", targetUsers.size()
            ));
        }

        User targetUser = userRepository.findByLoginId(loginId).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "\uB300\uC0C1 \uD68C\uC6D0\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        if (sendAsMileage) {
            long currentMileage = grantMileage(targetUser, rewardAmount);
            notificationService.createForAdminMileageGift(targetUser.getId(), adminUserId, rewardAmount, customMessage);
            response.put("message", "\uB9C8\uC77C\uB9AC\uC9C0 \uC9C0\uAE09\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");
            response.put("mileageBalance", currentMileage);
        } else {
            long currentBalance = pangService.grantFromMileage(targetUser.getId(), Math.toIntExact(rewardAmount));
            notificationService.createForAdminPangGift(targetUser.getId(), adminUserId, Math.toIntExact(rewardAmount), customMessage);
            response.put("message", "\uD31D \uC9C0\uAE09\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");
            response.put("pangBalance", currentBalance);
        }
        response.put("loginId", targetUser.getLoginId());
        return ResponseEntity.ok(response);
    }
}
