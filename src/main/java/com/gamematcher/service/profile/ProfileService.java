package com.gamematcher.service.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamematcher.dto.profile.ProfileMatchDto;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.entity.profile.UserProfile;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.UserRepository;
import com.gamematcher.repository.profile.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final int BIO_MAX_LEN = 500;
    private static final int IMAGE_URL_MAX_LEN = 512;
    private static final int PREFERRED_GAMES_MAX_LEN = 500;
    private static final int USERNAME_MAX_LEN = 50;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public ProfileService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public ProfilePublicResponseDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return userProfileRepository.findById(userId)
                .map(p -> ProfilePublicResponseDto.from(user, p))
                .orElseGet(() -> ProfilePublicResponseDto.from(user, null));
    }

    @Transactional(readOnly = true)
    public ProfilePublicResponseDto getProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "닉네임을 입력하세요.");
        }
        String trimmed = username.trim();
        User user = userRepository.findFirstByUsernameContainingIgnoreCaseOrderByIdAsc(trimmed)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "검색 조건에 맞는 사용자를 찾을 수 없습니다."));
        return getProfile(user.getId());
    }

    /**
     * 닉네임 부분 일치 사용자 목록(최대 50명, id 오름차순). 없으면 빈 리스트.
     */
    @Transactional(readOnly = true)
    public List<ProfileMatchDto> searchProfilesByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "닉네임을 입력하세요.");
        }
        String trimmed = username.trim();
        List<User> users = userRepository.findTop50ByUsernameContainingIgnoreCaseOrderByIdAsc(trimmed);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> ids = users.stream().map(User::getId).toList();
        Map<Long, UserProfile> profileByUserId = userProfileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        return users.stream()
                .map(u -> ProfileMatchDto.from(u, profileByUserId.get(u.getId())))
                .toList();
    }

    @Transactional
    public ProfilePublicResponseDto patchProfile(Long userId, ObjectNode body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GameApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        boolean patchUsername = body.has("username");
        boolean mentionsProfileField = body.has("bio") || body.has("profileImageUrl")
                || body.has("bannerImageUrl") || body.has("preferredGames");

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        if (!patchUsername && !mentionsProfileField) {
            return ProfilePublicResponseDto.from(user, profile);
        }

        if (patchUsername) {
            applyUsername(body, user);
            userRepository.save(user);
        }

        if (mentionsProfileField) {
            if (profile == null) {
                profile = createProfile(user);
            }
            applyStringField(body, "bio", profile::setBio, BIO_MAX_LEN);
            applyStringField(body, "profileImageUrl", profile::setProfileImageUrl, IMAGE_URL_MAX_LEN);
            applyStringField(body, "bannerImageUrl", profile::setBannerImageUrl, IMAGE_URL_MAX_LEN);
            applyStringField(body, "preferredGames", profile::setPreferredGames, PREFERRED_GAMES_MAX_LEN);
            userProfileRepository.save(profile);
        }

        profile = userProfileRepository.findById(userId).orElse(null);
        return ProfilePublicResponseDto.from(user, profile);
    }

    /** {@code username} 키가 있을 때만 반영. null·빈 문자열은 불가(DB NOT NULL). */
    private void applyUsername(ObjectNode body, User user) {
        JsonNode node = body.get("username");
        if (node.isNull()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "닉네임은 비울 수 없습니다.");
        }
        if (!node.isTextual()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "username은 문자열이어야 합니다.");
        }
        String raw = node.asText().trim();
        if (raw.isEmpty()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, "닉네임을 입력하세요.");
        }
        if (raw.length() > USERNAME_MAX_LEN) {
            throw new GameApiException(HttpStatus.BAD_REQUEST,
                    "닉네임은 " + USERNAME_MAX_LEN + "자를 넘을 수 없습니다.");
        }
        user.setUsername(raw);
    }

    private UserProfile createProfile(User user) {
        UserProfile p = new UserProfile();
        p.setUser(user);
        return userProfileRepository.save(p);
    }

    /**
     * JSON에 키가 있을 때만 반영. 값이 null이면 DB 필드는 null(이미지·소개 제거).
     * 문자열은 trim 후, 빈 문자열도 null로 저장(기본 이미지/빈 소개).
     */
    private void applyStringField(ObjectNode body, String jsonName, Consumer<String> entitySetter, int maxLen) {
        if (!body.has(jsonName)) {
            return;
        }
        JsonNode node = body.get(jsonName);
        if (node.isNull()) {
            entitySetter.accept(null);
            return;
        }
        if (!node.isTextual()) {
            throw new GameApiException(HttpStatus.BAD_REQUEST, jsonName + "는 문자열이어야 합니다.");
        }
        String raw = node.asText().trim();
        if (raw.isEmpty()) {
            entitySetter.accept(null);
            return;
        }
        if (raw.length() > maxLen) {
            throw new GameApiException(HttpStatus.BAD_REQUEST,
                    jsonName + "은(는) " + maxLen + "자를 넘을 수 없습니다.");
        }
        entitySetter.accept(raw);
    }
}
