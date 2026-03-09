package com.gamematcher.dto.stats;

import com.gamematcher.constant.GameList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LolStatsDTO extends BaseStatsDTO {

    private int gold;
    private int minionsKilled;   // CS (Creep Score)
    private long damageDealt;
    private int visionScore;
    private int gameDurationMinutes;  // 게임 시간(분)

    public LolStatsDTO(GameList game, int kills, int deaths, int assists, boolean won,
                       int gold, int minionsKilled, long damageDealt, int visionScore, int gameDurationMinutes) {
        super(game, kills, deaths, assists, won);
        this.gold = gold;
        this.minionsKilled = minionsKilled;
        this.damageDealt = damageDealt;
        this.visionScore = visionScore;
        this.gameDurationMinutes = gameDurationMinutes;
    }
}
