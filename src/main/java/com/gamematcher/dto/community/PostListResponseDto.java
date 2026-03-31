package com.gamematcher.dto.community;

import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.entity.community.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class PostListResponseDto {

    private Long id;
    private BoardCategory boardCategory;
    private String title;
    private Long authorId;
    private String authorUsername;
    private int viewCount;
    private int likeCount;
    private int recommendCount;
    private int commentCount;
    private boolean isNotice;
    private boolean isPopular;
    private List<String> hashtags;
    private LocalDateTime createdAt;

    public static PostListResponseDto from(Post post, boolean isPopular) {
        PostListResponseDto dto = new PostListResponseDto();
        dto.id = post.getId();
        dto.boardCategory = post.getBoardCategory();
        dto.title = post.getTitle();
        dto.authorId = post.getAuthor().getId();
        dto.authorUsername = post.getAuthor().getUsername();
        dto.viewCount = post.getViewCount();
        dto.likeCount = post.getLikeCount();
        dto.recommendCount = post.getRecommendCount();
        dto.commentCount = post.getCommentCount();
        dto.isNotice = post.isNotice();
        dto.isPopular = isPopular;
        dto.hashtags = post.getPostHashtags().stream()
                .map(ph -> ph.getHashtag().getName())
                .collect(Collectors.toList());
        dto.createdAt = post.getCreatedAt();
        return dto;
    }
}
