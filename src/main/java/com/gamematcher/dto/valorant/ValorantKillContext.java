package com.gamematcher.dto.valorant;

/**
 * 발로란트 킬 1건당 컨텍스트. 라운드 점수 계산 시 사용.
 * 경제 역전·상황 변화·킬 데미지 비율 계산에 활용.
 */
public record ValorantKillContext(
        String killerPuuid,
        String victimPuuid,
        int ourAliveAtKill,
        int theirAliveAtKill,
        int ourTeamLoadout,
        int theirTeamLoadout,
        int killTimeInRound
) {}
