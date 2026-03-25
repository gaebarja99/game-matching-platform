package com.gamematcher.config;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앱 기동 시 가상 사용자 10명(user1~user10, 비밀번호 1234) 생성.
 * 이미 존재하면 건너뜀.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class InitialUserLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "1234";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByLoginId("asd8219").ifPresentOrElse(
                u -> {
                    if (u.getRole() != Role.ADMIN) {
                        u.setRole(Role.ADMIN);
                        userRepository.save(u);
                    }
                },
                () -> {
                    User operator = new User();
                    operator.setLoginId("asd8219");
                    operator.setPassword(passwordEncoder.encode("qd415623"));
                    operator.setUsername("운영자");
                    operator.setNickname("운영자");
                    operator.setEmail("asd8219@game-matcher.local");
                    operator.setRole(Role.ADMIN);
                    operator.setStatus(UserStatus.ACTIVE);
                    userRepository.save(operator);
                }
        );
        if (userRepository.existsByLoginId("user1")) {
            return;
        }
        for (int i = 1; i <= 10; i++) {
            String loginId = "user" + i;
            User user = new User();
            user.setLoginId(loginId);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setUsername("User " + i);
            user.setNickname("유저" + i);
            user.setEmail(loginId + "@test.com");
            user.setRole(Role.USER);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
    }
}
