package com.gamematcher.dto.stats;

import com.gamematcher.constant.GameList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OTHERS 및 전용 DTO가 아직 없는 게임(OVERWATCH, PUBG 등)용 범용 통계 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class GenericStatsDTO extends BaseStatsDTO {

    public GenericStatsDTO(GameList game, int kills, int deaths, int assists, boolean won) {
        super(game, kills, deaths, assists, won);
    }
}
