package com.gamematcher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * pubg_match_sample.json 파일을 Jackson으로 pretty-print 포맷팅
 */
class JsonPrettyPrintTest {

    @Test
    void formatPubgMatchSample() throws Exception {
        Path path = Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json");
        String json = Files.readString(path);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        String formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);

        Files.writeString(path, formatted);
    }
}
