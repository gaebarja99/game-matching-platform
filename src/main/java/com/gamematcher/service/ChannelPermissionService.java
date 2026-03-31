package com.gamematcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.entity.ChannelPermission;
import com.gamematcher.entity.ChannelProfileSettings;
import com.gamematcher.entity.User;
import com.gamematcher.repository.ChannelPermissionRepository;
import com.gamematcher.repository.ChannelProfileSettingsRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelPermissionService {

    private final ChannelPermissionRepository channelPermissionRepository;
    private final ChannelProfileSettingsRepository channelProfileSettingsRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getContext(Long sessionUserId) {
        return getContext(sessionUserId, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContext(Long sessionUserId, Long requestedOwnerUserId) {
        User me = requireUser(sessionUserId);
        Long ownerUserId = resolveOwnerUserId(sessionUserId, requestedOwnerUserId);
        User owner = requireUser(ownerUserId);
        boolean actingAsManager = !ownerUserId.equals(sessionUserId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ownerUserId", ownerUserId);
        result.put("ownerNickname", displayName(owner));
        result.put("ownerLoginId", owner.getLoginId());
        result.put("actingAsManager", actingAsManager);
        result.put("canManagePermissions", !actingAsManager);
        result.put("myUserId", me.getId());
        result.put("managedChannels", getManagedChannels(sessionUserId));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getManagedChannels(Long sessionUserId) {
        return channelPermissionRepository.findByManagerUserIdOrderByCreatedAtDesc(sessionUserId)
                .stream()
                .map(permission -> userRepository.findById(permission.getOwnerUserId())
                        .map(owner -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("ownerUserId", owner.getId());
                            row.put("ownerNickname", displayName(owner));
                            row.put("ownerLoginId", owner.getLoginId());
                            row.put("roleName", permission.getRoleName() != null ? permission.getRoleName() : "");
                            row.put("grantedAt", permission.getCreatedAt() != null ? permission.getCreatedAt().toString() : "");
                            return row;
                        })
                        .orElse(null))
                .filter(row -> row != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPermissionList(Long ownerUserId) {
        User owner = requireUser(ownerUserId);

        Map<String, Object> ownerRow = new LinkedHashMap<>();
        ownerRow.put("userId", owner.getId());
        ownerRow.put("nickname", displayName(owner));
        ownerRow.put("loginId", owner.getLoginId());
        ownerRow.put("role", "소유자");
        ownerRow.put("registeredBy", displayName(owner));
        ownerRow.put("registeredAt", owner.getCreatedAt() != null ? owner.getCreatedAt().toLocalDate().toString() : "");
        ownerRow.put("removable", false);

        List<Map<String, Object>> rows = channelPermissionRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)
                .stream()
                .map(permission -> userRepository.findById(permission.getManagerUserId())
                        .map(user -> {
                            User actor = userRepository.findById(permission.getCreatedByUserId()).orElse(owner);
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("userId", user.getId());
                            row.put("nickname", displayName(user));
                            row.put("loginId", user.getLoginId());
                            row.put("role", permission.getRoleName());
                            row.put("registeredBy", displayName(actor));
                            row.put("registeredAt", permission.getCreatedAt() != null ? permission.getCreatedAt().toLocalDate().toString() : "");
                            row.put("removable", true);
                            return row;
                        })
                        .orElse(null))
                .filter(row -> row != null)
                .collect(Collectors.toList());

        rows.add(0, ownerRow);
        return rows;
    }

    @Transactional
    public void addPermission(Long ownerUserId, String keyword, String roleName) {
        User owner = requireUser(ownerUserId);
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("닉네임 또는 아이디를 입력해 주세요.");
        }

        User target = userRepository.findByLoginId(normalized)
                .or(() -> userRepository.findByNickname(normalized))
                .orElseThrow(() -> new IllegalArgumentException("사이트에 실제로 존재하는 닉네임 또는 아이디만 추가할 수 있습니다."));

        if (ownerUserId.equals(target.getId())) {
            throw new IllegalArgumentException("본인에게는 권한을 줄 수 없습니다.");
        }
        if (channelPermissionRepository.existsByOwnerUserIdAndManagerUserId(ownerUserId, target.getId())) {
            throw new IllegalArgumentException("이미 권한을 부여한 사용자입니다.");
        }

        ChannelPermission permission = new ChannelPermission();
        permission.setOwnerUserId(ownerUserId);
        permission.setManagerUserId(target.getId());
        permission.setRoleName((roleName == null || roleName.isBlank()) ? "채널 관리자" : roleName.trim());
        permission.setCreatedByUserId(ownerUserId);
        channelPermissionRepository.save(permission);

        notificationService.createForChannelPermissionGranted(target.getId(), ownerUserId, displayName(owner));
    }

    @Transactional
    public void removePermission(Long ownerUserId, Long managerUserId) {
        User owner = requireUser(ownerUserId);
        ChannelPermission permission = channelPermissionRepository.findByOwnerUserIdAndManagerUserId(ownerUserId, managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("해당 권한 정보를 찾을 수 없습니다."));
        channelPermissionRepository.delete(permission);
        notificationService.createForChannelPermissionRevoked(managerUserId, ownerUserId, displayName(owner));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChannelManage(Long sessionUserId) {
        return getChannelManage(sessionUserId, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChannelManage(Long sessionUserId, Long requestedOwnerUserId) {
        Map<String, Object> context = getContext(sessionUserId, requestedOwnerUserId);
        Long ownerUserId = ((Number) context.get("ownerUserId")).longValue();
        User owner = requireUser(ownerUserId);
        ChannelProfileSettings settings = channelProfileSettingsRepository.findById(ownerUserId).orElseGet(() -> {
            ChannelProfileSettings created = new ChannelProfileSettings();
            created.setOwnerUserId(ownerUserId);
            return created;
        });

        Map<String, Object> result = new LinkedHashMap<>(context);
        result.put("nickname", owner.getNickname() != null ? owner.getNickname() : owner.getUsername());
        result.put("bio", owner.getBio() != null ? owner.getBio() : "");
        result.put("profileImageUrl", owner.getProfileImageUrl() != null ? owner.getProfileImageUrl() : "");
        result.put("socialLinks", parseSocialLinks(settings.getSocialLinksJson()));
        result.put("cafeEnabled", Boolean.TRUE.equals(settings.getCafeEnabled()));
        result.put("sponsorRankingVisible", Boolean.TRUE.equals(settings.getSponsorRankingVisible()));
        result.put("missionVisible", Boolean.TRUE.equals(settings.getMissionVisible()));
        return result;
    }

    @Transactional
    public Map<String, Object> saveChannelManage(Long sessionUserId, Map<String, Object> body) {
        Long requestedOwnerUserId = parseOwnerUserId(body == null ? null : body.get("ownerUserId"));
        Map<String, Object> context = getContext(sessionUserId, requestedOwnerUserId);
        Long ownerUserId = ((Number) context.get("ownerUserId")).longValue();
        User owner = requireUser(ownerUserId);
        ChannelProfileSettings settings = channelProfileSettingsRepository.findById(ownerUserId).orElseGet(() -> {
            ChannelProfileSettings created = new ChannelProfileSettings();
            created.setOwnerUserId(ownerUserId);
            return created;
        });

        String nickname = asTrimmedString(body.get("nickname"));
        if (!nickname.isBlank()) {
            userRepository.findByNickname(nickname)
                    .filter(other -> !other.getId().equals(ownerUserId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    });
            owner.setNickname(nickname);
        }

        String bio = asTrimmedString(body.get("bio"));
        owner.setBio(bio.length() > 500 ? bio.substring(0, 500) : bio);
        userRepository.save(owner);

        settings.setSocialLinksJson(writeSocialLinks(body.get("socialLinks")));
        settings.setCafeEnabled(asBoolean(body.get("cafeEnabled")));
        settings.setSponsorRankingVisible(asBoolean(body.get("sponsorRankingVisible")));
        settings.setMissionVisible(asBoolean(body.get("missionVisible")));
        channelProfileSettingsRepository.save(settings);

        return getChannelManage(sessionUserId, ownerUserId);
    }

    private Long resolveOwnerUserId(Long sessionUserId, Long requestedOwnerUserId) {
        if (requestedOwnerUserId == null || requestedOwnerUserId.equals(sessionUserId)) {
            return sessionUserId;
        }
        boolean hasPermission = channelPermissionRepository.existsByOwnerUserIdAndManagerUserId(requestedOwnerUserId, sessionUserId);
        if (!hasPermission) {
            throw new IllegalArgumentException("해당 채널을 관리할 권한이 없습니다.");
        }
        return requestedOwnerUserId;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String displayName(User user) {
        return (user.getNickname() != null && !user.getNickname().isBlank()) ? user.getNickname() : user.getUsername();
    }

    private List<String> parseSocialLinks(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return parsed == null ? List.of() : parsed.stream()
                    .filter(link -> link != null && !link.isBlank())
                    .limit(5)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeSocialLinks(Object value) {
        List<String> links = (value instanceof List<?> list ? list : List.of()).stream()
                .filter(item -> item != null)
                .map(Object::toString)
                .map(String::trim)
                .filter(link -> !link.isBlank())
                .limit(5)
                .toList();
        try {
            return objectMapper.writeValueAsString(links);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String asTrimmedString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private Long parseOwnerUserId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = value.toString().trim();
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
