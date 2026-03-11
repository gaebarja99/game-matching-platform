package com.gamematcher.dto.valorant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValorantDtoDeserializeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void valorantMatchJson_역직렬화_성공() throws Exception {
        String jsonPath = "src/test/resources/samples/valorant/valorant_match_sample.json";
        String json = java.nio.file.Files.readString(
                java.nio.file.Paths.get(jsonPath)
        );

        ValorantMatchApiResponse response = objectMapper.readValue(
                json,
                ValorantMatchApiResponse.class
        );

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isNotEmpty();

        ValorantMatchDetailDto match = response.getData().get(0);
        assertThat(match.getMetadata()).isNotNull();           // ValorantMatchInfoDto
        assertThat(match.getMetadata().getMap()).isEqualTo("Pearl");
        assertThat(match.getPlayers()).isNotNull();
        assertThat(match.getPlayers().getAllPlayers()).isNotEmpty();  // ValorantPlayerDto[]
        assertThat(match.getTeams()).isNotNull();
        assertThat(match.getRounds()).isNotEmpty();
        assertThat(match.getKills()).isNotEmpty();
    }
}
