package com.gamematcher.dto.ai.evaluation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gamematcher.constant.ai.evaluation.MatchResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 게임별 전적 데이터 공통 추상화.
 * {@code @JsonTypeInfo} / {@code @JsonSubTypes}로 game 코드(String)와 매핑하여 다형적 역직렬화 지원.
 * 전용 DTO가 없는 게임은 defaultImpl(GenericStatsDTO)로 역직렬화됨.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "game",
    visible = true,
    defaultImpl = GenericStatsDTO.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ValorantMatchStatsDTO.class, name = "VALORANT"),
    @JsonSubTypes.Type(value = LolStatsDTO.class, name = "LEAGUE_OF_LEGENDS"),
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseStatsDTO {

    private String game;

    private int kills;
    private int deaths;
    private int assists;
    private MatchResult result;

    protected BaseStatsDTO(String game, int kills, int deaths, int assists, MatchResult result) {
        this.game = game;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.result = result;
    }
}
