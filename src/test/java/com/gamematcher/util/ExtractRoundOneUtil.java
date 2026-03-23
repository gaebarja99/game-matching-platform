package com.gamematcher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * valorant_match_sample.json에서 1라운드만 추출해 valorant_match_one_round_sample.json 생성.
 * 실행: mvn test -Dtest=ExtractRoundOneUtil#extractRoundOne
 */
public class ExtractRoundOneUtil {

    public static void main(String[] args) throws Exception {
        extractRoundOne();
    }

    public static void extractRoundOne() throws Exception {
        Path src = Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json");
        Path dst = Paths.get("src/test/resources/samples/valorant/valorant_match_one_round_sample.json");

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(Files.readString(src));

        JsonNode data = root.get("data");
        if (data == null || !data.isObject()) {
            throw new IllegalStateException("Expected data object");
        }

        ObjectNode outData = data.deepCopy();
        ArrayNode rounds = (ArrayNode) outData.get("rounds");
        if (rounds == null || rounds.isEmpty()) {
            throw new IllegalStateException("No rounds found");
        }

        ArrayNode roundOne = om.createArrayNode();
        roundOne.add(rounds.get(0));
        outData.set("rounds", roundOne);

        ArrayNode allKills = (ArrayNode) outData.get("kills");
        if (allKills != null) {
            ArrayNode killsRoundOne = om.createArrayNode();
            for (JsonNode k : allKills) {
                JsonNode r = k.get("round");
                if (r != null && r.asInt() == 0) {
                    killsRoundOne.add(k.deepCopy());
                }
            }
            outData.set("kills", killsRoundOne);
        }

        ObjectNode result = om.createObjectNode();
        result.put("status", root.get("status").asInt());
        result.set("data", outData);

        String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        Files.writeString(dst, json);
        System.out.println("Wrote 1 round to " + dst);
    }
}
