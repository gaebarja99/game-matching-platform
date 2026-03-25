package com.gamematcher.constant.community;

/**
 * 커뮤니티 신고 대상 유형
 */
public enum ReportTargetType {
    POST("게시글"),
    COMMENT("댓글");

    private final String displayName;

    ReportTargetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
