package com.gamematcher.service.pubg;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.pubg.PubgTelemetryEventDto;
import com.gamematcher.dto.pubg.PubgTelemetryEventRowDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * PUBG 텔레메트리(배열 JSON)에서 event를 스트리밍 방식으로 읽어,
 * 역정규화 통합 테이블(row DTO)로 정규화합니다.
 */
@Component
public class PubgTelemetryStreamExtractor {

    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;

    public PubgTelemetryStreamExtractor() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.jsonFactory = this.objectMapper.getFactory();
    }

    /**
     * 스트리밍 파싱:
     * - JSON 최상위 배열을 순서대로 읽습니다.
     * - {@code maxEvents}까지 처리한 뒤 중단합니다. (maxEvents <= 0 이면 제한 없음)
     */
    public void iterateEvents(InputStream inputStream, int maxEvents, Consumer<PubgTelemetryEventRowDto> consumer)
            throws IOException {
        if (inputStream == null) return;
        if (consumer == null) return;

        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            JsonToken token = parser.nextToken();
            if (token != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("PUBG 텔레메트리 JSON은 최상위 배열이어야 합니다.");
            }

            int seq = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                PubgTelemetryEventDto dto = objectMapper.readValue(parser, PubgTelemetryEventDto.class);

                PubgTelemetryEventRowDto row = toRow(seq, dto);
                consumer.accept(row);

                seq++;
                if (maxEvents > 0 && seq >= maxEvents) {
                    break;
                }
            }
        }
    }

    private PubgTelemetryEventRowDto toRow(int seq, PubgTelemetryEventDto dto) {
        String eventType = dto.getType();
        Instant eventTimestamp = parseInstant(dto.getTimestamp());

        Map<String, Object> additional = dto.getAdditional();

        String accountId = extractAccountId(additional);
        Integer teamId = extractTeamId(additional);

        Location loc = extractLocation(additional);
        Boolean isInBlueZone = extractIsInBlueZone(additional);

        Item item = extractItem(additional);

        String payloadJson = toPayloadJson(eventType, dto.getTimestamp(), additional);

        return PubgTelemetryEventRowDto.builder()
                .eventSequence(seq)
                .eventType(eventType)
                .eventTimestamp(eventTimestamp)
                .accountId(accountId)
                .teamId(teamId)
                .locationX(loc.x)
                .locationY(loc.y)
                .locationZ(loc.z)
                .isInBlueZone(isInBlueZone)
                .itemId(item.itemId)
                .itemCategory(item.itemCategory)
                .payloadJson(payloadJson)
                .build();
    }

    private String toPayloadJson(String type, String timestamp, Map<String, Object> additional) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("_T", type);
            payload.put("_D", timestamp);
            if (additional != null) {
                payload.putAll(additional);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Instant parseInstant(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String extractAccountId(Map<String, Object> additional) {
        if (additional == null) return null;

        Object rootAccountId = additional.get("accountId");
        if (rootAccountId instanceof String s && !s.isBlank()) {
            return s;
        }

        Object characterObj = additional.get("character");
        if (characterObj instanceof Map<?, ?> character) {
            Object cid = character.get("accountId");
            if (cid instanceof String s && !s.isBlank()) return s;
        }

        return null;
    }

    private Integer extractTeamId(Map<String, Object> additional) {
        if (additional == null) return null;
        Object characterObj = additional.get("character");
        if (!(characterObj instanceof Map<?, ?> character)) return null;
        Object teamIdObj = character.get("teamId");
        return toInteger(teamIdObj);
    }

    private Boolean extractIsInBlueZone(Map<String, Object> additional) {
        if (additional == null) return null;
        Object characterObj = additional.get("character");
        if (!(characterObj instanceof Map<?, ?> character)) return null;
        Object v = character.get("isInBlueZone");
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return null;
    }

    private Location extractLocation(Map<String, Object> additional) {
        Location l = new Location();
        if (additional == null) return l;

        Object characterObj = additional.get("character");
        if (!(characterObj instanceof Map<?, ?> character)) return l;

        Object locationObj = character.get("location");
        if (!(locationObj instanceof Map<?, ?> location)) return l;

        l.x = toDouble(location.get("x"));
        l.y = toDouble(location.get("y"));
        l.z = toDouble(location.get("z"));
        return l;
    }

    private Item extractItem(Map<String, Object> additional) {
        if (additional == null) return new Item();
        Object itemObj = additional.get("item");
        if (!(itemObj instanceof Map<?, ?> item)) return new Item();

        Item i = new Item();
        Object itemId = item.get("itemId");
        Object category = item.get("category");
        i.itemId = itemId instanceof String s ? s : null;
        i.itemCategory = category instanceof String s ? s : null;
        return i;
    }

    private Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Double d) return d;
        if (v instanceof Float f) return (double) f;
        if (v instanceof Integer i) return (double) i;
        if (v instanceof Long l) return (double) l;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static class Location {
        private Double x;
        private Double y;
        private Double z;
    }

    private static class Item {
        private String itemId;
        private String itemCategory;
    }
}

