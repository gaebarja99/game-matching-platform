package com.gamematcher.controller.account;

import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.entity.match.lol.LolMatch;
import com.gamematcher.service.lol.LolMatchJsonService;
import com.gamematcher.service.lol.LolMatchJsonService.LolMatchJsonParseException;
import com.gamematcher.service.lol.LolMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * LoL 매치 JSON 파싱 및 DB 저장 API
 */
@RestController
@RequestMapping("/api/lol")
public class LolController {

    private final LolMatchJsonService lolMatchJsonService;
    private final LolMatchService lolMatchService;

    public LolController(LolMatchJsonService lolMatchJsonService, LolMatchService lolMatchService) {
        this.lolMatchJsonService = lolMatchJsonService;
        this.lolMatchService = lolMatchService;
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
}
