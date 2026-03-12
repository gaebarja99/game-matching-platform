package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant Lifetime API data[] 배열의 각 매치 항목 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantLifetimeDataItem {

    private ValorantLifetimeMeta meta;
    private ValorantLifetimeStats stats;
    private ValorantLifetimeTeams teams;

    // --- meta ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ValorantLifetimeMeta {
        private String id;
        private MapRef map;
        private String version;
        private String mode;

        @JsonProperty("started_at")
        private String startedAt;

        private SeasonRef season;
        private String region;
        private String cluster;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MapRef {
        private String id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SeasonRef {
        private String id;

        @JsonProperty("short")
        private String shortName;
    }

    // --- stats ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ValorantLifetimeStats {
        private String puuid;
        private String name;
        private String tag;
        private String team;
        private int level;
        private CharacterRef character;
        private int tier;
        private int score;
        private int kills;
        private int deaths;
        private int assists;
        private Shots shots;
        private Damage damage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CharacterRef {
        private String id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Shots {
        private int head;
        private int body;
        private int leg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Damage {
        private int made;
        private int received;
    }

    // --- teams ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ValorantLifetimeTeams {
        private int red;
        private int blue;
    }
}
