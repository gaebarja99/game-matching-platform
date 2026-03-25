package com.gamematcher.repository.common;

import com.gamematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByAuthToken(String authToken);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
}
