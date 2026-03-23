package com.gamematcher.constant.community;

/**
 * 알림 유형
 */
public enum NotificationType {
    COMMENT("댓글"),
    REPLY("대댓글"),
    LIKE("좋아요"),
    REPORT_RESOLVED("신고 처리 완료"),
    POST_DELETED("게시글 삭제");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
