spackage com.gamematcher.dto.lol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LoL 매치 API 래퍼 응답 DTO
 * - Riot API는 래퍼 없이 단일 매치를 반환하지만, 커스텀/프록시 API에서 사용 가능
 * - 형식: { "matches": [...] } 또는 { "data": [...] }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolMatchApiResponse {

    @JsonProperty("matches")
    private List<LolMatchDetailDto> matches;

    @JsonProperty("data")
    private List<LolMatchDetailDto> data;

    /** matches 또는 data 중 비어있지 않은 목록 반환 */
    public List<LolMatchDetailDto> getMatchList() {
        if (matches != null && !matches.isEmpty()) {
            return matches;
        }
        return data;
    }
}
