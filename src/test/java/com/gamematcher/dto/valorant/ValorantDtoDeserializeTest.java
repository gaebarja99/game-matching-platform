package com.gamematcher.dto.valorant;

import com.gamematcher.service.valorant.ValorantMatchJsonService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ValorantDtoDeserializeTest {

    private final ValorantMatchJsonService jsonService = new ValorantMatchJsonService();

    @Test
    void valorantMatchJson_역직렬화_성공() throws Exception {
        String jsonPath = "src/test/resources/samples/valorant/valorant_match_sample.json";
        String json = Files.readString(Paths.get(jsonPath));

        // parseFirstMatch: data가 배열 또는 객체 모두 지원
        ValorantMatchDetailDto match = jsonService.parseFirstMatch(json);

        assertThat(match.getMetadata()).isNotNull();
        assertThat(match.getMetadata().getMap()).isNotBlank();
        assertThat(match.getPlayers()).isNotNull();
        assertThat(match.getPlayers().getAllPlayers()).isNotEmpty();
        assertThat(match.getTeams()).isNotNull();
        assertThat(match.getRounds()).isNotEmpty();
        assertThat(match.getKills()).isNotEmpty();
    }
}
