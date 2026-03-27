package com.gamematcher.repository.account;

import com.gamematcher.entity.account.ValorantAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValorantAccountRepository extends JpaRepository<ValorantAccount, Long> {

    boolean existsByPuuid(String puuid);

    Optional<ValorantAccount> findByPuuid(String puuid);

    Optional<ValorantAccount> findByNameAndTag(String name, String tag);

    Optional<ValorantAccount> findByNameIgnoreCaseAndTagIgnoreCase(String name, String tag);
}
