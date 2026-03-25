package com.gamematcher.repository.account;

import com.gamematcher.entity.account.LolAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    boolean existsByPuuid(String puuid);

    Optional<LolAccount> findByPuuid(String puuid);

    Optional<LolAccount> findByGameNameAndTagLine(String gameName, String tagLine);
}
