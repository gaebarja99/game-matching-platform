package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valorant 계정 조회 API 응답 (Henrik /valorant/v1/account/{name}/{tag})
 * { "status": 200, "data": { puuid, region, name, tag, ... } }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValorantPuuidApiResponse {

    private int status;
    private AccountData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AccountData {
        private String puuid;
        private String region;

        @JsonProperty("account_level")
        private Integer accountLevel;

        private String name;
        private String tag;
        private Card card;

        @JsonProperty("last_update")
        private String lastUpdate;

        @JsonProperty("last_update_raw")
        private Long lastUpdateRaw;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Card {
        private String small;
        private String large;
        private String wide;
        private String id;
    }
}
