package com.gamematcher.service.pubg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.dto.pubg.map.PubgMapRegionConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * PUBG 맵 설정을 resources/maps/에서 로드합니다.
 *
 * <ul>
 *   <li>pubg_map_name.json: 맵 ID → 표시명</li>
 *   <li>*_regions.json: 맵별 그리드·POI 설정</li>
 * </ul>
 */
@Component
public class PubgMapConfigLoader {

    private static final String MAPS_BASE = "maps/";
    private static final String MAP_NAME_FILE = "pubg_map_name.json";

    private final ObjectMapper objectMapper;

    private Map<String, String> mapNameMap;
    private Map<String, PubgMapRegionConfig> regionConfigByMapId;

    public PubgMapConfigLoader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 맵 ID → 표시명 (Erangel_Main → Erangel 등)
     */
    public Map<String, String> getMapNameMap() {
        if (mapNameMap == null) {
            mapNameMap = loadMapNameMap();
        }
        return mapNameMap;
    }

    /**
     * 맵 ID로 지역 설정 조회.
     */
    public Optional<PubgMapRegionConfig> getRegionConfig(String mapId) {
        if (regionConfigByMapId == null) {
            regionConfigByMapId = loadAllRegionConfigs();
        }
        return Optional.ofNullable(regionConfigByMapId.get(mapId));
    }

    /**
     * 맵 ID에 해당하는 표시명 반환.
     */
    public String getMapDisplayName(String mapId) {
        return getMapNameMap().getOrDefault(mapId, mapId);
    }

    private Map<String, String> loadMapNameMap() {
        try (InputStream in = new ClassPathResource(MAPS_BASE + MAP_NAME_FILE).getInputStream()) {
            TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(in, typeRef);
        } catch (IOException e) {
            return Map.of();
        }
    }

    private Map<String, PubgMapRegionConfig> loadAllRegionConfigs() {
        Map<String, PubgMapRegionConfig> result = new HashMap<>();
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:" + MAPS_BASE + "*_regions.json");
            for (var resource : resources) {
                if (!resource.exists()) continue;
                try (InputStream in = resource.getInputStream()) {
                    PubgMapRegionConfig config = objectMapper.readValue(in, PubgMapRegionConfig.class);
                    for (String mapId : config.getApplicableMapIds()) {
                        result.put(mapId, config);
                    }
                } catch (IOException e) {
                    // 개별 파일 로드 실패 시 스킵
                }
            }
        } catch (IOException e) {
            // 패턴 리소스 조회 실패
        }
        return result;
    }

    /**
     * 캐시 초기화. 설정 파일 변경 후 재로드 시 사용.
     */
    public void clearCache() {
        mapNameMap = null;
        regionConfigByMapId = null;
    }
}
