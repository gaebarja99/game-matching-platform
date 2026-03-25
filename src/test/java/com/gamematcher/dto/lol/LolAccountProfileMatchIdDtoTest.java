package com.gamematcher.dto.lol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoL Account/Profile/MatchId DTO 역직렬화 테스트")
class LolAccountProfileMatchIdDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("lol_puuid_sample.json을 LolAccountResponseDto로 파싱한다")
    void lolPuuidJson_역직렬화_성공() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_puuid_sample.json"));
        LolAccountResponseDto dto = objectMapper.readValue(json, LolAccountResponseDto.class);

        assertThat(dto.getPuuid()).isEqualTo("ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA");
        assertThat(dto.getGameName()).isEqualTo("Hide on bush");
        assertThat(dto.getTagLine()).isEqualTo("KR1");
    }

    @Test
    @DisplayName("lol_profile_sample.json을 LolSummonerProfileDto로 파싱한다")
    void lolProfileJson_역직렬화_성공() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_profile_sample.json"));
        LolSummonerProfileDto dto = objectMapper.readValue(json, LolSummonerProfileDto.class);

        assertThat(dto.getPuuid()).isEqualTo("ggrdvya4vWUKvZHsaUbvX9B80tSUH6jLAwjuSxxM0AbrhIciAFne_emQXGppZ7I6mJaYAb_JKZYAFA");
        assertThat(dto.getProfileIconId()).isEqualTo(6);
        assertThat(dto.getRevisionDate()).isEqualTo(1773666214000L);
        assertThat(dto.getSummonerLevel()).isEqualTo(888);
    }

    @Test
    @DisplayName("lol_match_id_list_sample.json을 LolMatchIdListResponseDto로 파싱한다")
    void lolMatchIdListJson_역직렬화_성공() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_id_list_sample.json"));
        LolMatchIdListResponseDto dto = objectMapper.readValue(json, LolMatchIdListResponseDto.class);

        assertThat(dto.getMatchIds()).containsExactly(
                "KR_8136533346",
                "KR_8136458444",
                "KR_8136370937",
                "KR_8136318964",
                "KR_8136054224"
        );
    }
}
