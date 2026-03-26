package com.gamematcher.dto.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Riot 계정 연동 결과
 */
@Getter
@Setter
@NoArgsConstructor
public class RiotAccountLinkResponseDto {

    private Long id;
    private Long userId;
    private String puuid;
    private String gameName;
    private String tagLine;
    private String gameType;
    private String verificationMethod;
    private boolean ownershipVerified;
    private String message;

    public RiotAccountLinkResponseDto(Long id, Long userId, String puuid, String gameName, String tagLine, String message) {
        this.id = id;
        this.userId = userId;
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.message = message;
    }

    public RiotAccountLinkResponseDto(Long id, Long userId, String puuid, String gameName, String tagLine,
                                      String gameType, String verificationMethod, boolean ownershipVerified,
                                      String message) {
        this.id = id;
        this.userId = userId;
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.gameType = gameType;
        this.verificationMethod = verificationMethod;
        this.ownershipVerified = ownershipVerified;
        this.message = message;
    }
}
