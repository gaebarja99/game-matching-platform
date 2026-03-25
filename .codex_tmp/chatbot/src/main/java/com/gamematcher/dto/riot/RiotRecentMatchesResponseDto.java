package com.gamematcher.dto.riot;

import java.util.List;

public class RiotRecentMatchesResponseDto {

    private String puuid;
    private String gameName;
    private String tagLine;
    private List<RiotMatchDetailResponseDto> matches;

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getTagLine() {
        return tagLine;
    }

    public void setTagLine(String tagLine) {
        this.tagLine = tagLine;
    }

    public List<RiotMatchDetailResponseDto> getMatches() {
        return matches;
    }

    public void setMatches(List<RiotMatchDetailResponseDto> matches) {
        this.matches = matches;
    }
}
