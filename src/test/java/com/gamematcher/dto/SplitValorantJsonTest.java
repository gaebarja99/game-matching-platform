package com.gamematcher.dto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/** Valorant 통합 JSON을 매치·puuid 파일로 분리 (일회성 실행용) */
class SplitValorantJsonTest {

    @Test
    @Disabled("수동 실행 시에만 사용")
    void splitValorantJson() throws Exception {
        String content = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_sample_json.txt"));
        int matchSection = content.indexOf("1판 전적");
        int puuidStart = content.indexOf("{", 0);
        int matchStart = content.indexOf("{", matchSection);

        int depth = 0;
        int puuidEnd = puuidStart;
        for (int i = puuidStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    puuidEnd = i + 1;
                    break;
                }
            }
        }

        Files.writeString(Paths.get("src/test/resources/samples/valorant/valorant_puuid_sample.json"),
                content.substring(puuidStart, puuidEnd));
        Files.writeString(Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"),
                content.substring(matchStart));

        System.out.println("Created valorant_puuid_sample.json and valorant_match_sample.json");
    }
}
