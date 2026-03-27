package com.gamematcher.mapper;

import com.gamematcher.dto.lol.LolAiEvaluationResponseDto;
import com.gamematcher.entity.match.lol.LolMatchAiEvaluation;
import com.gamematcher.entity.match.lol.LolMatchParticipant;
import org.springframework.stereotype.Component;

@Component
public class LolAiEvaluationMapper {

    public LolAiEvaluationResponseDto toDto(LolMatchAiEvaluation entity) {
        if (entity == null) {
            return null;
        }
        LolMatchParticipant p = entity.getLolMatchParticipant();
        String matchId = p != null && p.getMatch() != null ? p.getMatch().getMatchId() : null;
        String display = formatDisplayName(
                p != null ? p.getRiotIdGameName() : null,
                p != null ? p.getRiotIdTagline() : null
        );
        return LolAiEvaluationResponseDto.builder()
                .matchId(matchId)
                .playerPuuid(p != null ? p.getPuuid() : null)
                .playerDisplayName(display)
                .champion(p != null ? p.getChampionName() : null)
                .teamPosition(p != null ? coalesce(p.getTeamPosition(), p.getIndividualPosition()) : null)
                .status(entity.getStatus())
                .score(entity.getScore())
                .grade(entity.getGrade())
                .summary(entity.getSummary())
                .detailedComment(entity.getDetailedComment())
                .llmModel(entity.getLlmModel())
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }

    private static String coalesce(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static String formatDisplayName(String name, String tag) {
        if (name == null && tag == null) {
            return null;
        }
        if (tag == null || tag.isBlank()) {
            return name;
        }
        return (name != null ? name : "") + "#" + tag;
    }
}
