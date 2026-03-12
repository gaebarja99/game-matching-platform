package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValorantPlayerDto {

    private String puuid;
    private String name;
    private String tag;
    private String team;
    private int level;
    @JsonProperty("character")
    private String agent;

    @JsonProperty("currenttier")
    private int currentTier;

    @JsonProperty("currenttier_patched")
    private String currentTierPatched;

    @JsonProperty("player_card")
    private String playerCard;

    @JsonProperty("player_title")
    private String playerTitle;

    @JsonProperty("party_id")
    private String partyId;

    @JsonProperty("session_playtime")
    private SessionPlaytime sessionPlaytime;

    private Behavior behavior;
    private Platform platform;

    @JsonProperty("ability_casts")
    private AbilityCasts abilityCasts;

    private Assets assets;
    private Stats stats;
    private Economy economy;

    @JsonProperty("damage_made")
    private int damageMade;

    @JsonProperty("damage_received")
    private int damageReceived;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SessionPlaytime {
        private int minutes;
        private int seconds;
        private long milliseconds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Behavior {
        @JsonProperty("afk_rounds")
        private int afkRounds;

        @JsonProperty("friendly_fire")
        private FriendlyFire friendlyFire;

        @JsonProperty("rounds_in_spawn")
        private int roundsInSpawn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FriendlyFire {
        private int incoming;
        private int outgoing;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Platform {
        private String type;
        private Os os;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Os {
        private String name;
        private String version;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AbilityCasts {
        @JsonProperty("x_cast")
        private int xCast;

        @JsonProperty("e_cast")
        private int eCast;

        @JsonProperty("q_cast")
        private int qCast;

        @JsonProperty("c_cast")
        private int cCast;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Assets {
        private Card card;
        private Agent agent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Card {
        private String small;
        private String large;
        private String wide;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Agent {
        private String small;
        private String bust;
        private String full;
        private String killfeed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Stats {
        private int score;
        private int kills;
        private int deaths;
        private int assists;
        private int bodyshots;
        private int headshots;
        private int legshots;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Economy {
        private Spent spent;

        @JsonProperty("loadout_value")
        private LoadoutValue loadoutValue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Spent {
        private int overall;
        private double average;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoadoutValue {
        private int overall;
        private double average;
    }
}
