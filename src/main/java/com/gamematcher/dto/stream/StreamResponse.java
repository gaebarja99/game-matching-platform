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
    /** 간편 모드: 트위치/유튜브 URL. 있으면 watch 페이지에서 iframe 임베드 */
    private String externalUrl;
    private Long userId;
    /** 방송을 송출하는 사용자 닉네임(또는 이름) */
    private String broadcasterNickname;
    /** 방송자 프로필 이미지 URL (카드/방제목 옆 표시용) */
    private String broadcasterProfileImageUrl;
    /** 해당 방송자의 팔로워 수 */
    private Long followerCount;
    /** 현재 시청자 수 (실시간 집계) */
    private Integer viewerCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    /** 채팅 얼리기: true면 방장만 채팅 가능 */
    private Boolean chatFrozen;
    /** BJ 공지 (채팅창 상단 노출) */
    private String streamNotice;
    /** BJ 공지 노출 여부 */
    private Boolean streamNoticeVisible;
    /** 영상 후원 최소 팡 (0/null = 제한 없음) */
    private Integer minVideoPang;
    /** TTS(후원 메시지) 최소 팡 (0/null = 제한 없음) */
    private Integer minTtsPang;
    /** 파트너 스트리머 여부 (닉네임 옆 체크 표시용) */
    private Boolean partner;

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

    public static StreamResponse from(LiveStream stream, String broadcasterNickname, Long followerCount, Integer viewerCount, String broadcasterProfileImageUrl, StreamerTier streamerTier) {
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
                .build();
    }
}
