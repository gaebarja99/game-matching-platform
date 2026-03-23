package com.gamematcher.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentCreateRequestDto {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 2000)
    private String content;

    private Long parentId; // 대댓글인 경우 부모 댓글 ID
}
