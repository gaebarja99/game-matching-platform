package com.gamematcher.controller.account;

import com.gamematcher.dto.pubg.PubgAiEvaluationResponseDto;
import com.gamematcher.service.pubg.PubgAiEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PUBG AI 평가 API (LLM 코멘트 반환).
 *
 * <p>{@code accountId} 또는 {@code playerName}을 주면 해당 플레이어만 평가합니다. 둘 다 생략 시 매치 전원.</p>
 */
@RestController
@RequestMapping("/api/pubg")
public class PubgController {

    private final PubgAiEvaluationService pubgAiEvaluationService;

    public PubgController(PubgAiEvaluationService pubgAiEvaluationService) {
        this.pubgAiEvaluationService = pubgAiEvaluationService;
    }

    @PostMapping("/evaluations/match/{matchId}")
    public ResponseEntity<List<PubgAiEvaluationResponseDto>> evaluateMatch(
            @PathVariable String matchId,
            @RequestParam(defaultValue = "120") int maxTimelineLines,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String playerName
    ) {
        return ResponseEntity.ok(
                pubgAiEvaluationService.evaluateMatch(matchId, maxTimelineLines, accountId, playerName)
        );
    }
}

