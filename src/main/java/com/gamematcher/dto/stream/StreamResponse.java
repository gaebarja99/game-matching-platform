package com.gamematcher.dto.stream;

import com.gamematcher.constant.GameList;
import com.gamematcher.constant.StreamStatus;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.entity.LiveStream;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StreamResponse {

    private Long id;
    private String title;
    private GameList game;
    private StreamStatus status;
    private String playbackUrl;
    private String externalUrl;
    private Long userId;
    private String broadcasterNickname;
    private String broadcasterProfileImageUrl;
    private Long followerCount;
    private Integer viewerCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private Boolean chatFrozen;
    private String streamNotice;
    private Boolean streamNoticeVisible;
    private Integer minVideoPang;
    private Integer minTtsPang;
    private Boolean partner;
    private Boolean visibleInRecent;
    private Integer adminWarningCount;
    private LocalDateTime lastAdminWarningAt;
    private String lastAdminWarningMessage;

    public static StreamResponse from(LiveStream stream) {
        return from(stream, null);
    }

    public static StreamResponse from(LiveStream stream, String broadcasterNickname) {
        return from(stream, broadcasterNickname, null);
    }

    public static StreamResponse from(LiveStream stream, String broadcasterNickname, Long followerCount) {
        return from(stream, broadcasterNickname, followerCount, null);
    }

    public static StreamResponse from(LiveStream stream, String broadcasterNickname, Long followerCount, Integer viewerCount) {
        return from(stream, broadcasterNickname, followerCount, viewerCount, null);
    }

    public static StreamResponse from(LiveStream stream, String broadcasterNickname, Long followerCount, Integer viewerCount, String broadcasterProfileImageUrl) {
        return from(stream, broadcasterNickname, followerCount, viewerCount, broadcasterProfileImageUrl, null);
    }

    public static StreamResponse from(
            LiveStream stream,
            String broadcasterNickname,
            Long followerCount,
            Integer viewerCount,
            String broadcasterProfileImageUrl,
            StreamerTier streamerTier
    ) {
        return StreamResponse.builder()
                .id(stream.getId())
                .title(stream.getTitle())
                .game(stream.getGame())
                .status(stream.getStatus())
                .playbackUrl(stream.getPlaybackUrl())
                .externalUrl(stream.getExternalUrl())
                .userId(stream.getUserId())
                .broadcasterNickname(broadcasterNickname)
                .broadcasterProfileImageUrl(broadcasterProfileImageUrl)
                .followerCount(followerCount != null ? followerCount : 0L)
                .viewerCount(viewerCount != null ? viewerCount : 0)
                .startedAt(stream.getStartedAt())
                .endedAt(stream.getEndedAt())
                .createdAt(stream.getCreatedAt())
                .chatFrozen(stream.getChatFrozen() != null ? stream.getChatFrozen() : false)
                .streamNotice(stream.getStreamNotice())
                .streamNoticeVisible(stream.getStreamNoticeVisible() != null ? stream.getStreamNoticeVisible() : false)
                .minVideoPang(stream.getMinVideoPang() != null ? stream.getMinVideoPang() : 0)
                .minTtsPang(stream.getMinTtsPang() != null ? stream.getMinTtsPang() : 0)
                .partner(streamerTier == StreamerTier.PARTNER)
                .visibleInRecent(stream.getVisibleInRecent() != null ? stream.getVisibleInRecent() : true)
                .adminWarningCount(stream.getAdminWarningCount() != null ? stream.getAdminWarningCount() : 0)
                .lastAdminWarningAt(stream.getLastAdminWarningAt())
                .lastAdminWarningMessage(stream.getLastAdminWarningMessage())
                .build();
    }
}
