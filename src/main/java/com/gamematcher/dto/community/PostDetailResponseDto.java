package com.gamematcher.dto.community;

import com.gamematcher.constant.community.BoardCategory;
import com.gamematcher.constant.community.PostStatus;
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
public class PostDetailResponseDto {

    private Long id;
    private BoardCategory boardCategory;
    private String title;
    private String content;
    private PostStatus status;
    private String statusLabel;
    private Long authorId;
    private String authorUsername;
    private int viewCount;
    private int likeCount;
    private int recommendCount;
    private int notRecommendCount;
    private int commentCount;
    private boolean isNotice;
    private List<String> hashtags;
    private List<AttachmentResponseDto> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean liked;
    private boolean bookmarked;
    private Integer myRecommend; // 1: 추천, -1: 비추천, null: 없음

    public static PostDetailResponseDto from(Post post, boolean liked, boolean bookmarked, Integer myRecommend) {
        PostDetailResponseDto dto = new PostDetailResponseDto();
        dto.id = post.getId();
        dto.boardCategory = post.getBoardCategory();
        dto.title = post.getTitle();
        dto.content = post.getContent();
        dto.status = post.getStatus();
        dto.statusLabel = post.getStatus() != null ? post.getStatus().getDisplayName() : "";
        dto.authorId = post.getAuthor().getId();
        dto.authorUsername = post.getAuthor().getUsername();
        dto.viewCount = post.getViewCount();
        dto.likeCount = post.getLikeCount();
        dto.recommendCount = post.getRecommendCount();
        dto.notRecommendCount = post.getNotRecommendCount();
        dto.commentCount = post.getCommentCount();
        dto.isNotice = post.isNotice();
        dto.hashtags = post.getPostHashtags().stream()
                .map(ph -> ph.getHashtag().getName())
                .collect(Collectors.toList());
        dto.attachments = post.getAttachments().stream()
                .map(AttachmentResponseDto::from)
                .collect(Collectors.toList());
        dto.createdAt = post.getCreatedAt();
        dto.updatedAt = post.getUpdatedAt();
        dto.liked = liked;
        dto.bookmarked = bookmarked;
        dto.myRecommend = myRecommend;
        return dto;
    }
}
