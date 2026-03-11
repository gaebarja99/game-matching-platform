package com.gamematcher.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostUpdateRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 1, max = 200)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000)
    private String content;

    @Size(max = 10)
    private List<String> hashtags;
}
