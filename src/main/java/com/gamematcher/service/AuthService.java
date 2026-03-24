package com.gamematcher.service;

import com.gamematcher.constant.Provider;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.dto.auth.AuthResponse;
import com.gamematcher.dto.auth.LoginRequest;
import com.gamematcher.dto.auth.RegisterRequest;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        String phone = request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null;
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("전화번호를 입력해 주세요.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("이미 가입한 전화번호입니다.");
        }
        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank() ? request.getNickname().trim() : null);
        user.setEmail(request.getEmail());
        user.setPhone(phone);
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthToken(newAuthToken());
        userRepository.save(user);
        return AuthResponse.from(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            LocalDateTime suspendedUntil = user.getSuspendedUntil();
            if (suspendedUntil != null && suspendedUntil.isBefore(LocalDateTime.now())) {
                user.setStatus(UserStatus.ACTIVE);
                user.setSuspendedUntil(null);
                user.setSuspensionReason(null);
            } else if (suspendedUntil == null) {
                throw new IllegalArgumentException("영구 정지된 계정입니다.");
            } else {
                String untilLabel = suspendedUntil.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                throw new IllegalArgumentException("정지된 계정입니다. 해제 예정: " + untilLabel);
            }
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setAuthToken(newAuthToken());
        userRepository.save(user);
        return AuthResponse.from(user);
    }

    private static String newAuthToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    private String generateUniqueOAuthNickname(Provider provider) {
        String prefix = provider.name().charAt(0) + provider.name().substring(1).toLowerCase() + "name";
        int n = 1;
        String candidate;
        do {
            candidate = prefix + n;
            n++;
        } while (userRepository.existsByNickname(candidate));
        return candidate;
    }

    @Transactional
    public AuthResponse findOrCreateByOAuth(Provider provider, String providerSubject, String email, String name, String profileImageUrl) {
        User user = userRepository.findByProviderAndProviderSubject(provider, providerSubject).orElse(null);
        if (user == null) {
            String loginId = provider.name().toLowerCase() + "_" + providerSubject;
            if (userRepository.existsByLoginId(loginId)) {
                user = userRepository.findByLoginId(loginId).orElse(null);
            }
            if (user == null) {
                user = new User();
                user.setLoginId(loginId);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setUsername(name != null && !name.isBlank() ? name.trim() : (email != null ? email : "User"));
                user.setNickname(generateUniqueOAuthNickname(provider));
                user.setEmail(email != null && !email.isBlank() ? email : (loginId + "@oauth.local"));
                user.setProvider(provider);
                user.setProviderSubject(providerSubject);
                user.setProfileImageUrl(profileImageUrl != null && !profileImageUrl.isBlank() ? profileImageUrl : null);
                user.setRole(Role.USER);
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setAuthToken(newAuthToken());
        userRepository.save(user);
        return AuthResponse.from(user);
    }

    @Transactional(readOnly = true)
    public java.util.List<String> findLoginIdsByPhone(String phone, String verificationCode) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해 주세요.");
        }
        String trimmed = phone.trim();
        java.util.List<User> users = userRepository.findByPhone(trimmed);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("해당 휴대폰 번호로 가입한 계정이 없습니다.");
        }
        return users.stream()
                .map(User::getLoginId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    @Transactional
    public void resetPasswordByPhone(String loginId, String phone, String verificationCode, String newPassword) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해 주세요.");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }
        String trimmedLoginId = loginId.trim();
        String trimmedPhone = phone.trim();
        User user = userRepository.findByLoginId(trimmedLoginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디로 가입한 계정이 없습니다."));
        if (user.getPhone() == null || !user.getPhone().trim().equals(trimmedPhone)) {
            throw new IllegalArgumentException("해당 아이디에 등록된 휴대폰 번호와 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    @Transactional
    public void withdrawAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (user.getProvider() == null) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }
        user.setStatus(UserStatus.DELETED);
        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
        userRepository.save(user);
    }
}
