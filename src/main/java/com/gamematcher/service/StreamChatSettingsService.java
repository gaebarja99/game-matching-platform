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

/** 獄쎻뫗??筌띲끇???: 筌?쑵????겸봺疫?獄쎻뫗?ｏ쭕?, BJ ?⑤벊?, 筌?쑵??疫뀀뜆?(??????揶쎛??, ?됰뗀?볡뵳???? 筌띲끇??? 筌왖??*/
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

    /** 獄쎻뫗???癒?뮉 筌띲끇???筌?筌?쑵???됰뗀??疫뀀뜆? ???온??揶쎛??*/
    private boolean canManage(Long streamId, Long userId) {
        return isStreamOwner(streamId, userId) || isManager(streamId, userId);
    }

    /** 筌?쑵????겸봺疫???쇱젟 (獄쎻뫗?ｏ쭕? */
    @Transactional
    public void setChatFrozen(Long streamId, Long userId, boolean frozen) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("獄쎻뫗?ｏ쭕???쇱젟??????됰뮸??덈뼄.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("獄쎻뫗???筌≪뼚??????곷뮸??덈뼄."));
        stream.setChatFrozen(frozen);
        liveStreamRepository.save(stream);
    }

    /** BJ ?⑤벊? ??쇱젟 (獄쎻뫗?ｏ쭕? 筌ㅼ뮆? 200?? */
    @Transactional
    public void setStreamNotice(Long streamId, Long userId, String text, Boolean visible) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("獄쎻뫗?ｏ쭕???쇱젟??????됰뮸??덈뼄.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("獄쎻뫗???筌≪뼚??????곷뮸??덈뼄."));
        if (text != null && text.length() > NOTICE_MAX_LENGTH) text = text.substring(0, NOTICE_MAX_LENGTH);
        stream.setStreamNotice(text != null && !text.isBlank() ? text.trim() : null);
        if (visible != null) stream.setStreamNoticeVisible(visible);
        liveStreamRepository.save(stream);
    }

    /** 筌?쑵??疫뀀뜆? ?곕떽? (獄쎻뫗??筌띲끇???). durationMinutes null?????怨대럡, 5筌?5????筌띾슢利? */
    @Transactional
    public void addChatBan(Long streamId, Long operatorUserId, Long targetUserId, Integer durationMinutes) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("諛⑹넚???먮뒗 留ㅻ땲?留?梨꾪똿 湲덉?瑜??ㅼ젙?????덉뒿?덈떎.");
        if (targetUserId == null) throw new IllegalArgumentException("????ъ슜?먮? 吏?뺥빐 二쇱꽭??");
        if (isStreamOwner(streamId, targetUserId)) throw new IllegalArgumentException("諛⑹넚?먮뒗 梨꾪똿 湲덉?瑜??ㅼ젙?????놁뒿?덈떎.");
        if (isManager(streamId, targetUserId)) throw new IllegalArgumentException("留ㅻ땲???梨꾪똿 湲덉?瑜??ㅼ젙?????놁뒿?덈떎.");
        StreamChatBan ban = streamChatBanRepository.findByStreamIdAndUserId(streamId, targetUserId)
                .orElseGet(() -> {
                    StreamChatBan created = new StreamChatBan();
                    created.setStreamId(streamId);
                    created.setUserId(targetUserId);
                    return created;
                });
        if (durationMinutes != null && durationMinutes > 0) {
            ban.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        } else {
            ban.setExpiresAt(null);
        }
        streamChatBanRepository.save(ban);
    }

    /** 嚥≪뮄????袁⑹뵠???얜챷???嚥?筌?쑵??疫뀀뜆? ?곕떽?. durationMinutes null=?怨대럡, 5=5???? */
    @Transactional
    public void addChatBanByLoginId(Long streamId, Long operatorUserId, String loginId, Integer durationMinutes) {
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("嚥≪뮄????袁⑹뵠?遺? ??낆젾??雅뚯눘苑??");
        Long targetUserId = userRepository.findByLoginId(loginId.trim())
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("????嚥≪뮄????袁⑹뵠?遺우벥 ????癒? 筌≪뼚??????곷뮸??덈뼄."));
        addChatBan(streamId, operatorUserId, targetUserId, durationMinutes);
    }

    /** 筌?쑵??疫뀀뜆? ??곸젫 (獄쎻뫗??筌띲끇???) */
    @Transactional
    public void removeChatBan(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("獄쎻뫗???癒?뮉 筌띲끇???筌???곸젫??????됰뮸??덈뼄.");
        streamChatBanRepository.deleteByStreamIdAndUserId(streamId, targetUserId);
    }

    /** ?怨멸맒 ?袁⑹뜚 / TTS 筌ㅼ뮇??????쇱젟 (獄쎻뫗?ｏ쭕?. null ?癒?뮉 0 ??곴맒. */
    @Transactional
    public void setDonationLimits(Long streamId, Long userId, Integer minVideoPang, Integer minTtsPang) {
        if (!isStreamOwner(streamId, userId)) throw new IllegalArgumentException("獄쎻뫗?ｏ쭕???쇱젟??????됰뮸??덈뼄.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("獄쎻뫗???筌≪뼚??????곷뮸??덈뼄."));
        if (minVideoPang != null && minVideoPang < 0) minVideoPang = 0;
        if (minTtsPang != null && minTtsPang < 0) minTtsPang = 0;
        stream.setMinVideoPang(minVideoPang != null ? minVideoPang : 0);
        stream.setMinTtsPang(minTtsPang != null ? minTtsPang : 0);
        liveStreamRepository.save(stream);
    }

    /** ????獄쎻뫗??癒?퐣 筌?쑵??疫뀀뜆? ??? (筌띾슢利??疫뀀뜆?????볤탢 ??false) */
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

    @Transactional
    public boolean hasActiveTimedBan(Long streamId, Long userId) {
        if (streamId == null || userId == null) return false;
        Optional<StreamChatBan> opt = streamChatBanRepository.findByStreamIdAndUserId(streamId, userId);
        if (opt.isEmpty()) return false;
        StreamChatBan ban = opt.get();
        if (ban.getExpiresAt() == null) {
            return false;
        }
        if (ban.getExpiresAt().isBefore(LocalDateTime.now())) {
            streamChatBanRepository.delete(ban);
            return false;
        }
        return true;
    }

    /** 疫뀀뜆? 筌뤴뫖以?(userId, displayName, loginId, expiresAt) - 獄쎻뫗??筌띲끇?????*/
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBannedList(Long streamId, Long operatorUserId) {
        if (!canManage(streamId, operatorUserId)) return List.of();
        return streamChatBanRepository.findByStreamIdOrderByIdDesc(streamId, PageRequest.of(0, 100)).stream()
                .filter(b -> b.getExpiresAt() == null || b.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(b -> {
                    var userOpt = userRepository.findById(b.getUserId());
                    String name = userOpt
                            .map(u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername())
                            .orElse("??????곸벉");
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

    // ----- ?됰뗀?볡뵳????(獄쎻뫗????놁삢 筌△뫀?? -----

    @Transactional
    public void addBlacklist(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("獄쎻뫗???癒?뮉 筌띲끇???筌??됰뗀?볡뵳?????곕떽???????됰뮸??덈뼄.");
        if (targetUserId == null) throw new IllegalArgumentException("????????癒? 筌왖?類λ퉸 雅뚯눘苑??");
        if (isStreamOwner(streamId, targetUserId)) throw new IllegalArgumentException("獄쎻뫗??癒?뮉 ?됰뗀?볡뵳???紐꾨퓠 ?節뚯뱽 ????곷뮸??덈뼄.");
        if (streamBlacklistRepository.existsByStreamIdAndUserId(streamId, targetUserId)) return;
        StreamBlacklist b = new StreamBlacklist();
        b.setStreamId(streamId);
        b.setUserId(targetUserId);
        streamBlacklistRepository.save(b);
    }

    @Transactional
    public void removeBlacklist(Long streamId, Long operatorUserId, Long targetUserId) {
        if (!canManage(streamId, operatorUserId)) throw new IllegalArgumentException("獄쎻뫗???癒?뮉 筌띲끇???筌??됰뗀?볡뵳???紐꾨퓠????볤탢??????됰뮸??덈뼄.");
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
                            .orElse("??????곸벉");
                    String loginId = userOpt.map(u -> u.getLoginId()).orElse(null);
                    return Map.<String, Object>of("userId", b.getUserId(), "displayName", name, "loginId", loginId != null && !loginId.isBlank() ? loginId : "");
                })
                .collect(Collectors.toList());
    }

    // ----- 筌띲끇??? (獄쎻뫗?ｏ쭕?筌왖????곸젫) -----

    @Transactional
    public void addManager(Long streamId, Long ownerUserId, Long targetUserId) {
        if (!isStreamOwner(streamId, ownerUserId)) throw new IllegalArgumentException("獄쎻뫗?ｏ쭕?筌띲끇?????筌왖?類λ막 ????됰뮸??덈뼄.");
        if (targetUserId == null) throw new IllegalArgumentException("????????癒? 筌왖?類λ퉸 雅뚯눘苑??");
        if (ownerUserId.equals(targetUserId)) throw new IllegalArgumentException("癰귣챷??? 筌띲끇???嚥?筌왖?類λ막 ????곷뮸??덈뼄.");
        if (streamManagerRepository.existsByStreamIdAndUserId(streamId, targetUserId)) return;
        StreamManager m = new StreamManager();
        m.setStreamId(streamId);
        m.setUserId(targetUserId);
        streamManagerRepository.save(m);
    }

    @Transactional
    public void removeManager(Long streamId, Long ownerUserId, Long targetUserId) {
        if (!isStreamOwner(streamId, ownerUserId)) throw new IllegalArgumentException("獄쎻뫗?ｏ쭕?筌띲끇???????곸젫??????됰뮸??덈뼄.");
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
                            .orElse("??????곸벉");
                    String loginId = userOpt.map(u -> u.getLoginId()).orElse(null);
                    return Map.<String, Object>of("userId", m.getUserId(), "displayName", name, "loginId", loginId != null && !loginId.isBlank() ? loginId : "");
                })
                .collect(Collectors.toList());
    }
}
