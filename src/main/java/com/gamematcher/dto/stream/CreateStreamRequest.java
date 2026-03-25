package com.gamematcher.dto.stream;

import com.gamematcher.constant.GameList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateStreamRequest {

    @NotBlank(message = "제목을 입력해 주세요")
    private String title;

    @NotNull(message = "게임을 선택해 주세요")
    private GameList game;

    /** 간편 모드: 트위치 또는 유튜브 방송 URL. 넣으면 서버 설정 없이 해당 방송만 사이트에 임베드 */
    private String externalUrl;
}
