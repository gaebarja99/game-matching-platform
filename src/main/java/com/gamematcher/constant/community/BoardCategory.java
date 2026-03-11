package com.gamematcher.constant.community;

/**
 * 게시판 카테고리
 */
public enum BoardCategory {
    FREE("자유게시판"),
    NOTICE("공지사항"),
    QUESTION("질문게시판"),
    LOL("롤"),
    VALORANT("발로란트"),
    PUBG("배그");

    private final String displayName;

    BoardCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
