package com.gamematcher.repository.account;

import com.gamematcher.entity.account.BlizzardAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlizzardAccountRepository extends JpaRepository<BlizzardAccount, Long> {

    Optional<BlizzardAccount> findByAccountIdAndRegion(String accountId, String region);

    boolean existsByAccountIdAndRegion(String accountId, String region);

    List<BlizzardAccount> findByUserId(Long userId);
}
