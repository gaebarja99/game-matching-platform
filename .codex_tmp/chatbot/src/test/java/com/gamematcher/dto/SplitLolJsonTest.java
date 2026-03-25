package com.gamematcher.dto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/** LoL 통합 JSON을 매치·타임라인 파일로 분리 (일회성 실행용) */
class SplitLolJsonTest {

    @Test
    @Disabled("수동 실행 시에만 사용")
    void splitLolJson() throws Exception {
        String content = Files.readString(Paths.get("src/test/resources/samples/lol/lol_sample_json.txt"));
        int matchSection = content.indexOf("페이커 1판 전적");
        int timelineSection = content.indexOf("위의 전적 타임라인");
        int matchStart = content.indexOf("{", matchSection);
        int timelineStart = content.indexOf("{", timelineSection);

        int depth = 0;
        int matchEnd = matchStart;
        for (int i = matchStart; i < timelineSection; i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    matchEnd = i + 1;
                    break;
                }
            }
        }

        Files.writeString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"),
                content.substring(matchStart, matchEnd));
        Files.writeString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"),
                content.substring(timelineStart));

        System.out.println("Created lol_match_sample.json and lol_timeline_sample.json");
    }
}
