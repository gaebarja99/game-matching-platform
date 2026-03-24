package com.gamematcher.service;

import com.gamematcher.entity.LiveStream;
import com.gamematcher.entity.StreamBlacklist;
import com.gamematcher.entity.StreamChatBan;
import com.gamematcher.entity.StreamManager;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.StreamBlacklistRepository;
import com.gamematcher.repository.StreamChatBanRepository;
import com.gamematcher.repository.StreamManagerRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** 방장/매니저: 채팅 얼리기(방장만), BJ 공지, 채팅 금지(타이머 가능), 블랙리스트, 매니저 지정 */
@Service
@RequiredArgsConstructor
public class StreamChatSettingsService {

    private static final int NOTICE_MAX_LENGTH = 200;

    private final LiveStreamRepository liveStreamRepository;
    private final StreamChatBanRepository streamChatBanRepository;
    private final StreamBlacklistRepository streamBlacklistRepository;
    private final StreamManagerRepository streamManagerRepository;
    private final UserRepository userRepository;

    public boolean isStreamOwner(Long streamId, Long userId) {
        if (streamId == null || userId == null) return false;
        return liveStreamRepository.findById(streamId)
                .map(s -> userId.equals(s.getUserId()))
                .orElse(false);
    }

    public boolean isManager(Long streamId, Long userId) {
        if (streamId == null || userId == null) return false;
        return streamManagerRepository.existsByStreamIdAndUserId(streamId, userId);
    }

    /** 방장 또는 매니저면 채팅/블랙/금지 등 관리 가능 */
    private boolean canManage(Long streamId, Long userId) {
        return isStreamOwner(streamId, userId) || isManager(streamId, userId);
    }

    /** 채팅 얼리기 설정 (방장만) */
    @Transactional
    public void setChatFrozen(Long streamId, Long userId, boolean frozen) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("방장만 설정할 수 있습니다.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        stream.setChatFrozen(frozen);
        liveStreamRepository.save(stream);
    }

    /** BJ 공지 설정 (방장만, 최대 200자) */
    @Transactional
    public void setStreamNotice(Long streamId, Long userId, String text, Boolean visible) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("방장만 설정할 수 있습니다.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        if (text != null && text.length() > NOTICE_MAX_LENGTH) text = text.substring(0, NOTICE_MAX_LENGTH);
        stream.setStreamNotice(text != null && !text.isBlank() ? text.trim() : null);
        if (visible != null) stream.setStreamNoticeVisible(visible);
        liveStreamRepository.save(stream);
    }

    /** 채팅 금지 추가 (방장/매니저). durationMinutes null이면 영구, 5면 5분 후 만료. */
    @Transactional
    public void addChatBan(Long streamId, Long operatorUserId, Long targetUserId, Integer durationMinutes) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("방장 또는 매니저만 채팅 금지할 수 있습니다.");
        if (targetUserId == null) throw new IllegalArgumentException("대상 사용자를 지정해 주세요.");
        if (isStreamOwner(streamId, targetUserId)) throw new IllegalArgumentException("방송자는 채팅 금지할 수 없습니다.");
        if (isManager(streamId, targetUserId)) throw new IllegalArgumentException("매니저는 채팅 금지할 수 없습니다.");
        streamChatBanRepository.findByStreamIdAndUserId(streamId, targetUserId).ifPresent(streamChatBanRepository::delete);
        StreamChatBan ban = new StreamChatBan();
        ban.setStreamId(streamId);
        ban.setUserId(targetUserId);
        if (durationMinutes != null && durationMinutes > 0) {
            ban.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        }
        streamChatBanRepository.save(ban);
    }

    /** 로그인 아이디(문자열)로 채팅 금지 추가. durationMinutes null=영구, 5=5분 등. */
    @Transactional
    public void addChatBanByLoginId(Long streamId, Long operatorUserId, String loginId, Integer durationMinutes) {
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("로그인 아이디를 입력해 주세요.");
        Long targetUserId = userRepository.findByLoginId(loginId.trim())
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 로그인 아이디의 사용자를 찾을 수 없습니다."));
        addChatBan(streamId, operatorUserId, targetUserId, durationMinutes);
    }

    /** 채팅 금지 해제 (방장/매니저) */
    @Transactional
    public void removeChatBan(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("방장 또는 매니저만 해제할 수 있습니다.");
        streamChatBanRepository.deleteByStreamIdAndUserId(streamId, targetUserId);
    }

    /** 영상 후원 / TTS 최소 팡 설정 (방장만). null 또는 0 이상. */
    @Transactional
    public void setDonationLimits(Long streamId, Long userId, Integer minVideoPang, Integer minTtsPang) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("방장만 설정할 수 있습니다.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        if (minVideoPang != null && minVideoPang < 0) minVideoPang = 0;
        if (minTtsPang != null && minTtsPang < 0) minTtsPang = 0;
        stream.setMinVideoPang(minVideoPang != null ? minVideoPang : 0);
        stream.setMinTtsPang(minTtsPang != null ? minTtsPang : 0);
        liveStreamRepository.save(stream);
    }

    /** 해당 방송에서 채팅 금지 여부 (만료된 금지는 제거 후 false) */
    @Transactional
    public boolean isBanned(Long streamId, Long userId) {
        if (streamId == null || userId == null) return false;
        Optional<StreamChatBan> opt = streamChatBanRepository.findByStreamIdAndUserId(streamId, userId);
        if (opt.isEmpty()) return false;
        StreamChatBan b = opt.get();
        if (b.getExpiresAt() != null && b.getExpiresAt().isBefore(LocalDateTime.now())) {
            streamChatBanRepository.delete(b);
            return false;
        }
        return true;
    }

    /** 금지 목록 (userId, displayName, loginId, expiresAt) - 방장/매니저용 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBannedList(Long streamId, Long operatorUserId) {
        if (!canManage(streamId, operatorUserId)) return List.of();
        return streamChatBanRepository.findByStreamIdOrderByIdDesc(streamId, PageRequest.of(0, 100)).stream()
                .filter(b -> b.getExpiresAt() == null || b.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(b -> {
                    var userOpt = userRepository.findById(b.getUserId());
                    String name = userOpt
                            .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                            .orElse("알 수 없음");
                    String loginId = userOpt.map(u -> u.getLoginId()).orElse(null);
                    return Map.<String, Object>of(
                            "userId", b.getUserId(),
                            "displayName", name,
                            "loginId", loginId != null && !loginId.isBlank() ? loginId : "",
                            "expiresAt", b.getExpiresAt() != null ? b.getExpiresAt().toString() : ""
                    );
                })
                .collect(Collectors.toList());
    }

    // ----- 블랙리스트 (방송 입장 차단) -----

    @Transactional
    public void addBlacklist(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("방장 또는 매니저만 블랙리스트 추가할 수 있습니다.");
        if (targetUserId == null) throw new IllegalArgumentException("대상 사용자를 지정해 주세요.");
        if (isStreamOwner(streamId, targetUserId)) throw new IllegalArgumentException("방송자는 블랙리스트에 넣을 수 없습니다.");
        if (streamBlacklistRepository.existsByStreamIdAndUserId(streamId, targetUserId)) return;
        StreamBlacklist b = new StreamBlacklist();
        b.setStreamId(streamId);
        b.setUserId(targetUserId);
        streamBlacklistRepository.save(b);
    }

    @Transactional
    public void removeBlacklist(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("방장 또는 매니저만 블랙리스트에서 제거할 수 있습니다.");
        streamBlacklistRepository.deleteByStreamIdAndUserId(streamId, targetUserId);
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(Long streamId, Long userId) {
        if (streamId == null || userId == null) return false;
        return streamBlacklistRepository.existsByStreamIdAndUserId(streamId, userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBlacklist(Long streamId, Long operatorUserId) {
        if (!canManage(streamId, operatorUserId)) return List.of();
        return streamBlacklistRepository.findByStreamIdOrderByIdDesc(streamId, PageRequest.of(0, 100)).stream()
                .map(b -> {
                    var userOpt = userRepository.findById(b.getUserId());
                    String name = userOpt
                            .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                            .orElse("알 수 없음");
                    String loginId = userOpt.map(u -> u.getLoginId()).orElse(null);
                    return Map.<String, Object>of("userId", b.getUserId(), "displayName", name, "loginId", loginId != null && !loginId.isBlank() ? loginId : "");
                })
                .collect(Collectors.toList());
    }

    // ----- 매니저 (방장만 지정/해제) -----

    @Transactional
    public void addManager(Long streamId, Long ownerUserId, Long targetUserId) {
        if (!isStreamOwner(streamId, ownerUserId)) throw new IllegalArgumentException("방장만 매니저를 지정할 수 있습니다.");
        if (targetUserId == null) throw new IllegalArgumentException("대상 사용자를 지정해 주세요.");
        if (ownerUserId.equals(targetUserId)) throw new IllegalArgumentException("본인은 매니저로 지정할 수 없습니다.");
        if (streamManagerRepository.existsByStreamIdAndUserId(streamId, targetUserId)) return;
        StreamManager m = new StreamManager();
        m.setStreamId(streamId);
        m.setUserId(targetUserId);
        streamManagerRepository.save(m);
    }

    @Transactional
    public void removeManager(Long streamId, Long ownerUserId, Long targetUserId) {
        if (!isStreamOwner(streamId, ownerUserId)) throw new IllegalArgumentException("방장만 매니저를 해제할 수 있습니다.");
        streamManagerRepository.deleteByStreamIdAndUserId(streamId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getManagers(Long streamId, Long ownerUserId) {
        if (!isStreamOwner(streamId, ownerUserId)) return List.of();
        return streamManagerRepository.findByStreamIdOrderByIdDesc(streamId, PageRequest.of(0, 50)).stream()
                .map(m -> {
                    var userOpt = userRepository.findById(m.getUserId());
                    String name = userOpt
                            .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                            .orElse("알 수 없음");
                    String loginId = userOpt.map(u -> u.getLoginId()).orElse(null);
                    return Map.<String, Object>of("userId", m.getUserId(), "displayName", name, "loginId", loginId != null && !loginId.isBlank() ? loginId : "");
                })
                .collect(Collectors.toList());
    }
}
