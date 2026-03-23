package com.gamematcher.dto.lol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LolMatchDetailDto 역직렬화 테스트")
class LolMatchDetailDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("LoL 매치 JSON을 LolMatchDetailDto로 파싱한다")
    void lolMatchJson_역직렬화_성공() throws Exception {
        String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
        LolMatchDetailDto dto = objectMapper.readValue(matchJson, LolMatchDetailDto.class);

        assertThat(dto.getMetadata()).isNotNull();
        assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8136533346");
        assertThat(dto.getMetadata().getParticipants()).hasSize(10);

        assertThat(dto.getInfo()).isNotNull();
        assertThat(dto.getInfo().getGameId()).isEqualTo(8136533346L);
        assertThat(dto.getInfo().getGameMode()).isEqualTo("CLASSIC");
        assertThat(dto.getInfo().getQueueId()).isEqualTo(420);
        assertThat(dto.getInfo().getParticipants()).hasSize(10);
        assertThat(dto.getInfo().getTeams()).hasSize(2);

        String fakerPuuid = "ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA";
        LolParticipantDto faker = dto.getInfo().getParticipants().stream()
                .filter(p -> fakerPuuid.equals(p.getPuuid()))
                .findFirst()
                .orElseThrow();
        assertThat(faker.getChampionName()).isNotNull();
        assertThat(faker.getKills()).isNotNull();
        assertThat(faker.getDeaths()).isNotNull();
        assertThat(faker.getAssists()).isNotNull();
    }
}
