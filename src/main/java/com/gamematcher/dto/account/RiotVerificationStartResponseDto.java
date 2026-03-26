package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RiotVerificationStartResponseDto {
    private String verificationId;
    private String gameType;
    private String platform;
    private String gameName;
    private String tagLine;
    private String verificationMethod;
    private String verificationCode;
    private String currentCardId;
    private String currentCardImageUrl;
    private String instructionTitle;
    private String instructionBody;
}
