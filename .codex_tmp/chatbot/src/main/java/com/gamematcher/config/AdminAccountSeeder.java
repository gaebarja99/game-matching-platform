package com.gamematcher.config;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.User;
import com.gamematcher.repository.common.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.seed.login-id:admin}")
    private String adminLoginId;

    @Value("${app.admin.seed.password:admin1234}")
    private String adminPassword;

    @Value("${app.admin.seed.username:관리자}")
    private String adminUsername;

    @Value("${app.admin.seed.email:admin@gamematcher.local}")
    private String adminEmail;

    public AdminAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByLoginId(adminLoginId)) {
            return;
        }

        User admin = new User();
        admin.setLoginId(adminLoginId);
        admin.setPassword(adminPassword);
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        userRepository.save(admin);
    }
}
