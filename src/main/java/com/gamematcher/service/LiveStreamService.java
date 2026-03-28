package com.gamematcher.service;

import com.gamematcher.config.StreamingProperties;
import com.gamematcher.constant.StreamStatus;
import com.gamematcher.constant.StreamerTier;
import com.gamematcher.dto.stream.CreateStreamRequest;
import com.gamematcher.dto.stream.ObsSetupResponse;
import com.gamematcher.dto.stream.StreamResponse;
import com.gamematcher.dto.stream.UpdateStreamRequest;
import com.gamematcher.entity.LiveStream;
import com.gamematcher.repository.ChannelPermissionRepository;
import com.gamematcher.repository.FollowRepository;
import com.gamematcher.repository.LiveStreamRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ChannelPermissionRepository channelPermissionRepository;
    private final StreamingProperties streamingProperties;
    private final StreamViewerCountService streamViewerCountService;
    private final NotificationService notificationService;

    private String getBroadcasterDisplayName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse(null);
    }

    private String getBroadcasterProfileImageUrl(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getProfileImageUrl())
                .orElse(null);
    }

    private StreamerTier getBroadcasterStreamerTier(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getStreamerTier())
                .orElse(null);
    }

    /**
     * 계정당 하나의 스트림(채널) 유지. 이미 있으면 제목·카테고리만 갱신하고 같은 스트림 반환.
     * - externalUrl 없음: 우리 서버 OBS 송출용 → 스트림 키·채팅/오버레이 URL 고정(재발급은 설정에서 가능).
     * - externalUrl 있음(간편 모드): 트위치/유튜브 URL만 등록.
     */
    @Transactional
    public StreamResponse create(Long userId, CreateStreamRequest request) {
        LiveStream stream = new LiveStream();
        stream.setUserId(userId);
        stream.setTitle(request.getTitle());
        stream.setGame(request.getGame());
        stream.setStatus(StreamStatus.CREATED);

        if (request.getExternalUrl() != null && !request.getExternalUrl().isBlank()) {
            stream.setExternalUrl(request.getExternalUrl().trim());
            stream.setStreamKey(null);
            stream.setPlaybackUrl(null);
        } else {
            String streamKey = UUID.randomUUID().toString().replace("-", "");
            String playbackUrl = streamingProperties.getHlsBaseUrl().replaceAll("/$", "") + "/live/" + streamKey + "/index.m3u8";
            stream.setStreamKey(streamKey);
            stream.setPlaybackUrl(playbackUrl);
        }

        liveStreamRepository.save(stream);
        return StreamResponse.from(stream);
    }

    /**
     * 스트림 정보 수정 (제목, 카테고리). 본인 스트림만 수정 가능.
     */
    @Transactional
    public StreamResponse update(Long streamId, Long userId, UpdateStreamRequest request) {
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("스트림을 찾을 수 없습니다."));
        if (!stream.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        stream.setTitle(request.getTitle());
        stream.setGame(request.getGame());
        liveStreamRepository.save(stream);
        return StreamResponse.from(stream);
    }

    /**
     * OBS Studio 설정에 필요한 서버 URL과 스트림 키 반환.
     * 외부 URL(트위치/유튜브) 연동 스트림은 OBS 설정 없음.
     */
    public ObsSetupResponse getObsSetup(Long streamId, Long userId) {
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("스트림을 찾을 수 없습니다."));
        if (!stream.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        if (stream.getStreamKey() == null || stream.getStreamKey().isBlank()) {
            throw new IllegalArgumentException("트위치/유튜브 연동 스트림은 OBS 설정이 없습니다. OBS는 우리 서버 송출용 스트림에만 사용하세요.");
        }
        String serverUrl = streamingProperties.getRtmpServerUrl().replaceAll("/$", "");

        return ObsSetupResponse.builder()
                .streamKey(stream.getStreamKey())
                .serverUrl(serverUrl)
                .instructions("OBS 스튜디오에서 방송 설정 → 서비스: 사용자 지정, 서버: " + serverUrl + ", 스트림 키: " + stream.getStreamKey())
                .build();
    }

    /** 스트림 키 재발급. OBS 등에 새 키를 다시 입력해야 함. */
    @Transactional
    public String regenerateStreamKey(Long streamId, Long userId) {
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("스트림을 찾을 수 없습니다."));
        if (!stream.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        if (stream.getStreamKey() == null || stream.getStreamKey().isBlank()) {
            throw new IllegalArgumentException("트위치/유튜브 연동 스트림은 스트림 키 재발급이 없습니다.");
        }
        String newKey = UUID.randomUUID().toString().replace("-", "");
        String baseUrl = streamingProperties.getHlsBaseUrl().replaceAll("/$", "");
        stream.setStreamKey(newKey);
        stream.setPlaybackUrl(baseUrl + "/live/" + newKey + "/index.m3u8");
        liveStreamRepository.save(stream);
        return newKey;
    }

    /** nginx-rtmp on_publish 콜백: 스트림 키로 방송 시작 처리 */
    @Transactional
    public boolean notifyLive(String streamKey) {
        if (streamKey == null || streamKey.isBlank()) return false;
        return liveStreamRepository.findByStreamKey(streamKey)
                .map(stream -> {
                    stream.setStatus(StreamStatus.LIVE);
                    stream.setStartedAt(LocalDateTime.now());
                    // OBS 재연결 시 notify/end로 endedAt이 남아 있으면 시청 화면이 LIVE && !endedAt 조건에서 오프라인으로 보임
                    stream.setEndedAt(null);
                    liveStreamRepository.save(stream);
                    notificationService.createForFollowedStreamStart(stream.getId(), stream.getUserId());
                    return true;
                })
                .orElse(false);
    }

    /** nginx-rtmp on_publish_done 콜백: 방송 종료 처리 */
    @Transactional
    public boolean notifyEnd(String streamKey) {
        if (streamKey == null || streamKey.isBlank()) return false;
        return liveStreamRepository.findByStreamKey(streamKey)
                .map(stream -> {
                    stream.setStatus(StreamStatus.ENDED);
                    stream.setEndedAt(LocalDateTime.now());
                    liveStreamRepository.save(stream);
                    return true;
                })
                .orElse(false);
    }

    /** 방송자가 수동으로 방송 종료 처리 (RTMP notify/end 미호출 시 사용). 본인 방송이고 LIVE일 때만 ENDED로 변경. */
    @Transactional
    public void endStreamByOwner(Long streamId, Long userId) {
        if (streamId == null || userId == null) throw new IllegalArgumentException("방송 정보가 올바르지 않습니다.");
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        if (!userId.equals(stream.getUserId())) throw new IllegalArgumentException("본인 방송만 종료 처리할 수 있습니다.");
        if (stream.getStatus() != StreamStatus.LIVE) return;
        stream.setStatus(StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        liveStreamRepository.save(stream);
    }

    /** 스트림 키 유효 여부 (on_publish에서 2xx 시 허용) */
    public boolean isValidStreamKey(String streamKey) {
        return streamKey != null && !streamKey.isBlank()
                && liveStreamRepository.findByStreamKey(streamKey).isPresent();
    }

    /** 스트림 존재 및 LIVE 상태 여부 (시청 join 허용용) */
    public boolean existsAndLive(Long streamId) {
        if (streamId == null) return false;
        return liveStreamRepository.findById(streamId)
                .map(s -> s.getStatus() == StreamStatus.LIVE && s.getEndedAt() == null)
                .orElse(false);
    }

    public void viewerJoin(Long streamId, Long userId) {
        streamViewerCountService.join(streamId, userId);
    }

    public void viewerLeave(Long streamId, Long userId) {
        streamViewerCountService.leave(streamId, userId);
    }

    public List<StreamResponse> listLive() {
        return liveStreamRepository.findByStatusAndEndedAtIsNullOrderByStartedAtDesc(StreamStatus.LIVE)
                .stream()
                .map(s -> {
                    long fc = s.getUserId() != null ? followRepository.countByFollowingId(s.getUserId()) : 0L;
                    int vc = streamViewerCountService.getViewerCount(s.getId());
                    return StreamResponse.from(s, getBroadcasterDisplayName(s.getUserId()), fc, vc, getBroadcasterProfileImageUrl(s.getUserId()));
                })
                .collect(Collectors.toList());
    }

    public List<StreamResponse> listByUser(Long userId) {
        return listByUser(userId, null);
    }

    public List<StreamResponse> listByUser(Long userId, Long actorUserId) {
        String name = getBroadcasterDisplayName(userId);
        String profileImageUrl = getBroadcasterProfileImageUrl(userId);
        boolean canManage = canManageChannel(userId, actorUserId);
        return liveStreamRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(stream -> canManage || Boolean.TRUE.equals(stream.getVisibleInRecent()))
                .map(s -> {
                    int vc = streamViewerCountService.getViewerCount(s.getId());
                    return StreamResponse.from(s, name, null, vc, profileImageUrl).toBuilder()
                            .canManage(canManage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public StreamResponse updateVisibility(Long streamId, Long actorUserId, boolean visibleInRecent) {
        LiveStream stream = requireManageableStream(streamId, actorUserId);
        stream.setVisibleInRecent(visibleInRecent);
        liveStreamRepository.save(stream);
        return StreamResponse.from(
                stream,
                getBroadcasterDisplayName(stream.getUserId()),
                null,
                streamViewerCountService.getViewerCount(stream.getId()),
                getBroadcasterProfileImageUrl(stream.getUserId()),
                getBroadcasterStreamerTier(stream.getUserId())
        ).toBuilder().canManage(true).build();
    }

    @Transactional
    public void deleteFromChannel(Long streamId, Long actorUserId) {
        LiveStream stream = requireManageableStream(streamId, actorUserId);
        if (stream.getStatus() == StreamStatus.LIVE) {
            throw new IllegalArgumentException("진행 중인 방송은 삭제할 수 없습니다. 먼저 방송을 종료해 주세요.");
        }
        liveStreamRepository.delete(stream);
    }

    public StreamResponse getById(Long id) {
        LiveStream stream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("스트림을 찾을 수 없습니다."));
        long followerCount = stream.getUserId() != null ? followRepository.countByFollowingId(stream.getUserId()) : 0L;
        int viewerCount = streamViewerCountService.getViewerCount(id);
        return StreamResponse.from(stream, getBroadcasterDisplayName(stream.getUserId()), followerCount, viewerCount, getBroadcasterProfileImageUrl(stream.getUserId()), getBroadcasterStreamerTier(stream.getUserId()));
    }

    private boolean canManageChannel(Long ownerUserId, Long actorUserId) {
        if (ownerUserId == null || actorUserId == null) return false;
        return ownerUserId.equals(actorUserId)
                || channelPermissionRepository.existsByOwnerUserIdAndManagerUserId(ownerUserId, actorUserId);
    }

    private LiveStream requireManageableStream(Long streamId, Long actorUserId) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        LiveStream stream = liveStreamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        if (!canManageChannel(stream.getUserId(), actorUserId)) {
            throw new IllegalArgumentException("이 방송을 관리할 권한이 없습니다.");
        }
        return stream;
    }

    /** 최근 방송 목록 - 종료된 방송만 (지금 라이브와 중복되지 않음) */
    public List<StreamResponse> listRecent(int limit) {
        Map<Long, LiveStream> latestByUser = new LinkedHashMap<>();
        for (LiveStream stream : liveStreamRepository.findTop20ByStatusInOrderByStartedAtDesc(List.of(StreamStatus.ENDED))) {
            if (stream.getUserId() == null || latestByUser.containsKey(stream.getUserId())) {
                continue;
            }
            latestByUser.put(stream.getUserId(), stream);
            if (latestByUser.size() >= limit) {
                break;
            }
        }
        return latestByUser.values().stream()
                .map(s -> {
                    int vc = streamViewerCountService.getViewerCount(s.getId());
                    return StreamResponse.from(s, getBroadcasterDisplayName(s.getUserId()), null, vc, getBroadcasterProfileImageUrl(s.getUserId()));
                })
                .collect(Collectors.toList());
    }

    /** 팔로우한 사용자들의 라이브 방송 */
    public List<StreamResponse> listLiveByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return liveStreamRepository.findByUserIdInAndStatusAndEndedAtIsNullOrderByStartedAtDesc(userIds, StreamStatus.LIVE).stream()
                .map(s -> {
                    int vc = streamViewerCountService.getViewerCount(s.getId());
                    return StreamResponse.from(s, getBroadcasterDisplayName(s.getUserId()), null, vc, getBroadcasterProfileImageUrl(s.getUserId()));
                })
                .collect(Collectors.toList());
    }

    /** 팔로우한 사용자들의 최근 방송 (LIVE/ENDED) */
    public List<StreamResponse> listRecentByUserIds(List<Long> userIds, int limit) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        Map<Long, LiveStream> latestByUser = new LinkedHashMap<>();
        for (LiveStream stream : liveStreamRepository.findTop20ByUserIdInAndStatusInOrderByStartedAtDesc(userIds, List.of(StreamStatus.ENDED))) {
            if (stream.getUserId() == null || latestByUser.containsKey(stream.getUserId())) {
                continue;
            }
            latestByUser.put(stream.getUserId(), stream);
            if (latestByUser.size() >= limit) {
                break;
            }
        }
        return latestByUser.values().stream()
                .map(s -> {
                    int vc = streamViewerCountService.getViewerCount(s.getId());
                    return StreamResponse.from(s, getBroadcasterDisplayName(s.getUserId()), null, vc, getBroadcasterProfileImageUrl(s.getUserId()));
                })
                .collect(Collectors.toList());
    }
}
