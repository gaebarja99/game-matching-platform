package com.gamematcher.repository.ai.evaluation;

import com.gamematcher.entity.ai.evaluation.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByCode(String code);
}
