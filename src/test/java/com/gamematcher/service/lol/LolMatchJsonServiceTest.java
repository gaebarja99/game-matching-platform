package com.gamematcher.service.lol;

import com.gamematcher.dto.lol.LolMatchDetailDto;
import com.gamematcher.dto.lol.LolMatchTimelineDetailDto;
import com.gamematcher.service.lol.LolMatchJsonService.LolMatchJsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LolMatchJsonService 테스트")
class LolMatchJsonServiceTest {

    private LolMatchJsonService service;

    @BeforeEach
    void setUp() {
        service = new LolMatchJsonService();
    }

    @Nested
    @DisplayName("parseMatchDetail - 매치 전적 파싱")
    class ParseMatchDetailTest {

        @Test
        @DisplayName("매치 JSON을 LolMatchDetailDto로 파싱한다")
        void parseMatchDetail_validJson_returnsDto() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            LolMatchDetailDto dto = service.parseMatchDetail(matchJson);

            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
            assertThat(dto.getInfo().getParticipants()).hasSize(10);
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
    @DisplayName("parseTimelineDetail - 타임라인 파싱")
    class ParseTimelineDetailTest {

        @Test
        @DisplayName("타임라인 JSON을 LolMatchTimelineDetailDto로 파싱한다")
        void parseTimelineDetail_validJson_returnsDto() throws Exception {
            String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));
            LolMatchTimelineDetailDto dto = service.parseTimelineDetail(timelineJson);

            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
            assertThat(dto.getInfo().getFrameInterval()).isEqualTo(60000);
            assertThat(dto.getInfo().getFrames()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("parseFirstMatch - 유연한 매치 파싱")
    class ParseFirstMatchTest {

        @Test
        @DisplayName("단일 매치 JSON을 파싱한다")
        void parseFirstMatch_singleMatch_returnsDto() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            LolMatchDetailDto dto = service.parseFirstMatch(matchJson);

            assertThat(dto).isNotNull();
            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
            assertThat(dto.getInfo().getParticipants()).hasSize(10);
        }

        @Test
        @DisplayName("래퍼 형식(matches)에서 첫 매치를 추출한다")
        void parseFirstMatch_wrapperMatches_returnsFirst() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            String wrapped = "{\"matches\":[" + matchJson + "]}";
            LolMatchDetailDto dto = service.parseFirstMatch(wrapped);

            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
        }

        @Test
        @DisplayName("래퍼 형식(data)에서 첫 매치를 추출한다")
        void parseFirstMatch_wrapperData_returnsFirst() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            String wrapped = "{\"data\":[" + matchJson + "]}";
            LolMatchDetailDto dto = service.parseFirstMatch(wrapped);

            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
        }

        @Test
        @DisplayName("유효하지 않은 형식이면 예외를 던진다")
        void parseFirstMatch_invalidFormat_throwsException() {
            assertThatThrownBy(() -> service.parseFirstMatch("{}"))
                    .isInstanceOf(LolMatchJsonParseException.class)
                    .hasMessageContaining("유효한 LoL 매치 JSON 형식이 아닙니다");
        }
    }

    @Nested
    @DisplayName("parseFirstTimeline - 유연한 타임라인 파싱")
    class ParseFirstTimelineTest {

        @Test
        @DisplayName("단일 타임라인 JSON을 파싱한다")
        void parseFirstTimeline_singleTimeline_returnsDto() throws Exception {
            String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));
            LolMatchTimelineDetailDto dto = service.parseFirstTimeline(timelineJson);

            assertThat(dto).isNotNull();
            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
            assertThat(dto.getInfo().getFrames()).isNotEmpty();
        }

        @Test
        @DisplayName("래퍼 형식(timelines)에서 첫 타임라인을 추출한다")
        void parseFirstTimeline_wrapperTimelines_returnsFirst() throws Exception {
            String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));
            String wrapped = "{\"timelines\":[" + timelineJson + "]}";
            LolMatchTimelineDetailDto dto = service.parseFirstTimeline(wrapped);

            assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");
        }
    }

    @Nested
    @DisplayName("parseMatchesFromApiResponse - 래퍼에서 매치 목록 추출")
    class ParseMatchesFromApiResponseTest {

        @Test
        @DisplayName("래퍼 JSON에서 매치 목록을 추출한다")
        void parseMatchesFromApiResponse_validWrapper_returnsList() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            String wrapped = "{\"matches\":[" + matchJson + "]}";
            var list = service.parseMatchesFromApiResponse(wrapped);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getMetadata().getMatchId()).isEqualTo("KR_8121193767");
        }

        @Test
        @DisplayName("data가 비어있으면 빈 리스트를 반환한다")
        void parseMatchesFromApiResponse_emptyData_returnsEmptyList() {
            var list = service.parseMatchesFromApiResponse("{\"matches\":[]}");

            assertThat(list).isEmpty();
        }
    }

    @Nested
    @DisplayName("splitAndParse - 통합 JSON 분리 파싱")
    class SplitAndParseTest {

        @Test
        @DisplayName("매치+타임라인 연속 JSON을 분리하여 파싱한다")
        void splitAndParse_combined_returnsBoth() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));
            String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));
            String combined = matchJson + "\n" + timelineJson;

            var result = service.splitAndParse(combined);

            assertThat(result.match()).isNotNull();
            assertThat(result.match().getMetadata().getMatchId()).isEqualTo("KR_8121193767");
            assertThat(result.timeline()).isNotNull();
            assertThat(result.timeline().getMetadata().getMatchId()).isEqualTo("KR_8121193767");
        }

        @Test
        @DisplayName("매치만 있으면 timeline은 null이다")
        void splitAndParse_matchOnly_timelineNull() throws Exception {
            String matchJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_match_sample.json"));

            var result = service.splitAndParse(matchJson);

            assertThat(result.match()).isNotNull();
            assertThat(result.timeline()).isNull();
        }
    }
}
