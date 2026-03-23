package com.gamematcher.constant;

/** 스트리머 구분: 수수료 차등 적용 */
public enum StreamerTier {
    /** 일반 스트리머 - 팡 수수료 30% */
    GENERAL(30),
    /** 파트너 스트리머 - 팡 수수료 20% (운영자 지정) */
    PARTNER(20);

    private final int commissionPercent;

    StreamerTier(int commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }
}
