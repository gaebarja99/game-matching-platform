package com.gamematcher.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 후원 API 성공 시 화면 알림/애니메이션용 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponse {
    private String message;
    private String donorName;
    private Integer amount;
    /** 팡 등급: 팡(100~999), 슈퍼팡(1000~9999), 메가팡(10000+) */
    private String tier;
    /** 후원자가 입력한 선택 메시지 (후원 메시지) */
    private String donorMessage;
    /** 후원자 프로필 이미지 URL (채팅 옆 표시용) */
    private String donorProfileImageUrl;
    /** 연속후원 일수 (해당 스트리머에게 연속으로 후원한 날 수, 1 이상일 때만 표시) */
    private Integer consecutiveDonationDays;
}
