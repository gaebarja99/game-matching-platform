package com.gamematcher.dto.community;

import com.gamematcher.constant.community.BoardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequestDto {

    @NotNull(message = "게시판 카테고리는 필수입니다.")
    private BoardCategory boardCategory;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 1, max = 200)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000)
    private String content;

    private boolean isNotice = false;

    @Size(max = 10)
    private List<String> hashtags;
}
