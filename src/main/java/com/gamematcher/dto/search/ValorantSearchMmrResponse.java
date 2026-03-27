package com.gamematcher.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValorantSearchMmrResponse {

    private boolean success;
    private String errorMessage;
    /** 화면 표시용 티어 문자열 */
    private String tierDisplay;

    public static ValorantSearchMmrResponse ok(String tierDisplay) {
        return ValorantSearchMmrResponse.builder()
                .success(true)
                .tierDisplay(tierDisplay != null ? tierDisplay : "")
                .build();
    }

    public static ValorantSearchMmrResponse fail(String message) {
        return ValorantSearchMmrResponse.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }
}
