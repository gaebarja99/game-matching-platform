package com.gamematcher.repository.account;

import com.gamematcher.entity.account.DiscordAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscordAccountRepository extends JpaRepository<DiscordAccount, Long> {

    Optional<DiscordAccount> findByDiscordId(String discordId);

    boolean existsByDiscordId(String discordId);

    List<DiscordAccount> findByUserId(Long userId);

    Optional<DiscordAccount> findFirstByUserId(Long userId);
}
