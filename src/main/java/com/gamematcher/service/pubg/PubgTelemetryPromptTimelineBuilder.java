package com.gamematcher.service.pubg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.ai.evaluation.PubgTimelineEventLineDTO;
import com.gamematcher.entity.match.pubg.PubgTelemetryEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PUBG 텔레메트리 이벤트(통합 테이블) → 프롬프트 타임라인 라인 목록.
 *
 * <p>MVP 목표로, 문서의 샘플링 규칙을 "근사" 구현합니다.</p>
 */
@Component
public class PubgTelemetryPromptTimelineBuilder {

    private final ObjectMapper objectMapper;
    private final PubgMapRegionMapper regionMapper;

    public PubgTelemetryPromptTimelineBuilder(ObjectMapper objectMapper,
                                               PubgMapRegionMapper regionMapper) {
        this.objectMapper = objectMapper;
        this.regionMapper = regionMapper;
    }

    public List<PubgTimelineEventLineDTO> buildTimelineLines(
            String mapName,
            List<PubgTelemetryEvent> accountEvents,
            int maxTimelineLines
    ) {
        if (accountEvents == null || accountEvents.isEmpty()) return List.of();

        List<PubgTelemetryEvent> events = new ArrayList<>(accountEvents);
        events.sort(Comparator.comparing(PubgTelemetryEvent::getEventTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Instant firstTs = events.stream()
                .map(PubgTelemetryEvent::getEventTimestamp)
                .filter(t -> t != null)
                .findFirst()
                .orElse(null);
        if (firstTs == null) return List.of();

        Set<String> combatTypes = Set.of(
                "LogPlayerAttack",
                "LogPlayerTakeDamage",
                "LogPlayerMakeGroggy",
                "LogPlayerKill",
                "LogPlayerKillV2"
        );

        Set<String> healTypes = Set.of("LogHeal");
        Set<String> positionTypes = new HashSet<>(List.of("LogPlayerPosition", "LogPlayerCreate"));
        Set<String> itemTypes = new HashSet<>(List.of("LogItemEquip", "LogItemPickup", "LogItemDrop"));
        Set<String> vehicleTypes = new HashSet<>(List.of("LogVehicleRide", "LogVehicleLeave", "LogVehicleDestroy"));

        // 위치 라벨용: 가장 가까운 교전 이벤트 기준 델타 초를 계산
        List<Instant> combatInstants = new ArrayList<>();
        for (PubgTelemetryEvent e : events) {
            if (e.getEventTimestamp() != null && combatTypes.contains(e.getEventType())) {
                combatInstants.add(e.getEventTimestamp());
            }
        }

        // 샘플링 상태
        Instant lastNormalPosSampleAt = null;
        long lastCombatSecondSampled = -1;

        Instant lastItemSampleAt = null;

        Instant lastVehicleSampleAt = null;

        List<PubgTimelineEventLineDTO> out = new ArrayList<>();

        int combatCount = 0;
        int maxCombatCount = maxTimelineLines > 0 ? Math.max(10, Math.min(80, maxTimelineLines / 2)) : 80;

        for (PubgTelemetryEvent e : events) {
            if (out.size() >= maxTimelineLines && maxTimelineLines > 0) break;
            Instant ts = e.getEventTimestamp();
            if (ts == null) continue;
            long elapsedSeconds = Duration.between(firstTs, ts).getSeconds();

            String type = e.getEventType();
            if (combatTypes.contains(type)) {
                if (combatCount++ > maxCombatCount) continue;
                String label = type.startsWith("LogPlayerKill") ? "결과" : "교전";
                out.add(PubgTimelineEventLineDTO.builder()
                        .label(label)
                        .elapsedSeconds(elapsedSeconds)
                        .message(buildCombatMessage(e))
                        .build());
                continue;
            }

            if (healTypes.contains(type)) {
                out.add(PubgTimelineEventLineDTO.builder()
                        .label("회복")
                        .elapsedSeconds(elapsedSeconds)
                        .message(buildHealMessage(e))
                        .build());
                if (out.size() >= maxTimelineLines && maxTimelineLines > 0) break;
                continue;
            }

            if (positionTypes.contains(type)) {
                String region = regionMapper.toRegionName(mapName,
                        safe(e.getLocationX()), safe(e.getLocationY()), safe(e.getLocationZ()));

                PositionLabelResult pl = decidePositionLabel(ts, combatInstants);
                boolean shouldInclude;
                if (pl.label.equals("교전") || pl.label.equals("위험")) {
                    // 1초당 1개
                    shouldInclude = (elapsedSeconds != lastCombatSecondSampled);
                    if (shouldInclude) lastCombatSecondSampled = elapsedSeconds;
                } else {
                    // 운영: 25초당 1개
                    if (lastNormalPosSampleAt == null) {
                        shouldInclude = true;
                    } else {
                        shouldInclude = Duration.between(lastNormalPosSampleAt, ts).getSeconds() >= 25;
                    }
                    if (shouldInclude) lastNormalPosSampleAt = ts;
                }

                if (!shouldInclude) continue;

                String bluezone = e.getIsInBlueZone() == null ? "" : (e.getIsInBlueZone() ? "자기장 안" : "자기장 밖");
                String msg = region + (bluezone.isBlank() ? "" : " | " + bluezone);
                out.add(PubgTimelineEventLineDTO.builder()
                        .label(pl.label)
                        .elapsedSeconds(elapsedSeconds)
                        .message(msg)
                        .build());
                continue;
            }

            if (itemTypes.contains(type)) {
                boolean shouldInclude = (lastItemSampleAt == null)
                        || Duration.between(lastItemSampleAt, ts).getSeconds() >= 30;
                if (!shouldInclude) continue;
                lastItemSampleAt = ts;

                String region = regionMapper.toRegionName(mapName,
                        safe(e.getLocationX()), safe(e.getLocationY()), safe(e.getLocationZ()));

                String msg = buildItemMessage(e) + " | " + region;
                out.add(PubgTimelineEventLineDTO.builder()
                        .label("운영")
                        .elapsedSeconds(elapsedSeconds)
                        .message(msg)
                        .build());
                continue;
            }

            if (vehicleTypes.contains(type)) {
                // 차량 이벤트는 건수가 적을 거라서 기본적으로 전부(단, 너무 길면 제한)
                if (lastVehicleSampleAt != null && Duration.between(lastVehicleSampleAt, ts).getSeconds() < 5) {
                    continue;
                }
                lastVehicleSampleAt = ts;
                String region = regionMapper.toRegionName(mapName,
                        safe(e.getLocationX()), safe(e.getLocationY()), safe(e.getLocationZ()));
                out.add(PubgTimelineEventLineDTO.builder()
                        .label("운영")
                        .elapsedSeconds(elapsedSeconds)
                        .message(type + " | " + region)
                        .build());
                // 다음 이벤트로 진행
            }
        }

        return out;
    }

    private PositionLabelResult decidePositionLabel(Instant posTs, List<Instant> combatInstants) {
        if (combatInstants == null || combatInstants.isEmpty()) {
            return new PositionLabelResult("운영");
        }

        // posTs 기준 가장 가까운 교전 이벤트 1개만 보고 델타로 위험/교전 판정
        Instant nearest = null;
        long bestAbsDelta = Long.MAX_VALUE;
        for (Instant c : combatInstants) {
            long delta = Duration.between(c, posTs).getSeconds(); // pos - combat
            long abs = Math.abs(delta);
            if (abs < bestAbsDelta) {
                bestAbsDelta = abs;
                nearest = c;
            }
        }

        if (nearest == null) return new PositionLabelResult("운영");
        long deltaSec = Duration.between(nearest, posTs).getSeconds();

        // 문서 근사:
        // 진입: -30 ~ -10 => 위험
        // 교전: -10 ~ +5 => 교전
        if (deltaSec >= -30 && deltaSec < -10) {
            return new PositionLabelResult("위험");
        }
        if (deltaSec >= -10 && deltaSec <= 5) {
            return new PositionLabelResult("교전");
        }
        return new PositionLabelResult("운영");
    }

    private String buildItemMessage(PubgTelemetryEvent e) {
        if (e.getItemId() == null || e.getItemId().isBlank()) {
            return e.getEventType();
        }
        String cat = e.getItemCategory() != null ? e.getItemCategory() : "";
        return (cat.isBlank() ? "" : (cat + " ")) + e.getItemId();
    }

    private String buildHealMessage(PubgTelemetryEvent e) {
        String amount = tryExtractAnyNumber(e.getPayloadJson(), "healAmount", "amount", "heal");
        return amount != null ? "회복 발생 (" + amount + ")" : "회복 발생";
    }

    private String buildCombatMessage(PubgTelemetryEvent e) {
        // MVP: payloadJson에서 damage/무기 키가 존재하면 간단히 첨부, 아니면 이벤트 타입만 사용.
        if (e.getPayloadJson() == null || e.getPayloadJson().isBlank()) {
            return e.getEventType();
        }
        try {
            JsonNode node = objectMapper.readTree(e.getPayloadJson());

            String damageReason = tryExtractString(node, "damageReason");
            String causerName = tryExtractString(node, "damageCauserName");
            String itemId = tryExtractString(node, "itemId");
            String damage = tryExtractAnyNumberNode(node, "damage", "amount", "Damage");

            String base = e.getEventType();
            StringBuilder sb = new StringBuilder(base);
            if (damage != null) sb.append(" | 데미지 ").append(damage);
            if (causerName != null && !causerName.isBlank()) sb.append(" | 원인 ").append(causerName);
            if (damageReason != null && !damageReason.isBlank()) sb.append(" | 이유 ").append(damageReason);
            if (itemId != null && !itemId.isBlank()) sb.append(" | 아이템 ").append(itemId);
            return sb.toString();
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            return e.getEventType();
        }
    }

    private Double safe(Double v) {
        return v != null ? v : 0.0;
    }

    private String tryExtractString(JsonNode node, String key) {
        if (node == null) return null;
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) return null;
        String s = v.asText(null);
        return s != null ? s : null;
    }

    private String tryExtractAnyNumber(String json, String... keys) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            return tryExtractAnyNumberNode(node, keys);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            return null;
        }
    }

    private String tryExtractAnyNumberNode(JsonNode node, String... keys) {
        if (node == null) return null;
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && v.isNumber()) {
                return v.asText();
            }
            if (v != null && !v.isNull() && v.isTextual()) {
                String s = v.asText();
                if (s != null && s.matches("-?\\d+(\\.\\d+)?")) {
                    return s;
                }
            }
        }
        return null;
    }

    private static class PositionLabelResult {
        private final String label;
        private PositionLabelResult(String label) {
            this.label = label;
        }
    }
}

