package com.gamematcher.constant.community;

/**
 * 게시글 상태
 */
public enum PostStatus {
    ACTIVE("활성"),
    BLIND("블라인드"),
    DELETED_BY_USER("사용자 삭제"),
    DELETED_BY_ADMIN("관리자 삭제");

    private final String displayName;

    PostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
