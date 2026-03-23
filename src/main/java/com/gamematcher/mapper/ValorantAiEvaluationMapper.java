package com.gamematcher.mapper;

import com.gamematcher.dto.valorant.ValorantAiEvaluationResponseDto;
import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import org.springframework.stereotype.Component;

/**
 * ValorantMatchAiEvaluation ↔ ValorantAiEvaluationResponseDto 변환.
 */
@Component
public class ValorantAiEvaluationMapper {

    /**
     * 엔티티 → DTO
     */
    public ValorantAiEvaluationResponseDto toDto(ValorantMatchAiEvaluation entity) {
        if (entity == null) {
            return null;
        }
        ValorantMatchPlayer player = entity.getValorantMatchPlayer();
        String matchId = player != null && player.getMatch() != null
                ? player.getMatch().getMatchId()
                : null;
        String displayName = formatDisplayName(
                player != null ? player.getName() : null,
                player != null ? player.getTag() : null
        );
        return ValorantAiEvaluationResponseDto.builder()
                .matchId(matchId)
                .playerPuuid(player != null ? player.getPuuid() : null)
                .playerDisplayName(displayName)
                .agent(player != null ? player.getAgent() : null)
                .team(player != null ? player.getTeam() : null)
                .status(entity.getStatus())
                .score(entity.getScore())
                .grade(entity.getGrade())
                .summary(entity.getSummary())
                .detailedComment(entity.getDetailedComment())
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }

    private String formatDisplayName(String name, String tag) {
        if (name == null && tag == null) return null;
        if (tag == null || tag.isBlank()) return name;
        return (name != null ? name : "") + "#" + tag;
    }
}
