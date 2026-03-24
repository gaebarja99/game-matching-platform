package com.gamematcher.dto.valorant;

/**
 * 발로란트 어시스트 1건당. 해당 킬의 컨텍스트.
 * 데미지 비례 점수는 damageToEliminated(victim→데미지)로 조회.
 * 조회 시 없으면 0 (스킬 어시스트, 배율 1.0).
 */
public record ValorantAssistContext(
        ValorantKillContext killContext
) {}
