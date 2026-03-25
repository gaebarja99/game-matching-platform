package com.gamematcher.constant.community;

/**
 * Community board categories.
 */
public enum BoardCategory {
    FREE("자유게시판"),
    NOTICE("공지사항"),
    QUESTION("질문게시판"),
    LOL("롤"),
    VALORANT("발로란트"),
    PUBG("배그"),
    OVERWATCH("오버워치"),
    CS2("CS2"),
    APEX("에이펙스"),
    BLIZZARD("블리자드"),
    STEAM("스팀");

    private final String displayName;

    BoardCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
