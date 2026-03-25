package com.gamematcher.repository.account;

import com.gamematcher.entity.account.RiotAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiotAccountRepository extends JpaRepository<RiotAccount, Long> {

    Optional<RiotAccount> findByPuuid(String puuid);

    boolean existsByPuuid(String puuid);

    List<RiotAccount> findByUserId(Long userId);

    Optional<RiotAccount> findFirstByUserId(Long userId);
}
