package com.gamematcher.repository.common;

import com.gamematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * GM2 병합 분 리포지토리. {@link com.gamematcher.repository.UserRepository}와 빈 이름 충돌을 피하기 위해 명명.
 */
public interface CommonUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    Optional<User> findByAuthToken(String authToken);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findFirstByUsernameContainingIgnoreCaseOrderByIdAsc(String username);

    List<User> findTop50ByUsernameContainingIgnoreCaseOrderByIdAsc(String username);
}
