package com.gamematcher.service.pubg;

import com.gamematcher.dto.pubg.map.PubgMapRegionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PUBG 맵 설정 로더")
class PubgMapConfigLoaderTest {

    private PubgMapConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PubgMapConfigLoader();
    }

    @Test
    @DisplayName("맵 ID → 표시명 로드")
    void getMapNameMap() {
        Map<String, String> map = loader.getMapNameMap();

        assertThat(map).isNotEmpty();
        assertThat(map.get("Erangel_Main")).isEqualTo("Erangel");
        assertThat(map.get("Desert_Main")).isEqualTo("Miramar");
    }

    @Test
    @DisplayName("맵 ID로 표시명 조회")
    void getMapDisplayName() {
        assertThat(loader.getMapDisplayName("Erangel_Main")).isEqualTo("Erangel");
        assertThat(loader.getMapDisplayName("Unknown_Map")).isEqualTo("Unknown_Map");
    }

    @Test
    @DisplayName("에란겔 지역 설정 로드")
    void getRegionConfig_erangel() {
        Optional<PubgMapRegionConfig> config = loader.getRegionConfig("Baltic_Main");

        assertThat(config).isPresent();
        assertThat(config.get().getMapName()).isEqualTo("Erangel");
        assertThat(config.get().getPois()).isNotEmpty();
    }

    @Test
    @DisplayName("미구현 맵은 empty")
    void getRegionConfig_unknown() {
        Optional<PubgMapRegionConfig> config = loader.getRegionConfig("Unknown_Map");
        assertThat(config).isEmpty();
    }
}
