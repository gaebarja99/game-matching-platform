package com.gamematcher.dto.search;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordsAiEvaluationResponse {

    private String matchId;
    private String playerKey;
    private String llmModel;
    private String status;
    private String grade;
    private Integer score;
    private String summary;
    private String detailedComment;
}
