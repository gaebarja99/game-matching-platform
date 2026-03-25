package com.gamematcher.repository.common;

import com.gamematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    /**
     * 닉네임 부분 일치(LIKE %q%). 여러 명이면 id 오름차순 첫 번째.
     * {@code Containing}은 대소문자 구분 DB에서도 {@code IgnoreCase}로 비교합니다.
     */
    Optional<User> findFirstByUsernameContainingIgnoreCaseOrderByIdAsc(String username);

    /** 닉네임 부분 일치, 최대 50명, id 오름차순 */
    List<User> findTop50ByUsernameContainingIgnoreCaseOrderByIdAsc(String username);
}
