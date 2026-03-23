package com.gamematcher.service;

import com.gamematcher.dto.search.PlayerSearchRequest;
import com.gamematcher.dto.search.PlayerSearchResponse;
import org.springframework.stereotype.Service;

@Service
public class PlayerSearchService {

    public PlayerSearchResponse searchPlayer(PlayerSearchRequest request) {
        request.normalize();

        String nickname = request.getGameName();
        if ((nickname == null || nickname.isBlank()) && request.getSteamId() != null) {
            nickname = request.getSteamId();
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = "unknown";
        }

        return PlayerSearchResponse.error(
                request.getGame(),
                nickname,
                "전적검색 UI는 압축본 기준으로 이식되었습니다."
                        + "\n실제 게임 API 연동 백엔드는 아직 추가 연결이 필요합니다."
                        + "\n원하시면 다음 단계로 압축본의 게임별 검색 서비스까지 이어서 붙이겠습니다."
        );
    }
}
