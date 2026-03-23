package com.gamematcher.service.pubg;

import com.gamematcher.dto.pubg.PubgMatchApiResponse;
import com.gamematcher.dto.pubg.PubgPlayerApiResponse;
import com.gamematcher.dto.pubg.PubgTelemetryEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PUBG JSON 파싱 서비스 테스트")
class PubgJsonServiceTest {

    private final PubgJsonService service = new PubgJsonService();

    @Test
    @DisplayName("parseMatchResponse - 매치 API JSON 파싱")
    void parseMatchResponse() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_sample.json"));

        PubgMatchApiResponse response = service.parseMatchResponse(json);

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo("04192032-7e46-4d3a-a430-28817c8c5bcc");
        assertThat(response.getData().getAttributes().getGameMode()).isEqualTo("tdm");
        assertThat(response.getIncluded()).hasSize(5);
    }

    @Test
    @DisplayName("parsePlayerResponse - 플레이어 API JSON 파싱")
    void parsePlayerResponse() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_player_sample.json"));

        PubgPlayerApiResponse response = service.parsePlayerResponse(json);

        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo("account.fe1027e418594343bafd39e9685239e2");
        assertThat(response.getData().get(0).getAttributes().getName()).isEqualTo("8ink-");
        assertThat(response.getData().get(0).getRelationships().getMatchIds()).isNotEmpty();
    }

    @Test
    @DisplayName("parseTelemetryResponse - 텔레메트리 API JSON 파싱")
    void parseTelemetryResponse() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/samples/pubg/pubg_match_included_sample.json"));

        List<PubgTelemetryEventDto> events = service.parseTelemetryResponse(json);

        assertThat(events).isNotEmpty();
        assertThat(events.get(0).getType()).isEqualTo("LogMatchDefinition");
        assertThat(events.get(0).getTimestamp()).isEqualTo("2026-03-16T15:42:19.2863143Z");
        assertThat(events.get(0).getAdditional()).containsKey("MatchId");

        PubgTelemetryEventDto loginEvent = events.stream()
                .filter(e -> "LogPlayerLogin".equals(e.getType()))
                .findFirst()
                .orElseThrow();
        assertThat(loginEvent.getAdditional()).containsKey("accountId");
    }

    @Test
    @DisplayName("parseMatchResponse - 빈 JSON 예외")
    void parseMatchResponse_empty_throws() {
        assertThatThrownBy(() -> service.parseMatchResponse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비어있습니다");
    }

    @Test
    @DisplayName("parseTelemetryResponse - 빈 JSON은 빈 리스트 반환")
    void parseTelemetryResponse_empty_returnsEmpty() {
        List<PubgTelemetryEventDto> result = service.parseTelemetryResponse("");
        assertThat(result).isEmpty();
    }
}
