package com.gamematcher.config;

import com.gamematcher.constant.GameList;
import com.gamematcher.entity.ai.evaluation.Game;
import com.gamematcher.repository.ai.evaluation.GameRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("!test")
public class GameDataLoader implements ApplicationRunner {

    private static final Map<GameList, String> DISPLAY_NAMES = Map.of(
        GameList.OTHERS, "기타",
        GameList.LEAGUE_OF_LEGENDS, "리그 오브 레전드",
        GameList.VALORANT, "발로란트",
        GameList.OVERWATCH, "오버워치",
        GameList.PUBG, "배틀그라운드",
        GameList.COUNTER_STRIKE_2, "카운터 스트라이크 2"
    );

    private final GameRepository gameRepository;

    public GameDataLoader(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (gameRepository.count() > 0) {
            return;
        }

        for (GameList gameList : GameList.values()) {
            String code = gameList.name();
            String name = DISPLAY_NAMES.getOrDefault(gameList, toDisplayName(code));
            gameRepository.save(new Game(name, code, null));
        }
    }

    private static String toDisplayName(String code) {
        return code.replace("_", " ");
    }
}
