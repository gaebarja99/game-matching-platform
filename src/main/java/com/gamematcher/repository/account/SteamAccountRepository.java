package com.gamematcher.repository.account;

import com.gamematcher.entity.account.SteamAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SteamAccountRepository extends JpaRepository<SteamAccount, Long> {

    Optional<SteamAccount> findBySteamId(String steamId);

    boolean existsBySteamId(String steamId);

    List<SteamAccount> findByUserId(Long userId);
}
