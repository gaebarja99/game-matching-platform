package com.gamematcher.service.search;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.ai.evaluation.Grade;
import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.dto.search.RecordsAiEvaluationResponse;
import com.gamematcher.mapper.LolMatchStatsMapper;
import com.gamematcher.service.ai.LlmEvaluationService;
import com.gamematcher.service.ai.LolEvaluationPromptBuilder;
import com.gamematcher.service.ai.RuleBasedScoreService;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.lol.LolMatchJsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordsAiEvaluationService {

    @Value("${ai.llm.model:gpt-5-mini}")
    private String defaultLlmModel;

    private final LolApiService lolApiService;
    private final LolMatchJsonService lolMatchJsonService;
    private final LolMatchStatsMapper lolMatchStatsMapper;
    private final LolEvaluationPromptBuilder lolEvaluationPromptBuilder;
    private final LlmEvaluationService llmEvaluationService;
    private final RuleBasedScoreService ruleBasedScoreService;

    public RecordsAiEvaluationResponse evaluateLolMatch(
            String matchId,
            String puuid,
            String modelOverride,
            Integer maxTimelineEvents
    ) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId가 필요합니다.");
        }
        if (puuid == null || puuid.isBlank()) {
            throw new IllegalArgumentException("puuid가 필요합니다.");
        }

        LolMatchDetailDto matchDto = lolMatchJsonService.parseFirstMatch(
                lolApiService.getMatchRawByMatchId(matchId.trim())
        );

        LolMatchTimelineDetailDto timelineDto = null;
        try {
            String timelineRaw = lolApiService.getMatchTimelineRawByMatchId(matchId.trim());
            if (timelineRaw != null && !timelineRaw.isBlank()) {
                timelineDto = lolMatchJsonService.parseTimelineDetail(timelineRaw);
            }
        } catch (Exception e) {
            log.debug("LoL records AI timeline load skipped: {}", e.getMessage());
        }

        LolPlayerMatchStatsDTO playerStats = lolMatchStatsMapper.toPlayerMatchStatsDto(
                matchDto,
                puuid.trim(),
                timelineDto
        );
        if (playerStats == null) {
            throw new IllegalArgumentException("해당 플레이어를 매치 데이터에서 찾지 못했습니다.");
        }

        int timelineLimit = maxTimelineEvents != null ? maxTimelineEvents : 40;
        String prompt = lolEvaluationPromptBuilder.build(playerStats, timelineLimit);
        String resolvedModel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride.trim()
                : defaultLlmModel;

        Optional<LlmEvaluationResponseDTO> llmResult = llmEvaluationService.evaluate(prompt, resolvedModel);
        Integer score = ruleBasedScoreService.calculateScore(
                GameList.LEAGUE_OF_LEGENDS.name(),
                playerStats.getMatchStats()
        ).orElse(null);

        return RecordsAiEvaluationResponse.builder()
                .matchId(matchId.trim())
                .playerKey(playerStats.getPlayerPuuid())
                .llmModel(resolvedModel)
                .status(llmResult.isPresent() ? "COMPLETED" : "FAILED")
                .grade(score != null ? Grade.fromScore(score).name() : null)
                .score(score)
                .summary(llmResult.map(LlmEvaluationResponseDTO::getSummary).orElse(null))
                .detailedComment(llmResult.map(LlmEvaluationResponseDTO::getDetailedComment).orElse(null))
                .build();
    }
}
