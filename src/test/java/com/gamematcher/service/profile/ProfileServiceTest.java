package com.gamematcher.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.constant.profile.ProfileImageConstants;
import com.gamematcher.dto.profile.ProfileMatchDto;
import com.gamematcher.dto.profile.ProfilePublicResponseDto;
import com.gamematcher.entity.User;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.repository.common.CommonUserRepository;
import com.gamematcher.repository.profile.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceTest {

    @Autowired
    ProfileService profileService;
    @Autowired
    CommonUserRepository userRepository;
    @Autowired
    UserProfileRepository userProfileRepository;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("프로필 없는 사용자 — 기본 프로필 이미지 URL, bio·배너는 null")
    void getProfileWithoutRow() {
        User user = saveUser();

        ProfilePublicResponseDto dto = profileService.getProfile(user.getId());

        assertThat(dto.getUserId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo("닉네임");
        assertThat(dto.getBio()).isNull();
        assertThat(dto.getProfileImageUrl()).isEqualTo(ProfileImageConstants.DEFAULT_PROFILE_IMAGE_URL);
        assertThat(dto.getBannerImageUrl()).isNull();
    }

    @Test
    @DisplayName("PATCH로 프로필 생성 및 null로 이미지 제거")
    void patchCreatesAndClearsImage() {
        User user = saveUser();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("bio", "안녕하세요");
        body.put("profileImageUrl", "https://example.com/a.png");

        ProfilePublicResponseDto updated = profileService.patchProfile(user.getId(), body);
        assertThat(updated.getBio()).isEqualTo("안녕하세요");
        assertThat(updated.getProfileImageUrl()).isEqualTo("https://example.com/a.png");
        assertThat(userProfileRepository.findById(user.getId())).isPresent();

        ObjectNode clear = objectMapper.createObjectNode();
        clear.putNull("profileImageUrl");
        ProfilePublicResponseDto cleared = profileService.patchProfile(user.getId(), clear);
        assertThat(cleared.getProfileImageUrl()).isEqualTo(ProfileImageConstants.DEFAULT_PROFILE_IMAGE_URL);
        assertThat(cleared.getBio()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("PATCH로 닉네임 변경")
    void patchUsername() {
        User user = saveUser();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", "새닉네임");

        ProfilePublicResponseDto updated = profileService.patchProfile(user.getId(), body);
        assertThat(updated.getUsername()).isEqualTo("새닉네임");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getUsername()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("닉네임 빈 문자열 PATCH — BAD_REQUEST")
    void patchUsername_blank() {
        User user = saveUser();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", "   ");
        assertThatThrownBy(() -> profileService.patchProfile(user.getId(), body))
                .isInstanceOf(GameApiException.class)
                .satisfies(ex -> assertThat(((GameApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 — NOT_FOUND")
    void getUnknownUser() {
        assertThatThrownBy(() -> profileService.getProfile(999_999L))
                .isInstanceOf(GameApiException.class)
                .satisfies(ex -> assertThat(((GameApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("닉네임으로 조회 — userId 조회와 동일한 결과")
    void getProfileByUsername_matchesGetById() {
        User user = saveUser();
        ProfilePublicResponseDto byId = profileService.getProfile(user.getId(), null);
        ProfilePublicResponseDto byName = profileService.getProfileByUsername("닉네임", null);
        assertThat(byName.getUserId()).isEqualTo(byId.getUserId());
        assertThat(byName.getUsername()).isEqualTo(byId.getUsername());
    }

    @Test
    @DisplayName("닉네임 부분 일치로 조회")
    void getProfileByUsername_partialMatch() {
        User user = saveUserWithUsername("데모유저긴이름");
        ProfilePublicResponseDto dto = profileService.getProfileByUsername("유저긴", null);
        assertThat(dto.getUserId()).isEqualTo(user.getId());
        assertThat(dto.getUsername()).isEqualTo("데모유저긴이름");
    }

    @Test
    @DisplayName("닉네임 검색 목록 — 부분 일치 전원, id 오름차순")
    void searchProfilesByUsername_returnsOrderedMatches() {
        saveUserWithUsername("alpha-one");
        saveUserWithUsername("alpha-two");
        saveUserWithUsername("beta");
        List<ProfileMatchDto> list = profileService.searchProfilesByUsername("alpha");
        assertThat(list).hasSize(2);
        assertThat(list.stream().map(ProfileMatchDto::getUsername))
                .containsExactly("alpha-one", "alpha-two");
    }

    @Test
    @DisplayName("닉네임 검색 목록 — 없으면 빈 리스트")
    void searchProfilesByUsername_emptyWhenNoMatch() {
        saveUserWithUsername("lonely");
        List<ProfileMatchDto> list = profileService.searchProfilesByUsername("zzz");
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("동일 닉네임이 여러 명이면 id가 가장 작은 사용자")
    void getProfileByUsername_duplicateNickname_smallestId() {
        User first = saveUserWithUsername("중복닉");
        saveUserWithUsername("중복닉");
        ProfilePublicResponseDto dto = profileService.getProfileByUsername("중복닉", null);
        assertThat(dto.getUserId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("닉네임 공백만 — BAD_REQUEST")
    void getProfileByUsername_blank() {
        assertThatThrownBy(() -> profileService.getProfileByUsername("   ", null))
                .isInstanceOf(GameApiException.class)
                .satisfies(ex -> assertThat(((GameApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("없는 닉네임 — NOT_FOUND")
    void getProfileByUsername_unknown() {
        assertThatThrownBy(() -> profileService.getProfileByUsername("ghost_" + System.nanoTime(), null))
                .isInstanceOf(GameApiException.class)
                .satisfies(ex -> assertThat(((GameApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("공개 비공개 — 타인 조회는 마스킹, 본인 조회는 원문·visibility")
    void visibilityMasking_publicVsOwner() {
        User user = saveUser();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("bio", "내부소개");
        body.put("bannerImageUrl", "https://example.com/banner.png");
        body.put("bioVisible", false);
        body.put("bannerImageVisible", false);
        profileService.patchProfile(user.getId(), body);

        ProfilePublicResponseDto pub = profileService.getProfile(user.getId(), null);
        assertThat(pub.getBio()).isNull();
        assertThat(pub.getBannerImageUrl()).isNull();
        assertThat(pub.getVisibility()).isNull();

        ProfilePublicResponseDto own = profileService.getProfile(user.getId(), user.getId());
        assertThat(own.getBio()).isEqualTo("내부소개");
        assertThat(own.getBannerImageUrl()).isEqualTo("https://example.com/banner.png");
        assertThat(own.getVisibility()).isNotNull();
        assertThat(own.getVisibility().isBio()).isFalse();
        assertThat(own.getVisibility().isBannerImage()).isFalse();
    }

    @Test
    @DisplayName("PATCH로 공개 설정만 변경")
    void patchVisibilityOnly() {
        User user = saveUser();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("preferredGamesVisible", false);
        ProfilePublicResponseDto dto = profileService.patchProfile(user.getId(), body);
        assertThat(dto.getVisibility().isPreferredGames()).isFalse();
    }

    private User saveUser() {
        User user = new User();
        user.setLoginId("login-" + System.nanoTime());
        user.setUsername("닉네임");
        user.setEmail("e" + System.nanoTime() + "@test.com");
        user.setPassword("x");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private User saveUserWithUsername(String username) {
        User user = new User();
        user.setLoginId("login-" + System.nanoTime());
        user.setUsername(username);
        user.setEmail("e" + System.nanoTime() + "@test.com");
        user.setPassword("x");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
