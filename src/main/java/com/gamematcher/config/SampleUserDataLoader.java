package com.gamematcher.config;

import com.gamematcher.constant.Role;
import com.gamematcher.constant.UserStatus;
import com.gamematcher.entity.User;
import com.gamematcher.entity.profile.UserProfile;
import com.gamematcher.repository.common.UserRepository;
import com.gamematcher.repository.profile.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발용 데모 계정 생성.
 * <p>{@code gamematcher.seed-sample-users=true} 일 때만 실행되며,
 * 동일 {@code loginId}가 이미 있으면 건너뜁니다.</p>
 *
 * <p>예: {@code application.properties} 에 {@code gamematcher.seed-sample-users=true}
 * 또는 {@code mvn spring-boot:run -Dspring-boot.run.arguments=--gamematcher.seed-sample-users=true}</p>
 */
@Component
@Order(5)
@ConditionalOnProperty(name = "gamematcher.seed-sample-users", havingValue = "true")
public class SampleUserDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleUserDataLoader.class);

    /** 로컬 전용 placeholder 비밀번호 (인코딩 미적용) */
    private static final String DEMO_PASSWORD = "demo1234";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public SampleUserDataLoader(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        created += ensureUser(
                "demo1",
                "데모유저1",
                "demo1@localhost.local",
                Role.USER,
                "샘플 일반 유저입니다. 프로필·커뮤니티 테스트에 사용하세요.",
                "리그 오브 레전드, 발로란트"
        );
        created += ensureUser(
                "demo2",
                "데모유저2",
                "demo2@localhost.local",
                Role.USER,
                "본 시스템의 UI 및 기능 테스트를 위한 목적의 더미 데이터입니다. 해당 섹션에서는 텍스트 래핑(Wrapping)과 줄 바꿈(Line Break)이 "
                        + "정상적으로 이루어지는지 확인하며, 웹 브라우저의 뷰포트 크기 변화에 따른 반응형 레이아웃의 안정성을 검토합니다. "
                        + "데이터의 길이는 충분히 확보되어야 하며, 영문(English), 숫자(12345), 특수문자(!@#$) 등이 혼용되었을 때 "
                        + "폰트 렌더링에 깨짐 현상이 없는지 모니터링하기 위한 용도로 사용됩니다.",
                null
        );
        created += ensureUser(
                "admin1",
                "샘플관리자",
                "admin1@localhost.local",
                Role.ADMIN,
                "관리자 샘플 계정입니다.",
                null
        );
        if (created > 0) {
            log.info("[SampleUserDataLoader] 데모 유저 {}명 생성 (비밀번호 공통: {})", created, DEMO_PASSWORD);
        } else {
            log.info("[SampleUserDataLoader] 데모 loginId가 이미 존재하여 스킵");
        }
    }

    private int ensureUser(
            String loginId,
            String username,
            String email,
            Role role,
            String bio,
            String preferredGames
    ) {
        if (userRepository.findByLoginId(loginId).isPresent()) {
            return 0;
        }
        User user = new User();
        user.setLoginId(loginId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(DEMO_PASSWORD);
        user.setPhone(null);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setBio(bio);
        profile.setPreferredGames(preferredGames);
        userProfileRepository.save(profile);
        return 1;
    }
}
