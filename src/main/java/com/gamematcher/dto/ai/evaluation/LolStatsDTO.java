package com.gamematcher.dto.ai.evaluation;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LolStatsDTO extends BaseStatsDTO {

    private int gold;
    private int minionsKilled;   // CS (Creep Score)
    private long damageDealt;
    private int visionScore;
    private int gameDurationMinutes;  // 게임 시간(분)

    public LolStatsDTO(String game, int kills, int deaths, int assists, MatchResult result,
                       int gold, int minionsKilled, long damageDealt, int visionScore, int gameDurationMinutes) {
        super(game, kills, deaths, assists, result);
        this.gold = gold;
        this.minionsKilled = minionsKilled;
        this.damageDealt = damageDealt;
        this.visionScore = visionScore;
        this.gameDurationMinutes = gameDurationMinutes;
    }
}
