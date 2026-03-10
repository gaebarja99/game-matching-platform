package com.gamematcher.dto.ai.evaluation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValorantStatsDTO extends BaseStatsDTO {

    private int roundsWon;
    private int roundsPlayed;
    private int headshots;
    private int totalShots;  // 헤드샷율 계산용

    public ValorantStatsDTO(String game, int kills, int deaths, int assists, boolean won,
                            int roundsWon, int roundsPlayed, int headshots, int totalShots) {
        super(game, kills, deaths, assists, won);
        this.roundsWon = roundsWon;
        this.roundsPlayed = roundsPlayed;
        this.headshots = headshots;
        this.totalShots = totalShots;
    }
}
