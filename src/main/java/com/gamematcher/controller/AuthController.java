package com.gamematcher.controller;

import com.gamematcher.dto.auth.AuthResponse;
import com.gamematcher.dto.auth.LoginRequest;
import com.gamematcher.dto.auth.RegisterRequest;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import com.gamematcher.service.AuthService;
import com.gamematcher.service.RecaptchaEnterpriseService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_USER_ID = "userId";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RecaptchaEnterpriseService recaptchaEnterpriseService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        try {
            AuthResponse res = authService.login(request);
            session.setAttribute(SESSION_USER_ID, res.getId());
            com.gamematcher.config.OnlineUserStore.add(res.getId());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId != null) com.gamematcher.config.OnlineUserStore.remove(userId);
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    /** 앱 접속 중 온라인 유지용. 로그인된 세션이면 주기적으로 호출(heartbeat). */
    @GetMapping("/ping")
    public ResponseEntity<Void> ping(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId != null) com.gamematcher.config.OnlineUserStore.heartbeat(userId);
        return ResponseEntity.noContent().build();
    }

    /** 로그인 여부 조회. 로그인되어 있으면 온라인 목록에 추가(홈페이지 접속 = 온라인). */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(401).build();
        }
        com.gamematcher.config.OnlineUserStore.heartbeat(userId);
        return ResponseEntity.ok(AuthResponse.from(user));
    }

    /**
     * 아이디 중복 확인. available=true 면 사용 가능.
     */
    @GetMapping("/check/loginId")
    public ResponseEntity<java.util.Map<String, Boolean>> checkLoginId(@RequestParam("value") String value) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean available = !userRepository.existsByLoginId(value.trim());
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }

    /**
     * 이메일 중복 확인. available=true 면 사용 가능.
     */
    @GetMapping("/check/email")
    public ResponseEntity<java.util.Map<String, Boolean>> checkEmail(@RequestParam("value") String value) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean available = !userRepository.existsByEmail(value.trim());
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }

    /**
     * 전화번호 중복 확인. 회원가입 시 사용. available=true 면 사용 가능.
     */
    @GetMapping("/check/phone")
    public ResponseEntity<java.util.Map<String, Boolean>> checkPhone(@RequestParam("value") String value) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean available = !userRepository.existsByPhone(value.trim());
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }

    /**
     * 닉네임 중복 확인. 프로필 편집 시 본인 닉네임은 사용 가능.
     * available=true 면 사용 가능.
     */
    @GetMapping("/check/nickname")
    public ResponseEntity<java.util.Map<String, Boolean>> checkNickname(
            @RequestParam("value") String value,
            HttpSession session) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String trimmed = value.trim();
        Long currentUserId = (Long) session.getAttribute(SESSION_USER_ID);
        boolean available = currentUserId == null
                ? !userRepository.existsByNickname(trimmed)
                : !userRepository.existsByNicknameAndIdNot(trimmed, currentUserId);
        return ResponseEntity.ok(java.util.Map.of("available", available));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        try {
            AuthResponse res = authService.register(request);
            session.setAttribute(SESSION_USER_ID, res.getId());
            com.gamematcher.config.OnlineUserStore.add(res.getId());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * 비밀번호 변경. 현재 비밀번호 확인 후 새 비밀번호로 저장.
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody java.util.Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        String currentPassword = body != null ? body.get("currentPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "현재 비밀번호와 새 비밀번호를 입력해 주세요."));
        }
        try {
            authService.changePassword(userId, currentPassword, newPassword);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * 아이디 찾기: 휴대폰 번호 + 인증번호로 가입된 아이디(loginId) 목록 조회.
     * body: phone, verificationCode, recaptchaToken (선택, reCAPTCHA Enterprise 사용 시)
     */
    @PostMapping("/find-login-id")
    public ResponseEntity<?> findLoginId(@RequestBody java.util.Map<String, String> body) {
        String recaptchaToken = body != null ? body.get("recaptchaToken") : null;
        if (!recaptchaEnterpriseService.verify(recaptchaToken, "FIND_LOGIN_ID")) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "보안 검증에 실패했습니다. 다시 시도해 주세요."));
        }
        String phone = body != null ? body.get("phone") : null;
        try {
            java.util.List<String> loginIds = authService.findLoginIdsByPhone(phone);
            return ResponseEntity.ok(java.util.Map.of("loginIds", loginIds));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * 비밀번호 찾기: 아이디 확인 후 휴대폰 인증 완료된 계정의 비밀번호 재설정.
     * body: loginId, phone, verificationCode, newPassword, recaptchaToken (선택)
     */
    @PostMapping("/reset-password-by-phone")
    public ResponseEntity<?> resetPasswordByPhone(@RequestBody java.util.Map<String, String> body) {
        String recaptchaToken = body != null ? body.get("recaptchaToken") : null;
        if (!recaptchaEnterpriseService.verify(recaptchaToken, "RESET_PASSWORD")) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "보안 검증에 실패했습니다. 다시 시도해 주세요."));
        }
        String loginId = body != null ? body.get("loginId") : null;
        String phone = body != null ? body.get("phone") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        try {
            authService.resetPasswordByPhone(loginId, phone, newPassword);
            return ResponseEntity.ok(java.util.Map.of("message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * 계정 탈퇴. 비밀번호 확인 후 계정 비활성화. 세션 무효화.
     * body: password
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody java.util.Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        String password = body != null ? body.get("password") : null;
        try {
            authService.withdrawAccount(userId, password);
            if (session.getAttribute(SESSION_USER_ID) != null) {
                com.gamematcher.config.OnlineUserStore.remove(userId);
            }
            session.invalidate();
            return ResponseEntity.ok(java.util.Map.of("message", "계정이 탈퇴 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
