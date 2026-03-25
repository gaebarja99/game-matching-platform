package com.gamematcher.service.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.search.MatchDetailRequest;
import com.gamematcher.dto.search.MatchDetailResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.service.lol.LolApiService;
import com.gamematcher.service.pubg.PubgApiService;
import com.gamematcher.service.tft.TftApiService;
import com.gamematcher.entity.match.valorant.ValorantMatchAiEvaluation;
import com.gamematcher.entity.match.valorant.ValorantMatchPlayer;
import com.gamematcher.service.valorant.ValorantApiService;
import com.gamematcher.service.valorant.ValorantMatchService;
import com.gamematcher.repository.match.ValorantMatchAiEvaluationRepository;
import com.gamematcher.repository.match.ValorantMatchPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Records 페이지용 매치 상세 지연 로드
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordsMatchDetailService {

    @Value("${ai.llm.model:gpt-5-mini}")
    private String defaultLlmModel;

    private final ValorantApiService valorantApiService;
    private final ValorantMatchService valorantMatchService;
    private final LolApiService lolApiService;
    private final TftApiService tftApiService;
    private final PubgApiService pubgApiService;
    private final ObjectMapper objectMapper;
    private final ValorantMatchPlayerRepository valorantMatchPlayerRepository;
    private final ValorantMatchAiEvaluationRepository valorantMatchAiEvaluationRepository;

    public MatchDetailResponse load(MatchDetailRequest req) {
        req.normalize();
        String game = req.getGame();
        String matchId = req.getMatchId();
        if (game == null || game.isBlank()) {
            return MatchDetailResponse.error(null, matchId, "game이 필요합니다.");
        }
        if (matchId == null || matchId.isBlank()) {
            return MatchDetailResponse.error(game, null, "matchId가 필요합니다.");
        }
        try {
            return switch (game) {
                case "valorant" -> loadValorant(matchId, req.getPuuid(), req.getLlmModel());
                case "lol" -> loadLol(matchId, req.getRegion());
                case "tft" -> loadTft(matchId, req.getRegion(), req.getPuuid());
                case "pubg" -> loadPubg(matchId, req.getPlatform());
                default -> MatchDetailResponse.error(game, matchId,
                        "이 게임은 매치 상세 지연 로드를 지원하지 않습니다: " + game);
            };
        } catch (Exception e) {
            log.warn("매치 상세 로드 실패 game={} matchId={}: {}", game, matchId, e.getMessage());
            return MatchDetailResponse.error(game, matchId, e.getMessage());
        }
    }

    private MatchDetailResponse loadValorant(String matchId, String puuid, String llmModel) {
        ValorantMatchDetailDto dto = valorantApiService.getMatchDetail(matchId);
        if (dto == null) {
            return MatchDetailResponse.error("valorant", matchId, "매치를 찾을 수 없습니다.");
        }
        try {
            valorantMatchService.replaceMatchFromApi(dto);
        } catch (Exception e) {
            log.debug("Valorant 매치 상세 DB 저장 생략: {}", e.getMessage());
        }
        Map<String, Object> payload = objectMapper.convertValue(dto, new TypeReference<>() {});
        attachValorantAiEvaluationIfPresent(payload, matchId, puuid, llmModel);
        return MatchDetailResponse.builder()
                .success(true)
                .game("valorant")
                .matchId(matchId)
                .payload(payload)
                .build();
    }

    /**
     * DB에 저장된 해당 매치·puuid AI 평가가 있으면 payload에 넣어 전적 화면 AI 탭에서 표시한다.
     */
    private void attachValorantAiEvaluationIfPresent(
            Map<String, Object> payload, String matchId, String puuid, String llmModel) {
        if (puuid == null || puuid.isBlank()) {
            return;
        }
        Optional<ValorantMatchPlayer> playerOpt =
                valorantMatchPlayerRepository.findByMatch_MatchIdAndPuuid(matchId, puuid.trim());
        if (playerOpt.isEmpty()) {
            return;
        }
        String modelKey = (llmModel != null && !llmModel.isBlank())
                ? llmModel.trim()
                : (defaultLlmModel != null ? defaultLlmModel.trim() : "");
        Optional<ValorantMatchAiEvaluation> evalOpt = valorantMatchAiEvaluationRepository
                .findByValorantMatchPlayer_IdAndLlmModel(playerOpt.get().getId(), modelKey);
        if (evalOpt.isEmpty() && modelKey.equals(defaultLlmModel != null ? defaultLlmModel.trim() : "")) {
            evalOpt = valorantMatchAiEvaluationRepository.findByValorantMatchPlayer_IdAndLlmModel(
                    playerOpt.get().getId(), "");
        }
        if (evalOpt.isEmpty()) {
            return;
        }
        ValorantMatchAiEvaluation e = evalOpt.get();
        String summary = e.getSummary();
        String detailed = e.getDetailedComment();
        boolean hasText = (summary != null && !summary.isBlank())
                || (detailed != null && !detailed.isBlank());
        if (!hasText && e.getGrade() == null && e.getScore() == null) {
            return;
        }
        Map<String, Object> ai = new LinkedHashMap<>();
        if (e.getLlmModel() != null && !e.getLlmModel().isBlank()) {
            ai.put("llmModel", e.getLlmModel());
        }
        if (e.getStatus() != null) {
            ai.put("status", e.getStatus().name());
        }
        if (e.getGrade() != null) {
            ai.put("grade", e.getGrade().name());
        }
        if (e.getScore() != null) {
            ai.put("score", e.getScore());
        }
        if (summary != null && !summary.isBlank()) {
            ai.put("summary", summary);
        }
        if (detailed != null && !detailed.isBlank()) {
            ai.put("detailedComment", detailed);
        }
        payload.put("records_ai_evaluation", ai);
    }

    private MatchDetailResponse loadLol(String matchId, String region) {
        Map<String, Object> raw = lolApiService.fetchMatchV5RawForRecords(matchId, region);
        if (raw == null || raw.isEmpty()) {
            return MatchDetailResponse.error("lol", matchId, "매치를 불러올 수 없습니다.");
        }
        lolApiService.persistMatchV5FromSearchMap(raw);
        return MatchDetailResponse.builder()
                .success(true)
                .game("lol")
                .matchId(matchId)
                .payload(raw)
                .build();
    }

    private MatchDetailResponse loadTft(String matchId, String region, String puuid) {
        Map<String, Object> raw = tftApiService.fetchTftMatchRawForRecords(matchId, region);
        if (raw == null || raw.isEmpty()) {
            return MatchDetailResponse.error("tft", matchId, "매치를 불러올 수 없습니다.");
        }
        if (puuid != null && !puuid.isBlank()) {
            try {
                tftApiService.upsertTftMatchFromRiotMapForRecords(raw, puuid.trim());
            } catch (Exception e) {
                log.debug("TFT 매치 상세 DB 저장 생략: {}", e.getMessage());
            }
        }
        return MatchDetailResponse.builder()
                .success(true)
                .game("tft")
                .matchId(matchId)
                .payload(raw)
                .build();
    }

    private MatchDetailResponse loadPubg(String matchId, String platform) {
        if (platform == null || platform.isBlank()) {
            return MatchDetailResponse.error("pubg", matchId, "PUBG는 platform(steam/kakao 등)이 필요합니다.");
        }
        Map<String, Object> raw = pubgApiService.fetchMatchRawForRecords(matchId, platform);
        if (raw == null || raw.isEmpty()) {
            return MatchDetailResponse.error("pubg", matchId, "매치를 불러올 수 없습니다.");
        }
        pubgApiService.persistMatchFromSearchMap(raw);
        return MatchDetailResponse.builder()
                .success(true)
                .game("pubg")
                .matchId(matchId)
                .payload(raw)
                .build();
    }
}
