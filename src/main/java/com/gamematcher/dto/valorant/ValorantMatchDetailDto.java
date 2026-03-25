package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Valorant 매치 상세 API 응답 DTO
 * data[] 배열의 각 매치 항목 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantMatchDetailDto {

    @JsonProperty("is_available")
    private boolean available;

    private ValorantMatchInfoDto metadata;
    private PlayersWrapper players;
    private List<Object> observers;
    private List<Object> coaches;
    private Teams teams;
    private List<Round> rounds;
    private List<KillEvent> kills;

    // --- players wrapper ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlayersWrapper {
        @JsonProperty("all_players")
        private List<ValorantPlayerDto> allPlayers;

        private List<ValorantPlayerDto> red;
        private List<ValorantPlayerDto> blue;
    }

    // --- teams ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Teams {
        private TeamResult red;
        private TeamResult blue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamResult {
        @JsonProperty("has_won")
        private boolean hasWon;

        @JsonProperty("rounds_won")
        private int roundsWon;

        @JsonProperty("rounds_lost")
        private int roundsLost;

        private Object roster;
    }

    // --- round ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Round {
        @JsonProperty("winning_team")
        private String winningTeam;

        @JsonProperty("end_type")
        private String endType;

        @JsonProperty("bomb_planted")
        private boolean bombPlanted;

        @JsonProperty("bomb_defused")
        private boolean bombDefused;

        @JsonProperty("plant_events")
        private PlantEvents plantEvents;

        @JsonProperty("defuse_events")
        private DefuseEvents defuseEvents;

        @JsonProperty("player_stats")
        private List<RoundPlayerStat> playerStats;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlantEvents {
        @JsonProperty("plant_location")
        private Location plantLocation;

        @JsonProperty("planted_by")
        private PlayerRef plantedBy;

        @JsonProperty("plant_site")
        private String plantSite;

        @JsonProperty("plant_time_in_round")
        private int plantTimeInRound;

        @JsonProperty("player_locations_on_plant")
        private List<PlayerLocation> playerLocationsOnPlant;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DefuseEvents {
        @JsonProperty("defuse_location")
        private Location defuseLocation;

        @JsonProperty("defused_by")
        private PlayerRef defusedBy;

        @JsonProperty("defuse_time_in_round")
        private int defuseTimeInRound;

        @JsonProperty("player_locations_on_defuse")
        private List<PlayerLocation> playerLocationsOnDefuse;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Location {
        private int x;
        private int y;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlayerRef {
        private String puuid;

        @JsonProperty("display_name")
        private String displayName;

        private String team;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlayerLocation {
        @JsonProperty("player_puuid")
        private String playerPuuid;

        @JsonProperty("player_display_name")
        private String playerDisplayName;

        @JsonProperty("player_team")
        private String playerTeam;

        private Location location;

        @JsonProperty("view_radians")
        private double viewRadians;
    }

    // --- round player stat ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundPlayerStat {
        @JsonProperty("ability_casts")
        private RoundAbilityCasts abilityCasts;

        @JsonProperty("player_puuid")
        private String playerPuuid;

        @JsonProperty("player_display_name")
        private String playerDisplayName;

        @JsonProperty("player_team")
        private String playerTeam;

        @JsonProperty("damage_events")
        private List<DamageEvent> damageEvents;

        private int damage;
        private int headshots;
        private int bodyshots;
        private int legshots;

        @JsonProperty("kill_events")
        private List<KillEvent> killEvents;

        private int kills;
        private int score;
        private RoundEconomy economy;

        @JsonProperty("was_afk")
        private boolean wasAfk;

        @JsonProperty("was_penalized")
        private boolean wasPenalized;

        @JsonProperty("stayed_in_spawn")
        private boolean stayedInSpawn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundAbilityCasts {
        @JsonProperty("x_casts")
        private Integer xCasts;

        @JsonProperty("e_casts")
        private Integer eCasts;

        @JsonProperty("q_casts")
        private Integer qCasts;

        @JsonProperty("c_casts")
        private Integer cCasts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DamageEvent {
        @JsonProperty("receiver_puuid")
        private String receiverPuuid;

        @JsonProperty("receiver_display_name")
        private String receiverDisplayName;

        @JsonProperty("receiver_team")
        private String receiverTeam;

        private int bodyshots;
        private int headshots;
        private int legshots;
        private int damage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoundEconomy {
        @JsonProperty("loadout_value")
        private int loadoutValue;

        private int remaining;
        private int spent;
        private WeaponArmorItem weapon;
        private WeaponArmorItem armor;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WeaponArmorItem {
        private String id;
        private String name;
        private WeaponArmorAssets assets;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WeaponArmorAssets {
        @JsonProperty("display_icon")
        private String displayIcon;

        @JsonProperty("killfeed_icon")
        private String killfeedIcon;
    }

    // --- kill event (round 내 kill_events + match 최상위 kills 공통) ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class KillEvent {
        @JsonProperty("kill_time_in_round")
        private int killTimeInRound;

        @JsonProperty("kill_time_in_match")
        private int killTimeInMatch;

        private Integer round;

        @JsonProperty("killer_puuid")
        private String killerPuuid;

        @JsonProperty("killer_display_name")
        private String killerDisplayName;

        @JsonProperty("killer_team")
        private String killerTeam;

        @JsonProperty("victim_puuid")
        private String victimPuuid;

        @JsonProperty("victim_display_name")
        private String victimDisplayName;

        @JsonProperty("victim_team")
        private String victimTeam;

        @JsonProperty("victim_death_location")
        private Location victimDeathLocation;

        @JsonProperty("damage_weapon_id")
        private String damageWeaponId;

        @JsonProperty("damage_weapon_name")
        private String damageWeaponName;

        @JsonProperty("damage_weapon_assets")
        private WeaponArmorAssets damageWeaponAssets;

        @JsonProperty("secondary_fire_mode")
        private boolean secondaryFireMode;

        @JsonProperty("player_locations_on_kill")
        private List<PlayerLocation> playerLocationsOnKill;

        private List<Assistant> assistants;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Assistant {
        @JsonProperty("assistant_puuid")
        private String assistantPuuid;

        @JsonProperty("assistant_display_name")
        private String assistantDisplayName;

        @JsonProperty("assistant_team")
        private String assistantTeam;
    }
}
