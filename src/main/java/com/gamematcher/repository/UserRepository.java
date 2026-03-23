package com.gamematcher.repository;

import com.gamematcher.constant.Provider;
import com.gamematcher.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    @Query("SELECT u FROM User u WHERE u.id != :excludeId AND (LOWER(u.loginId) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<User> searchByLoginIdOrNickname(@Param("q") String q, @Param("excludeId") Long excludeId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** 다른 사용자가 해당 닉네임을 쓰는지 (본인 제외). 프로필 편집 시 중복 확인용 */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    Optional<User> findByProviderAndProviderSubject(Provider provider, String providerSubject);

    /** 휴대폰 번호로 가입된 계정 조회 (아이디 찾기·비밀번호 재설정용) */
    List<User> findByPhone(String phone);

    /** 전화번호 중복 여부 (회원가입 시 사용) */
    boolean existsByPhone(String phone);
}
