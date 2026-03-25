package com.gamematcher.constant.community;

/**
 * 게시글 추천/비추천
 */
public enum RecommendType {
    RECOMMEND("추천"),
    NOT_RECOMMEND("비추천");

    private final String displayName;

    RecommendType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
