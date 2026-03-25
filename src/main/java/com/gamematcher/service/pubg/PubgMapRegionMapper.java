package com.gamematcher.service.pubg;

import com.gamematcher.dto.pubg.map.PubgMapRegionConfig;
import com.gamematcher.dto.pubg.map.PubgPoiConfig;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PUBG 좌표(x,y) → 지역명(POI 중심)을 대략 변환합니다.
 *
 * <p>초기 MVP에서는 그리드(cells/bounds)가 없는 리소스도 있으므로,
 * POI 중심 판별만 수행합니다.</p>
 *
 * <p>맵/POI로 특정할 수 없을 때는 문구 뒤에 {@code | 좌표 x=…, y=…, z=…}를 붙입니다.</p>
 *
 * <p>텔레메트리 좌표 및 {@code maps/*_regions.json}의 POI center·반경은 cm 단위(언리얼 월드)로 가정하고,
 * 화면/프롬프트에 적는 거리만 m로 환산합니다.</p>
 */
@Component
public class PubgMapRegionMapper {

    /** PUBG 전장 좌표 스케일: 1m = 100cm (언리얼 기본) */
    private static final double CM_PER_METER = 100.0;

    /**
     * PUBG 매치 API의 {@code mapName}과 {@code maps/*_regions.json}의 {@code mapId}가
     * 항상 일치하지 않습니다. (예: Rondo → API는 {@code Neon_Main}, 리소스는 {@code Ron_Main})
     * 리소스 키로 조회되도록 API ID → regions 파일 mapId 매핑을 둡니다.
     */
    private static final Map<String, String> PUBG_API_MAP_ID_TO_REGION_CONFIG_ID = Map.of(
            "Neon_Main", "Ron_Main",
            "Erangel_Main", "Baltic_Main",
            "Chimera_Main", "P_Main",
            "Summerland_Main", "Karakin_Main",
            "DihorOtok_Main", "Vikendi_Main"
    );

    private final PubgMapConfigLoader configLoader;

    public PubgMapRegionMapper(PubgMapConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public String toRegionName(String mapName, double x, double y, double z) {
        if (mapName == null || mapName.isBlank()) {
            return appendCoordsIfUnknown("맵 미상 지역", x, y, z);
        }

        String trimmed = mapName.trim();
        Optional<PubgMapRegionConfig> cfgOpt = resolveRegionConfig(trimmed);
        if (cfgOpt.isEmpty()) {
            return appendCoordsIfUnknown(mapContextLabel(trimmed) + " (지역 설정 없음)", x, y, z);
        }

        PubgMapRegionConfig cfg = cfgOpt.get();
        List<PubgPoiConfig> pois = cfg.getPois();
        if (pois == null || pois.isEmpty()) {
            return appendCoordsIfUnknown(mapContextLabel(trimmed, cfg) + " (POI 없음)", x, y, z);
        }

        // POI 중 거리 가장 가까운 항목을 기준으로 중심부/인근을 결정
        PubgPoiConfig nearest = pois.stream()
                .filter(p -> p.getCenter() != null && p.getCenter().size() >= 2)
                .min(Comparator.comparingDouble(p -> distanceCm(
                        x, y, p.getCenter().get(0), p.getCenter().get(1)
                )))
                .orElse(null);

        if (nearest == null || nearest.getCenter() == null || nearest.getCenter().size() < 2) {
            return appendCoordsIfUnknown(mapContextLabel(trimmed, cfg) + " (유효 POI 없음)", x, y, z);
        }

        double centerX = nearest.getCenter().get(0);
        double centerY = nearest.getCenter().get(1);
        double distCm = distanceCm(x, y, centerX, centerY);

        Double radiusObj = nearest.getRadius();
        double radius = radiusObj != null ? radiusObj : 0d;
        Double nearRadiusObj = nearest.getNearRadius();
        double nearRadius = nearRadiusObj != null ? nearRadiusObj : radius;

        if (radius > 0 && distCm <= radius) {
            return nearest.getNameKo() + " 중심부";
        }

        if (nearRadius > 0 && distCm <= nearRadius) {
            String dir = direction8Korean(centerX, centerY, x, y);
            return nearest.getNameKo() + " " + dir + " " + formatDistanceMeters(distCm) + " 지점";
        }

        // POI 인접 반경(nearRadius) 밖: 좌표 기준 8방위 + 거리 (단순 "인근" 대신)
        String dirOuter = direction8Korean(centerX, centerY, x, y);
        return nearest.getNameKo() + " 기준 " + dirOuter + " " + formatDistanceMeters(distCm) + " (외곽)";
    }

    private Optional<PubgMapRegionConfig> resolveRegionConfig(String mapName) {
        Optional<PubgMapRegionConfig> direct = configLoader.getRegionConfig(mapName);
        if (direct.isPresent()) {
            return direct;
        }
        String altId = PUBG_API_MAP_ID_TO_REGION_CONFIG_ID.get(mapName);
        if (altId != null && !altId.isBlank()) {
            return configLoader.getRegionConfig(altId);
        }
        return Optional.empty();
    }

    private String mapContextLabel(String apiMapId) {
        return mapContextLabel(apiMapId, null);
    }

    private String mapContextLabel(String apiMapId, PubgMapRegionConfig cfg) {
        if (cfg != null && cfg.getMapName() != null && !cfg.getMapName().isBlank()) {
            return cfg.getMapName();
        }
        String display = configLoader.getMapDisplayName(apiMapId);
        if (display != null && !display.isBlank()) {
            return display;
        }
        return apiMapId;
    }

    /**
     * 지역명을 특정하지 못한 경우 메시지 뒤에 월드 좌표를 붙입니다.
     */
    private static String appendCoordsIfUnknown(String label, double x, double y, double z) {
        return label + " | 좌표 x=" + formatCoord(x) + ", y=" + formatCoord(y) + ", z=" + formatCoord(z);
    }

    private static String formatCoord(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "?";
        }
        // 텔레메트리 좌표는 cm 정수 스케일
        return String.valueOf(Math.round(v)) + "cm";
    }

    /**
     * 평면 거리(cm). POI center·반경과 동일 단위로 비교합니다.
     */
    private static double distanceCm(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** 거리(cm)를 사람이 읽기 쉬운 m 문자열로 (예: {@code "약 236m"}, 1km 이상은 km 소수 1자리). */
    private static String formatDistanceMeters(double distCm) {
        double m = distCm / CM_PER_METER;
        if (Double.isNaN(m) || Double.isInfinite(m)) {
            return "?";
        }
        if (m >= 1000.0) {
            return String.format("약 %.1fkm", m / 1000.0);
        }
        if (m >= 10.0) {
            return "약 " + Math.round(m) + "m";
        }
        return String.format("약 %.1fm", m);
    }

    private String direction8Korean(double centerX, double centerY, double x, double y) {
        // dx, dy: 중심 → 현재
        double dx = x - centerX;
        double dy = y - centerY;

        // atan2는 [-pi, pi] 범위
        double angle = Math.atan2(dy, dx);

        // 8방위 분할(45도씩), 기준축은 동쪽(+x)
        double sector = (Math.PI / 4.0);
        int idx = (int) Math.floor((angle + Math.PI) / sector); // 0..7(근사)

        // idx 8칸 중에서 각 구간에 대응시키기 위한 회전 보정
        idx = idx % 8;

        return switch (idx) {
            case 0 -> "남서쪽";
            case 1 -> "서쪽";
            case 2 -> "북서쪽";
            case 3 -> "북쪽";
            case 4 -> "북동쪽";
            case 5 -> "동쪽";
            case 6 -> "남동쪽";
            case 7 -> "남쪽";
            default -> "주변";
        };
    }
}

