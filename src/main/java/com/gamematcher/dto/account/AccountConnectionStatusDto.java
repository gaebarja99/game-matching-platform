package com.gamematcher.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class AccountConnectionStatusDto {
    private String provider;
    private boolean connected;
    private String displayName;
    private String secondaryValue;
    private String avatarUrl;
    private boolean ownershipVerified;
    private String connectUrl;
    private String note;
    /** Riot 연동 시 LoL 솔로/자유 랭크 요약 (API 조회) */
    private String lolRankSummary;
    /** Riot 연동 시 발로란트 경쟁 티어 요약 (Henrik API, 키 필요) */
    private String valorantRankSummary;
    /** 공개 프로필·커뮤니티 등에 이 연동을 노출할지 (연동된 경우에만 의미 있음) */
    @Builder.Default
    private boolean publicProfileVisible = true;
    /** Riot 전용: LoL 랭크 요약 공개 여부 */
    @Builder.Default
    private boolean publicLolRankVisible = true;
    /** Riot 전용: 발로란트 티어 요약 공개 여부 */
    @Builder.Default
    private boolean publicValorantRankVisible = true;
}
