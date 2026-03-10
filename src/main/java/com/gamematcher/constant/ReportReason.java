package com.gamematcher.constant;

/**
 * 신고 사유
 */
public enum ReportReason {
    SPAM("스팸"),
    HARASSMENT("괴롭힘"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
    CHEATING("부정행위"),
    IMPERSONATION("사칭"),
    HATE_SPEECH("혐오 발언"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
