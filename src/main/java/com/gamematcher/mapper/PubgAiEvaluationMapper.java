package com.gamematcher.mapper;

import com.gamematcher.dto.pubg.PubgAiEvaluationResponseDto;
import com.gamematcher.entity.match.pubg.PubgMatchAiEvaluation;
import com.gamematcher.entity.match.pubg.PubgMatchParticipant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PubgAiEvaluationMapper {

    public PubgAiEvaluationResponseDto toDto(PubgMatchAiEvaluation entity, List<String> debugTimelinePreview) {
        if (entity == null) {
            return null;
        }

        PubgMatchParticipant p = entity.getPubgMatchParticipant();
        String matchId = p != null && p.getMatch() != null ? p.getMatch().getMatchId() : null;

        return PubgAiEvaluationResponseDto.builder()
                .matchId(matchId)
                .accountId(p != null ? p.getPlayerId() : null)
                .playerName(p != null ? p.getName() : null)
                .summary(entity.getSummary())
                .detailedComment(entity.getDetailedComment())
                .debugTimelinePreview(debugTimelinePreview != null ? debugTimelinePreview : List.of())
                .build();
    }
}

