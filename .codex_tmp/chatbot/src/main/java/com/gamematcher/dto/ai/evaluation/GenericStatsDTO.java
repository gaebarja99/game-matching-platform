package com.gamematcher.dto.ai.evaluation;

import com.gamematcher.constant.ai.evaluation.MatchResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 전용 DTO가 없는 게임용 범용 통계 DTO.
 * DB에 새 게임 코드를 추가하면 defaultImpl로 자동 매핑됨.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GenericStatsDTO extends BaseStatsDTO {

    public GenericStatsDTO(String game, int kills, int deaths, int assists, MatchResult result) {
        super(game, kills, deaths, assists, result);
    }
}
