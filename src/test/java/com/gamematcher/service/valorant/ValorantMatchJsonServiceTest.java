package com.gamematcher.service.valorant;

import com.gamematcher.dto.valorant.ValorantMatchApiResponse;
import com.gamematcher.dto.valorant.ValorantMatchDetailDto;
import com.gamematcher.service.valorant.ValorantMatchJsonService.ValorantMatchJsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValorantMatchJsonService 테스트")
class ValorantMatchJsonServiceTest {

    private ValorantMatchJsonService service;
    private String apiResponseJson;
    private String singleMatchJson;

    @BeforeEach
    void setUp() throws Exception {
        service = new ValorantMatchJsonService();
        apiResponseJson = Files.readString(Paths.get("src/test/resources/samples/valorant/valorant_match_sample.json"));
        // 단일 매치 JSON = data[0] 추출
        ValorantMatchApiResponse parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(apiResponseJson, ValorantMatchApiResponse.class);
        singleMatchJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(parsed.getData().get(0));
    }

    @Nested
    @DisplayName("parseApiResponse - API 응답 형식 파싱")
    class ParseApiResponseTest {

        @Test
        @DisplayName("API 응답 JSON을 ValorantMatchApiResponse로 파싱한다")
        void parseApiResponse_validJson_returnsResponse() {
            ValorantMatchApiResponse response = service.parseApiResponse(apiResponseJson);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get(0).getMetadata().getMap()).isEqualTo("Pearl");
        assertThat(response.getData().get(0).getMetadata().getMatchId())
                .isEqualTo("d8d224b9-b56e-4be2-b235-d996fbe22bcf");
        }

        @Test
        @DisplayName("null 또는 빈 JSON이면 예외를 던진다")
        void parseApiResponse_nullOrBlank_throwsException() {
            assertThatThrownBy(() -> service.parseApiResponse((String) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비어있습니다");

            assertThatThrownBy(() -> service.parseApiResponse(""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> service.parseApiResponse("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("잘못된 JSON이면 ValorantMatchJsonParseException을 던진다")
        void parseApiResponse_invalidJson_throwsException() {
            assertThatThrownBy(() -> service.parseApiResponse("not valid json"))
                    .isInstanceOf(ValorantMatchJsonParseException.class)
                    .hasMessageContaining("파싱 실패");
        }

    }

    @Nested
    @DisplayName("parseMatchesFromApiResponse - JSON 쪼개기")
    class ParseMatchesFromApiResponseTest {

        @Test
        @DisplayName("API 응답 JSON을 쪼개어 매치 목록으로 반환한다")
        void parseMatchesFromApiResponse_validJson_returnsMatchList() {
            List<ValorantMatchDetailDto> matches = service.parseMatchesFromApiResponse(apiResponseJson);

            assertThat(matches).isNotEmpty();
            assertThat(matches.get(0).getMetadata().getMap()).isEqualTo("Pearl");
            assertThat(matches.get(0).getMetadata().getMatchId())
                    .isEqualTo("d8d224b9-b56e-4be2-b235-d996fbe22bcf");
        }

        @Test
        @DisplayName("data가 비어있으면 빈 리스트를 반환한다")
        void parseMatchesFromApiResponse_emptyData_returnsEmptyList() {
            String emptyDataJson = "{\"status\":200,\"data\":[]}";
            List<ValorantMatchDetailDto> matches = service.parseMatchesFromApiResponse(emptyDataJson);

            assertThat(matches).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseMatchDetail - 단일 매치 파싱")
    class ParseMatchDetailTest {

        @Test
        @DisplayName("단일 매치 JSON을 ValorantMatchDetailDto로 파싱한다")
        void parseMatchDetail_validJson_returnsDto() {
            ValorantMatchDetailDto dto = service.parseMatchDetail(singleMatchJson);

            assertThat(dto).isNotNull();
            assertThat(dto.isAvailable()).isTrue();
            assertThat(dto.getMetadata()).isNotNull();
            assertThat(dto.getMetadata().getMap()).isEqualTo("Pearl");
            assertThat(dto.getPlayers()).isNotNull();
            assertThat(dto.getPlayers().getAllPlayers()).isNotEmpty();
            assertThat(dto.getRounds()).isNotEmpty();
            assertThat(dto.getKills()).isNotEmpty();
        }

        @Test
        @DisplayName("null 또는 빈 JSON이면 예외를 던진다")
        void parseMatchDetail_nullOrBlank_throwsException() {
            assertThatThrownBy(() -> service.parseMatchDetail((String) null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> service.parseMatchDetail(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("parseFirstMatch - 첫 번째 매치 추출")
    class ParseFirstMatchTest {

        @Test
        @DisplayName("API 응답 JSON에서 첫 번째 매치를 추출한다")
        void parseFirstMatch_apiResponse_returnsFirstMatch() {
            ValorantMatchDetailDto match = service.parseFirstMatch(apiResponseJson);

            assertThat(match).isNotNull();
            assertThat(match.getMetadata().getMatchId()).isEqualTo("d8d224b9-b56e-4be2-b235-d996fbe22bcf");
            assertThat(match.getMetadata().getMap()).isEqualTo("Pearl");
        }

        @Test
        @DisplayName("단일 매치 JSON에서 매치를 추출한다")
        void parseFirstMatch_singleMatch_returnsMatch() {
            ValorantMatchDetailDto match = service.parseFirstMatch(singleMatchJson);

            assertThat(match).isNotNull();
            assertThat(match.getMetadata().getMap()).isEqualTo("Pearl");
        }

        @Test
        @DisplayName("유효하지 않은 형식이면 예외를 던진다")
        void parseFirstMatch_invalidFormat_throwsException() {
            assertThatThrownBy(() -> service.parseFirstMatch("{}"))
                    .isInstanceOf(ValorantMatchJsonParseException.class)
                    .hasMessageContaining("유효한 Valorant 전적 JSON 형식이 아닙니다");
        }
    }
}
