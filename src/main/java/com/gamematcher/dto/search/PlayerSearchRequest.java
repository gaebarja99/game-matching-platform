package com.gamematcher.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSearchRequest {

    private String game;
    private String gameName;
    private String tagLine;
    private String nickname;
    private String steamId;
    private String platform = "steam";
    private String region = "kr";
    private Integer count = 20;
    private Integer queueType;

    public void parseNickname() {
        if (nickname != null && !nickname.isBlank()) {
            int idx = nickname.lastIndexOf('#');
            if (idx > 0) {
                this.gameName = nickname.substring(0, idx).trim();
                this.tagLine = nickname.substring(idx + 1).trim();
            } else {
                this.gameName = nickname.trim();
            }
        }
    }

    public PlayerSearchRequest normalize() {
        parseNickname();
        if (region == null || region.isBlank()) region = "kr";
        if (platform == null || platform.isBlank()) platform = "steam";
        if (count == null || count <= 0 || count > 20) count = 20;
        if (game != null) game = game.toLowerCase().trim();
        return this;
    }
}
