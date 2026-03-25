package com.gamematcher.service.lol;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.lol.LolMatchApiResponse;
import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineApiResponse;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * LoL 전적 JSON → DTO 매핑 서비스
 * Riot Match-v5 API 응답 형식 지원
 * - 단일 매치/타임라인: { "metadata", "info" }
 * - 래퍼 형식: { "matches"/"data": [...] }, { "timelines"/"data": [...] }
 */
@Service
public class LolMatchJsonService {

    private final ObjectMapper objectMapper;

    public LolMatchJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** 매치 전적 JSON 파일에서 파싱 */
    public LolMatchDetailDto parseMatchFromFile(String path) throws IOException {
        String json = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        return parseMatchDetail(json);
    }

    /** 타임라인 JSON 파일에서 파싱 */
    public LolMatchTimelineDetailDto parseTimelineFromFile(String path) throws IOException {
        String json = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        return parseTimelineDetail(json);
    }

    /**
     * LoL 매치 전적 JSON 파싱
     * 형식: { "metadata": {...}, "info": { "participants": [...], "teams": [...] } }
     *
     * @param json 매치 전적 JSON 문자열
     * @return LolMatchDetailDto
     */
    public LolMatchDetailDto parseMatchDetail(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, LolMatchDetailDto.class);
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 매치 전적 JSON 파싱 실패", e);
        }
    }

    /**
     * LoL 매치 타임라인 JSON 파싱
     * 형식: { "metadata": {...}, "info": { "frames": [...], "participantFrames": {...} } }
     *
     * @param json 타임라인 JSON 문자열
     * @return LolMatchTimelineDetailDto
     */
    public LolMatchTimelineDetailDto parseTimelineDetail(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, LolMatchTimelineDetailDto.class);
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 타임라인 JSON 파싱 실패", e);
        }
    }

    /**
     * API 래퍼 JSON 파싱 → 매치 목록 추출
     * 형식: { "matches": [...] } 또는 { "data": [...] }
     *
     * @param json API 응답 JSON 문자열
     * @return 매치 DTO 목록 (비어있으면 빈 리스트)
     */
    public List<LolMatchDetailDto> parseMatchesFromApiResponse(String json) {
        LolMatchApiResponse response = parseMatchApiResponse(json);
        List<LolMatchDetailDto> list = response.getMatchList();
        return list != null ? list : Collections.emptyList();
    }

    /**
     * API 래퍼 JSON 파싱
     *
     * @param json API 응답 JSON 문자열
     * @return LolMatchApiResponse
     */
    public LolMatchApiResponse parseMatchApiResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, LolMatchApiResponse.class);
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 매치 API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * API 래퍼 JSON 파싱 → 타임라인 목록 추출
     * 형식: { "timelines": [...] } 또는 { "data": [...] }
     *
     * @param json API 응답 JSON 문자열
     * @return 타임라인 DTO 목록 (비어있으면 빈 리스트)
     */
    public List<LolMatchTimelineDetailDto> parseTimelinesFromApiResponse(String json) {
        LolMatchTimelineApiResponse response = parseTimelineApiResponse(json);
        List<LolMatchTimelineDetailDto> list = response.getTimelineList();
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 타임라인 API 래퍼 JSON 파싱
     *
     * @param json API 응답 JSON 문자열
     * @return LolMatchTimelineApiResponse
     */
    public LolMatchTimelineApiResponse parseTimelineApiResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, LolMatchTimelineApiResponse.class);
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 타임라인 API 응답 JSON 파싱 실패", e);
        }
    }

    /**
     * 유연한 매치 JSON 파싱
     * - 단일 매치: { "metadata", "info" } (info에 participants, teams 등)
     * - 래퍼: { "matches"/"data": [...] } → 첫 번째 매치 반환
     *
     * @param json JSON 문자열 (단일 매치 또는 래퍼)
     * @return LolMatchDetailDto
     */
    public LolMatchDetailDto parseFirstMatch(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            JsonNode tree = objectMapper.readTree(json);
            // 래퍼 형식: matches 또는 data 배열
            JsonNode matchesNode = tree.has("matches") ? tree.get("matches") : tree.get("data");
            if (matchesNode != null && matchesNode.isArray() && matchesNode.size() > 0) {
                return objectMapper.treeToValue(matchesNode.get(0), LolMatchDetailDto.class);
            }
            // 단일 매치: metadata + info (info에 participants 배열)
            if (tree.has("metadata") && tree.has("info")) {
                JsonNode info = tree.get("info");
                if (info != null && (info.has("participants") || info.has("teams"))) {
                    return objectMapper.treeToValue(tree, LolMatchDetailDto.class);
                }
            }
            throw new LolMatchJsonParseException(
                    "유효한 LoL 매치 JSON 형식이 아닙니다 (metadata+info 또는 matches/data 배열 필요)");
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 매치 JSON 파싱 실패", e);
        }
    }

    /**
     * 유연한 타임라인 JSON 파싱
     * - 단일 타임라인: { "metadata", "info": { "frames", ... } }
     * - 래퍼: { "timelines"/"data": [...] } → 첫 번째 타임라인 반환
     *
     * @param json JSON 문자열 (단일 타임라인 또는 래퍼)
     * @return LolMatchTimelineDetailDto
     */
    public LolMatchTimelineDetailDto parseFirstTimeline(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        try {
            JsonNode tree = objectMapper.readTree(json);
            // 래퍼 형식: timelines 또는 data 배열
            JsonNode timelinesNode = tree.has("timelines") ? tree.get("timelines") : tree.get("data");
            if (timelinesNode != null && timelinesNode.isArray() && timelinesNode.size() > 0) {
                return objectMapper.treeToValue(timelinesNode.get(0), LolMatchTimelineDetailDto.class);
            }
            // 단일 타임라인: metadata + info.frames
            if (tree.has("metadata") && tree.has("info")) {
                JsonNode info = tree.get("info");
                if (info != null && info.has("frames")) {
                    return objectMapper.treeToValue(tree, LolMatchTimelineDetailDto.class);
                }
            }
            throw new LolMatchJsonParseException(
                    "유효한 LoL 타임라인 JSON 형식이 아닙니다 (metadata+info.frames 또는 timelines/data 배열 필요)");
        } catch (IOException e) {
            throw new LolMatchJsonParseException("LoL 타임라인 JSON 파싱 실패", e);
        }
    }

    /**
     * 통합 JSON(매치 + 타임라인 연속)에서 매치·타임라인 추출
     * 첫 번째 완전한 JSON 객체를 매치로, 두 번째를 타임라인으로 파싱
     *
     * @param combinedJson 매치 JSON + 타임라인 JSON이 연속된 문자열
     * @return 매치와 타임라인 쌍 (타임라인 없으면 timeline null)
     */
    public MatchWithTimeline splitAndParse(String combinedJson) {
        if (combinedJson == null || combinedJson.isBlank()) {
            throw new IllegalArgumentException("JSON이 비어있습니다.");
        }
        String trimmed = combinedJson.trim();
        int firstBrace = trimmed.indexOf('{');
        if (firstBrace < 0) {
            throw new LolMatchJsonParseException("JSON 객체를 찾을 수 없습니다.");
        }

        int depth = 0;
        int start = firstBrace;
        int matchEnd = -1;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    matchEnd = i + 1;
                    break;
                }
            }
        }
        if (matchEnd < 0) {
            throw new LolMatchJsonParseException("첫 번째 JSON 객체가 불완전합니다.");
        }

        LolMatchDetailDto match = parseMatchDetail(trimmed.substring(start, matchEnd));

        int secondBrace = trimmed.indexOf('{', matchEnd);
        LolMatchTimelineDetailDto timeline = null;
        if (secondBrace >= 0) {
            depth = 0;
            int timelineEnd = -1;
            for (int i = secondBrace; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        timelineEnd = i + 1;
                        break;
                    }
                }
            }
            if (timelineEnd > 0) {
                try {
                    timeline = parseTimelineDetail(trimmed.substring(secondBrace, timelineEnd));
                } catch (LolMatchJsonParseException ignored) {
                    // 타임라인이 아니면 null 유지
                }
            }
        }

        return new MatchWithTimeline(match, timeline);
    }

    /** 매치 + 타임라인 파싱 결과 */
    public record MatchWithTimeline(LolMatchDetailDto match, LolMatchTimelineDetailDto timeline) {}

    /**
     * LoL 전적 JSON 파싱 예외
     */
    public static class LolMatchJsonParseException extends RuntimeException {
        public LolMatchJsonParseException(String message) {
            super(message);
        }

        public LolMatchJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
