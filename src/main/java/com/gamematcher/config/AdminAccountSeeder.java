package com.gamematcher.config;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.User;
import com.gamematcher.repository.common.CommonUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private final CommonUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.login-id:admin}")
    private String adminLoginId;

    @Value("${app.admin.seed.password:admin1234}")
    private String adminPassword;

    @Value("${app.admin.seed.username:관리자}")
    private String adminUsername;

    @Value("${app.admin.seed.email:admin@gamematcher.local}")
    private String adminEmail;

    public AdminAccountSeeder(CommonUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByLoginId(adminLoginId).orElseGet(User::new);
        admin.setLoginId(adminLoginId);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setAuthToken(java.util.UUID.randomUUID().toString().replace("-", ""));

        userRepository.save(admin);
    }
}
