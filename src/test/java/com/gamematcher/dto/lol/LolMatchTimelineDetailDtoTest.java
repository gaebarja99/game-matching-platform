package com.gamematcher.dto.lol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LolMatchTimelineDetailDto 역직렬화 테스트")
class LolMatchTimelineDetailDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("LoL 타임라인 JSON을 LolMatchTimelineDetailDto로 파싱한다")
    void lolTimelineJson_역직렬화_성공() throws Exception {
        String timelineJson = Files.readString(Paths.get("src/test/resources/samples/lol/lol_timeline_sample.json"));

        LolMatchTimelineDetailDto dto = objectMapper.readValue(timelineJson, LolMatchTimelineDetailDto.class);

        assertThat(dto.getMetadata()).isNotNull();
        assertThat(dto.getMetadata().getMatchId()).isEqualTo("KR_8121193767");

        assertThat(dto.getInfo()).isNotNull();
        assertThat(dto.getInfo().getFrameInterval()).isEqualTo(60000);
        assertThat(dto.getInfo().getGameId()).isEqualTo(8121193767L);
        assertThat(dto.getInfo().getFrames()).isNotEmpty();
        assertThat(dto.getInfo().getParticipants()).hasSize(10);

        var firstFrame = dto.getInfo().getFrames().get(0);
        assertThat(firstFrame.getEvents()).isNotEmpty();
        assertThat(firstFrame.getParticipantFrames()).isNotEmpty();
        assertThat(firstFrame.getParticipantFrames()).containsKey("1");

        var participantFrame = firstFrame.getParticipantFrames().get("1");
        assertThat(participantFrame.getParticipantId()).isEqualTo(1);
        assertThat(participantFrame.getLevel()).isEqualTo(1);
        assertThat(participantFrame.getCurrentGold()).isEqualTo(500);
        assertThat(participantFrame.getChampionStats()).isNotNull();
        assertThat(participantFrame.getPosition()).isNotNull();
    }
}
