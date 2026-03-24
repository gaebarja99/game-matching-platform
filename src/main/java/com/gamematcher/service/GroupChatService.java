package com.gamematcher.service;

import com.gamematcher.entity.*;
import com.gamematcher.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_TEXT_LENGTH = 2000;

    private final GroupChatRoomRepository roomRepository;
    private final GroupChatRoomMemberRepository memberRepository;
    private final GroupChatMessageRepository messageRepository;
    private final GroupChatInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final FriendRequestService friendRequestService;
    private final SimpMessagingTemplate messagingTemplate;

    /** 방 생성 (생성자를 멤버로 추가) */
    @Transactional
    public GroupChatRoom createRoom(Long userId, String name) {
        if (userId == null || name == null || name.isBlank()) throw new IllegalArgumentException("방 이름을 입력해 주세요.");
        String trimmed = name.trim();
        if (trimmed.length() > 100) trimmed = trimmed.substring(0, 100);
        GroupChatRoom room = new GroupChatRoom();
        room.setName(trimmed);
        room.setCreatedByUserId(userId);
        room = roomRepository.save(room);
        GroupChatRoomMember member = new GroupChatRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        memberRepository.save(member);
        return room;
    }

    /** 내가 참여 중인 방 목록 */
    public List<Map<String, Object>> getMyRooms(Long userId) {
        if (userId == null) return List.of();
        List<GroupChatRoomMember> myMemberships = memberRepository.findByUserIdOrderByJoinedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupChatRoomMember m : myMemberships) {
            roomRepository.findById(m.getRoomId()).ifPresent(room -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", room.getId());
                map.put("name", room.getName());
                map.put("createdByUserId", room.getCreatedByUserId());
                long memberCount = memberRepository.findByRoomId(room.getId()).size();
                map.put("memberCount", memberCount);
                result.add(map);
            });
        }
        return result;
    }

    /** 방 멤버 목록 (userId, nickname, profileImageUrl) */
    public List<Map<String, Object>> getRoomMembers(Long roomId, Long requestUserId) {
        if (roomId == null || !isMember(roomId, requestUserId)) return List.of();
        return memberRepository.findByRoomId(roomId).stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("userId", m.getUserId());
                    userRepository.findById(m.getUserId()).ifPresent(u -> {
                        map.put("nickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername());
                        map.put("loginId", u.getLoginId());
                        map.put("profileImageUrl", u.getProfileImageUrl());
                    });
                    return map;
                })
                .collect(Collectors.toList());
    }

    /** 초대 (친구만 가능, 실시간 푸시) */
    @Transactional
    public String invite(Long roomId, Long fromUserId, Long toUserId) {
        if (roomId == null || fromUserId == null || toUserId == null) return "invalid";
        if (!isMember(roomId, fromUserId)) return "not_member";
        if (fromUserId.equals(toUserId)) return "invalid";
        if (!friendRequestService.getFriendUserIds(fromUserId).contains(toUserId)) return "not_friend";
        if (memberRepository.existsByRoomIdAndUserId(roomId, toUserId)) return "already_member";
        if (invitationRepository.existsByRoomIdAndToUserIdAndStatus(roomId, toUserId, GroupChatInvitation.InvitationStatus.PENDING)) return "already_pending";
        GroupChatInvitation inv = new GroupChatInvitation();
        inv.setRoomId(roomId);
        inv.setFromUserId(fromUserId);
        inv.setToUserId(toUserId);
        inv.setStatus(GroupChatInvitation.InvitationStatus.PENDING);
        invitationRepository.save(inv);
        String roomName = roomRepository.findById(roomId).map(GroupChatRoom::getName).orElse("채팅방");
        String fromNickname = userRepository.findById(fromUserId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("");
        messagingTemplate.convertAndSend("/topic/user/" + toUserId, Map.<String, Object>of(
                "type", "GROUP_CHAT_INVITE",
                "invitationId", inv.getId(),
                "roomId", roomId,
                "roomName", roomName,
                "fromUserId", fromUserId,
                "fromNickname", fromNickname != null ? fromNickname : ""
        ));
        return "sent";
    }

    /** 받은 초대 목록 (PENDING) */
    public List<Map<String, Object>> getReceivedInvitations(Long userId) {
        if (userId == null) return List.of();
        return invitationRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(userId, GroupChatInvitation.InvitationStatus.PENDING)
                .stream()
                .map(inv -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", inv.getId());
                    map.put("roomId", inv.getRoomId());
                    map.put("fromUserId", inv.getFromUserId());
                    map.put("createdAt", inv.getCreatedAt() != null ? inv.getCreatedAt().format(ISO) : "");
                    roomRepository.findById(inv.getRoomId()).ifPresent(r -> map.put("roomName", r.getName()));
                    userRepository.findById(inv.getFromUserId()).ifPresent(u -> {
                        map.put("fromNickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername());
                        map.put("fromLoginId", u.getLoginId());
                    });
                    return map;
                })
                .collect(Collectors.toList());
    }

    /** 수락 (멤버 추가 후 실시간으로 방에 멤버 참여 알림) */
    @Transactional
    public boolean acceptInvitation(Long invitationId, Long userId) {
        if (invitationId == null || userId == null) return false;
        Optional<GroupChatInvitation> opt = invitationRepository.findById(invitationId);
        if (opt.isEmpty()) return false;
        GroupChatInvitation inv = opt.get();
        if (!userId.equals(inv.getToUserId()) || inv.getStatus() != GroupChatInvitation.InvitationStatus.PENDING) return false;
        inv.setStatus(GroupChatInvitation.InvitationStatus.ACCEPTED);
        invitationRepository.save(inv);
        if (memberRepository.existsByRoomIdAndUserId(inv.getRoomId(), userId)) return true;
        GroupChatRoomMember member = new GroupChatRoomMember();
        member.setRoomId(inv.getRoomId());
        member.setUserId(userId);
        memberRepository.save(member);
        String nickname = userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("");
        sendSystemMessage(inv.getRoomId(), (nickname != null ? nickname : "") + "님이 들어왔습니다.");
        messagingTemplate.convertAndSend("/topic/group-room/" + inv.getRoomId(), Map.<String, Object>of(
                "type", "MEMBER_JOINED",
                "userId", userId,
                "nickname", nickname != null ? nickname : ""
        ));
        return true;
    }

    /** 거절 */
    @Transactional
    public boolean rejectInvitation(Long invitationId, Long userId) {
        if (invitationId == null || userId == null) return false;
        Optional<GroupChatInvitation> opt = invitationRepository.findById(invitationId);
        if (opt.isEmpty()) return false;
        GroupChatInvitation inv = opt.get();
        if (!userId.equals(inv.getToUserId()) || inv.getStatus() != GroupChatInvitation.InvitationStatus.PENDING) return false;
        inv.setStatus(GroupChatInvitation.InvitationStatus.REJECTED);
        invitationRepository.save(inv);
        return true;
    }

    /** 메시지 전송 (실시간 브로드캐스트) */
    @Transactional
    public GroupChatMessage sendMessage(Long roomId, Long userId, String text) {
        if (roomId == null || userId == null || !isMember(roomId, userId)) throw new IllegalArgumentException("권한이 없습니다.");
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) throw new IllegalArgumentException("메시지를 입력해 주세요.");
        if (trimmed.length() > MAX_TEXT_LENGTH) trimmed = trimmed.substring(0, MAX_TEXT_LENGTH);
        GroupChatMessage msg = new GroupChatMessage();
        msg.setRoomId(roomId);
        msg.setFromUserId(userId);
        msg.setText(trimmed);
        msg = messageRepository.save(msg);
        String fromNickname = null;
        String fromProfileImageUrl = null;
        var fromUser = userRepository.findById(userId);
        if (fromUser.isPresent()) {
            var u = fromUser.get();
            fromNickname = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
            fromProfileImageUrl = u.getProfileImageUrl();
        }
        Map<String, Object> payload = toMessagePayload(msg, fromNickname, fromProfileImageUrl, false);
        messagingTemplate.convertAndSend("/topic/group-room/" + roomId, payload);
        return msg;
    }

    /** 시스템 메시지 저장 + 브로드캐스트 (입장/퇴장 등) */
    @Transactional
    public GroupChatMessage sendSystemMessage(Long roomId, String text) {
        if (roomId == null) throw new IllegalArgumentException("roomId가 필요합니다.");
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) throw new IllegalArgumentException("시스템 메시지가 비어 있습니다.");
        if (trimmed.length() > MAX_TEXT_LENGTH) trimmed = trimmed.substring(0, MAX_TEXT_LENGTH);

        GroupChatMessage msg = new GroupChatMessage();
        msg.setRoomId(roomId);
        msg.setFromUserId(0L);
        msg.setText(trimmed);
        msg = messageRepository.save(msg);

        Map<String, Object> payload = toMessagePayload(msg, "", "", true);
        messagingTemplate.convertAndSend("/topic/group-room/" + roomId, payload);
        return msg;
    }

    /** 방 메시지 목록 (최신순) */
    public List<Map<String, Object>> getMessages(Long roomId, Long userId, int limit) {
        if (roomId == null || userId == null || !isMember(roomId, userId)) return List.of();
        int size = Math.min(Math.max(1, limit), 100);
        List<GroupChatMessage> list = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, size));
        Collections.reverse(list);
        return list.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("roomId", m.getRoomId());
            map.put("fromUserId", m.getFromUserId());
            map.put("text", m.getText());
            map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(ISO) : "");
            map.put("system", Long.valueOf(0L).equals(m.getFromUserId()));
            userRepository.findById(m.getFromUserId()).ifPresent(u -> {
                map.put("fromNickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername());
                map.put("fromProfileImageUrl", u.getProfileImageUrl());
            });
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toMessagePayload(GroupChatMessage msg, String fromNickname, String fromProfileImageUrl, boolean system) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MESSAGE");
        payload.put("id", msg.getId());
        payload.put("roomId", msg.getRoomId());
        payload.put("fromUserId", msg.getFromUserId());
        payload.put("fromNickname", fromNickname != null ? fromNickname : "");
        payload.put("fromProfileImageUrl", fromProfileImageUrl != null ? fromProfileImageUrl : "");
        payload.put("text", msg.getText());
        payload.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(ISO) : "");
        payload.put("system", system);
        return payload;
    }

    public boolean isMember(Long roomId, Long userId) {
        if (roomId == null || userId == null) return false;
        return memberRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public boolean recordPresence(Long roomId, Long userId, String action) {
        if (roomId == null || userId == null) return false;
        if (!isMember(roomId, userId)) return false;
        String normalized = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (!"enter".equals(normalized) && !"leave".equals(normalized)) return false;
        String nickname = userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("알수없음");
        String text = "enter".equals(normalized)
                ? nickname + "님이 들어왔습니다."
                : nickname + "님이 나갔습니다.";
        sendSystemMessage(roomId, text);
        return true;
    }

    /** 방장 권한으로 멤버 강퇴 */
    @Transactional
    public String kickMember(Long roomId, Long hostUserId, Long targetUserId) {
        if (roomId == null || hostUserId == null || targetUserId == null) return "invalid";
        Optional<GroupChatRoom> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return "not_found";
        GroupChatRoom room = roomOpt.get();
        if (!hostUserId.equals(room.getCreatedByUserId())) return "not_host";
        if (hostUserId.equals(targetUserId)) return "cannot_kick_self";

        Optional<GroupChatRoomMember> memberOpt = memberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (memberOpt.isEmpty()) return "not_member";
        memberRepository.delete(memberOpt.get());

        String targetNickname = userRepository.findById(targetUserId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("알 수 없음");
        sendSystemMessage(roomId, targetNickname + "님이 강퇴되었습니다.");

        messagingTemplate.convertAndSend("/topic/group-room/" + roomId, Map.<String, Object>of(
                "type", "MEMBER_KICKED",
                "userId", targetUserId
        ));
        return "ok";
    }

    /** 방장 권한으로 방 삭제 */
    @Transactional
    public String deleteRoom(Long roomId, Long hostUserId) {
        if (roomId == null || hostUserId == null) return "invalid";
        Optional<GroupChatRoom> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return "not_found";
        GroupChatRoom room = roomOpt.get();
        if (!hostUserId.equals(room.getCreatedByUserId())) return "not_host";

        invitationRepository.deleteAll(invitationRepository.findByRoomId(roomId));
        messageRepository.deleteAll(messageRepository.findByRoomId(roomId));
        memberRepository.deleteAll(memberRepository.findByRoomId(roomId));
        roomRepository.delete(room);
        return "ok";
    }
}
