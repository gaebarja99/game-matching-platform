package com.gamematcher.controller;

import com.gamematcher.config.UploadProperties;
import com.gamematcher.dto.auth.AuthResponse;
import com.gamematcher.entity.User;
import com.gamematcher.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final String SESSION_USER_ID = "userId";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final String PROFILE_SUBDIR = "profile";

    private final UserRepository userRepository;
    private final UploadProperties uploadProperties;

    /**
     * 프로필 수정 (이름, 닉네임, 이메일, 전화번호, 자기소개, 프로필 사진).
     * multipart/form-data: username, nickname, email, phone (선택), bio (선택), profileImage (선택).
     * 전화번호/이름 변경은 내 정보에서 휴대전화 인증 후 가능.
     */
    @PutMapping
    public ResponseEntity<?> update(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile profileImage,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (username != null) {
            String trimmed = username.trim();
            if (trimmed.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "이름을 입력해 주세요."));
            }
            user.setUsername(trimmed);
        }
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (!trimmed.isEmpty() && userRepository.existsByNicknameAndIdNot(trimmed, userId)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "이미 사용 중인 닉네임입니다."));
            }
            user.setNickname(trimmed.isEmpty() ? null : trimmed);
        }
        if (email != null) {
            String trimmed = email.trim();
            if (trimmed.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "이메일을 입력해 주세요."));
            }
            if (!trimmed.equals(user.getEmail()) && userRepository.existsByEmail(trimmed)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "이미 사용 중인 이메일입니다."));
            }
            user.setEmail(trimmed);
        }
        if (phone != null) {
            user.setPhone(phone.trim().isEmpty() ? null : phone.trim());
        }
        if (bio != null) {
            String trimmed = bio.trim();
            user.setBio(trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed);
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "이미지 파일만 업로드 가능합니다."));
            }
            String ext = getExtension(profileImage.getOriginalFilename());
            if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "jpg, png, gif, webp만 가능합니다."));
            }
            try {
                String relativePath = saveProfileImage(userId, profileImage, ext);
                user.setProfileImageUrl("/uploads/" + relativePath);
            } catch (IOException e) {
                return ResponseEntity.status(500).body(java.util.Map.of("message", "파일 저장에 실패했습니다."));
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(AuthResponse.from(user));
    }

    private static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1) : null;
    }

    private String saveProfileImage(Long userId, MultipartFile file, String ext) throws IOException {
        String basePath = uploadProperties.getPath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "./uploads";
        }
        Path dir = Path.of(basePath).resolve(PROFILE_SUBDIR).toAbsolutePath();
        Files.createDirectories(dir);
        String fileName = userId + "_" + System.currentTimeMillis() + "." + ext;
        Path target = dir.resolve(fileName);
        file.transferTo(target.toFile());
        return PROFILE_SUBDIR + "/" + fileName;
    }
}
