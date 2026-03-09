package com.gamematcher.repository;

import com.gamematcher.constant.GameList;
import com.gamematcher.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByCode(GameList code);
}
