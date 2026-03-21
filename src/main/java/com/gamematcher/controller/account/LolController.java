package com.gamematcher.controller.account;

import com.gamematcher.dto.ai.evaluation.LlmEvaluationResponseDTO;
import com.gamematcher.dto.ai.evaluation.LolPlayerMatchStatsDTO;
import com.gamematcher.dto.lol.LolAiEvaluationRequestDto;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.mapper.LolMatchStatsMapper;
import com.gamematcher.service.ai.LolLlmEvaluationService;
import com.gamematcher.service.lol.LolMatchJsonService;
import com.gamematcher.service.lol.LolMatchJsonService.LolMatchJsonParseException;
import com.gamematcher.service.lol.LolMatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * LoL 매치 JSON 파싱, DB 저장, AI 평가 API
 */
@RestController
@RequestMapping("/api/lol")
public class LolController {

    private final LolMatchJsonService lolMatchJsonService;
    private final LolMatchService lolMatchService;
    private final LolMatchStatsMapper lolMatchStatsMapper;
    private final LolLlmEvaluationService lolLlmEvaluationService;

    public LolController(LolMatchJsonService lolMatchJsonService,
                         LolMatchService lolMatchService,
                         LolMatchStatsMapper lolMatchStatsMapper,
                         LolLlmEvaluationService lolLlmEvaluationService) {
        this.lolMatchJsonService = lolMatchJsonService;
        this.lolMatchService = lolMatchService;
        this.lolMatchStatsMapper = lolMatchStatsMapper;
        this.lolLlmEvaluationService = lolLlmEvaluationService;
    }

    /**
     * LoL 매치 JSON을 DB에 저장
     * - 단일 매치: { "metadata", "info" }
     * - 래퍼: { "matches"/"data": [...] }
     */
    @PostMapping("/matches/save")
    public ResponseEntity<?> saveMatchJson(@RequestBody String json) {
        try {
            LolMatchDetailDto matchDto = lolMatchJsonService.parseFirstMatch(json);
            LolMatch saved = lolMatchService.saveMatch(matchDto);
            if (saved != null) {
                return ResponseEntity.ok("LoL 매치 저장 완료: " + saved.getMatchId());
            }
            return ResponseEntity.ok("이미 존재하는 매치입니다: " + matchDto.getMetadata().getMatchId());
        } catch (LolMatchJsonParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * LoL 매치 + 타임라인 JSON을 DB에 저장
     * - request body: 매치 JSON + 타임라인 JSON이 연속된 문자열 (예: matchJson + "\n" + timelineJson)
     * - 매치만 있으면 timeline은 null로 저장
     */
    @PostMapping("/matches/save-with-timeline")
    public ResponseEntity<?> saveMatchWithTimelineJson(@RequestBody String json) {
        try {
            var parsed = lolMatchJsonService.splitAndParse(json);
            LolMatch saved = lolMatchService.saveMatchWithTimeline(parsed.match(), parsed.timeline());
            if (saved != null) {
                return ResponseEntity.ok("LoL 매치+타임라인 저장 완료: " + saved.getMatchId());
            }
            return ResponseEntity.ok("이미 존재하는 매치입니다: " + parsed.match().getMetadata().getMatchId());
        } catch (LolMatchJsonParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * LoL 플레이어 매치 AI 평가
     * - matchJson + timelineJson(선택) + 대상 지정({@code playerPuuid} &gt; {@code playerDisplayName} &gt; {@code playerIndex})
     *
     * @param request matchJson, timelineJson, 대상 필드 중 하나
     * @return summary, detailedComment (API 키 없음·실패 시 200 + summary/detailedComment null)
     */
    @PostMapping("/evaluations/evaluate")
    public ResponseEntity<?> evaluatePlayer(
            @Valid @RequestBody LolAiEvaluationRequestDto request) {
        try {
            LolMatchDetailDto matchDto = lolMatchJsonService.parseFirstMatch(request.getMatchJson());
            if (matchDto == null) {
                return ResponseEntity.badRequest().body("매치 JSON 파싱 실패");
            }
            LolMatchDetailDto.Info info = matchDto.getInfo();
            if (info == null || info.getParticipants() == null || info.getParticipants().isEmpty()) {
                return ResponseEntity.badRequest().body("매치에 플레이어 정보가 없습니다.");
            }

            var timelineDto = (request.getTimelineJson() != null && !request.getTimelineJson().isBlank())
                    ? lolMatchJsonService.parseFirstTimeline(request.getTimelineJson())
                    : null;

            List<LolPlayerMatchStatsDTO> playerStatsList = lolMatchStatsMapper.toPlayerMatchStatsDtos(
                    matchDto, timelineDto);

            boolean hasPuuid = request.getPlayerPuuid() != null && !request.getPlayerPuuid().isBlank();
            boolean hasDisplay = request.getPlayerDisplayName() != null && !request.getPlayerDisplayName().isBlank();
            boolean hasIndex = request.getPlayerIndex() != null;

            if (!hasPuuid && !hasDisplay && !hasIndex) {
                return ResponseEntity.badRequest().body(
                        "playerPuuid, playerDisplayName, playerIndex 중 하나는 필수입니다.");
            }

            LolPlayerMatchStatsDTO playerStats;
            if (hasPuuid) {
                String puuid = request.getPlayerPuuid().trim();
                playerStats = playerStatsList.stream()
                        .filter(s -> puuid.equals(s.getPlayerPuuid()))
                        .findFirst()
                        .orElse(null);
                if (playerStats == null) {
                    return ResponseEntity.badRequest().body("매치에 해당 playerPuuid 참가자가 없습니다: " + puuid);
                }
            } else if (hasDisplay) {
                String display = request.getPlayerDisplayName().trim();
                playerStats = playerStatsList.stream()
                        .filter(s -> s.getPlayerDisplayName() != null
                                && display.equalsIgnoreCase(s.getPlayerDisplayName().trim()))
                        .findFirst()
                        .orElse(null);
                if (playerStats == null) {
                    return ResponseEntity.badRequest().body("매치에 해당 playerDisplayName 참가자가 없습니다: " + display);
                }
            } else {
                int rawIdx = request.getPlayerIndex();
                if (rawIdx < 0 || rawIdx > 9) {
                    return ResponseEntity.badRequest().body("playerIndex는 0~9여야 합니다.");
                }
                if (rawIdx >= playerStatsList.size()) {
                    return ResponseEntity.badRequest().body(
                            "playerIndex가 참가자 수를 벗어났습니다. size=" + playerStatsList.size());
                }
                playerStats = playerStatsList.get(rawIdx);
            }

            Optional<LlmEvaluationResponseDTO> result = lolLlmEvaluationService.evaluate(playerStats);

            return ResponseEntity.ok(result.orElse(new LlmEvaluationResponseDTO()));
        } catch (LolMatchJsonParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
