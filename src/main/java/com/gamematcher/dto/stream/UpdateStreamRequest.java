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
public class UpdateStreamRequest {

    @NotBlank(message = "제목을 입력해 주세요")
    private String title;

    @NotNull(message = "게임을 선택해 주세요")
    private GameList game;
}
