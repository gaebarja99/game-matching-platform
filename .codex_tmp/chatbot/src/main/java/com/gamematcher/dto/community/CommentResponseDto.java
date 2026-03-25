package com.gamematcher.dto.community;

import com.gamematcher.entity.community.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class CommentResponseDto {

    private Long id;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private Long parentId;
    private String content;
    private boolean isDeleted;
    private List<CommentResponseDto> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponseDto from(Comment comment, boolean includeReplies) {
        CommentResponseDto dto = new CommentResponseDto();
        dto.id = comment.getId();
        dto.postId = comment.getPost().getId();
        dto.authorId = comment.getAuthor().getId();
        dto.authorUsername = comment.getAuthor().getUsername();
        dto.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        dto.content = comment.isDeleted() ? "(삭제된 댓글입니다)" : comment.getContent();
        dto.isDeleted = comment.isDeleted();
        dto.replies = includeReplies && !comment.getReplies().isEmpty()
                ? comment.getReplies().stream()
                    .map(c -> CommentResponseDto.from(c, false))
                    .collect(Collectors.toList())
                : null;
        dto.createdAt = comment.getCreatedAt();
        dto.updatedAt = comment.getUpdatedAt();
        return dto;
    }
}
