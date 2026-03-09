package com.gamematcher.dto.stats;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gamematcher.constant.GameList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게임별 전적 데이터 공통 추상화.
 * {@code @JsonTypeInfo} / {@code @JsonSubTypes}로 GameList enum과 매핑하여 다형적 역직렬화 지원.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "game",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ValorantStatsDTO.class, name = "VALORANT"),
    @JsonSubTypes.Type(value = LolStatsDTO.class, name = "LEAGUE_OF_LEGENDS"),
    @JsonSubTypes.Type(value = GenericStatsDTO.class, name = "OTHERS"),
    @JsonSubTypes.Type(value = GenericStatsDTO.class, name = "OVERWATCH"),
    @JsonSubTypes.Type(value = GenericStatsDTO.class, name = "PUBG"),
    @JsonSubTypes.Type(value = GenericStatsDTO.class, name = "COUNTER_STRIKE_2"),
    @JsonSubTypes.Type(value = GenericStatsDTO.class, name = "APEX_LEGENDS")
})
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseStatsDTO {

    private GameList game;

    private int kills;
    private int deaths;
    private int assists;
    private boolean won;

    protected BaseStatsDTO(GameList game, int kills, int deaths, int assists, boolean won) {
        this.game = game;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.won = won;
    }
}
