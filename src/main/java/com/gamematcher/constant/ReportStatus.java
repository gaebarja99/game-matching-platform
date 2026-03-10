package com.gamematcher.constant;

/**
 * 신고 처리 상태
 */
public enum ReportStatus {
    PENDING("접수대기"),
    IN_REVIEW("검토중"),
    RESOLVED("처리완료"),
    DISMISSED("기각");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
