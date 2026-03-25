package com.gamematcher.constant.community;

/**
 * Notification type.
 */
public enum NotificationType {
    COMMENT("Comment"),
    REPLY("Reply"),
    LIKE("Like"),
    REPORT_RESOLVED("Report resolved"),
    POST_DELETED("Post deleted");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
