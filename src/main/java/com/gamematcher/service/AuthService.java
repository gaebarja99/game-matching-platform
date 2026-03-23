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
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
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

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        user.setAuthToken(newAuthToken());
        userRepository.save(user);
        return AuthResponse.from(user);
    }

    private static String newAuthToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 비밀번호 변경. 현재 비밀번호 확인 후 새 비밀번호로 저장.
     */
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

    /**
     * 소셜 로그인용 고유 닉네임 생성. 기존 사이트 닉네임과 중복되지 않도록 Googlename1, Navername1 형식으로 부여.
     */
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

    /**
     * OAuth2(구글 등) 로그인: provider+subject로 기존 사용자 조회, 없으면 생성 후 반환.
     * 소셜 계정은 이름(username)은 제공자 이름 유지, 닉네임은 Googlename1·Navername1 등 고유값으로 부여해 중복 방지.
     */
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
                user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
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
        user.setAuthToken(newAuthToken());
        userRepository.save(user);
        return AuthResponse.from(user);
    }

    /** 데모용 휴대폰 인증 코드 (실서비스에서는 SMS 발송 후 사용자 입력값과 비교) */
    /**
     * 휴대폰 인증 후 해당 번호로 가입된 아이디(loginId) 목록 반환.
     * 휴대폰 인증은 프론트엔드에서 Firebase로 수행 후 전화번호만 전달.
     */
    @Transactional(readOnly = true)
    public java.util.List<String> findLoginIdsByPhone(String phone, String verificationCode) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해 주세요.");
        }
        // 인증은 프론트엔드 Firebase 휴대폰 인증으로 완료된 후 호출됨
        String trimmed = phone.trim();
        java.util.List<User> users = userRepository.findByPhone(trimmed);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("해당 휴대폰 번호로 가입된 계정이 없습니다.");
        }
        return users.stream()
                .map(User::getLoginId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 아이디 확인 후 휴대폰 인증이 완료된 계정의 비밀번호를 새 비밀번호로 변경.
     * loginId와 phone이 일치하는 계정만 변경. 휴대폰 인증은 프론트엔드 Firebase로 수행 후 전달.
     */
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
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디로 가입된 계정이 없습니다."));
        if (user.getPhone() == null || !user.getPhone().trim().equals(trimmedPhone)) {
            throw new IllegalArgumentException("해당 아이디에 등록된 휴대폰 번호와 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    /**
     * 계정 탈퇴: 일반 계정은 비밀번호 확인 후, 소셜 계정은 확인 없이 상태를 DELETED로 변경.
     */
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
        userRepository.save(user);
    }

}
