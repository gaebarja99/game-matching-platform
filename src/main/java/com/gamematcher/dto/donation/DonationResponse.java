package com.gamematcher.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponse {
    private String message;
    private String donorName;
    private Integer amount;
    private String tier;
    private String donorMessage;
    private String videoUrl;
    private String donorProfileImageUrl;
    private Integer consecutiveDonationDays;
}
