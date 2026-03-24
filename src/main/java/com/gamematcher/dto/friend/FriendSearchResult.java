package com.gamematcher.dto.friend;

import com.gamematcher.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendSearchResult {

    private Long id;
    private String loginId;
    private String nickname;
    private String profileImageUrl;

    public static FriendSearchResult from(User user) {
        String nick = user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
        return FriendSearchResult.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .nickname(nick)
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
