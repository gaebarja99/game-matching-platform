package com.gamematcher.service;

import com.gamematcher.entity.GameRoom;
import com.gamematcher.entity.GameRoomMember;
import com.gamematcher.entity.GroupChatRoom;
import com.gamematcher.entity.GroupChatRoomMember;
import com.gamematcher.repository.GameRoomMemberRepository;
import com.gamematcher.repository.GameRoomRepository;
import com.gamematcher.repository.GroupChatRoomMemberRepository;
import com.gamematcher.repository.GroupChatRoomRepository;
import com.gamematcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GameRoomRepository roomRepository;
    private final GameRoomMemberRepository memberRepository;
    private final GroupChatRoomRepository groupChatRoomRepository;
    private final GroupChatRoomMemberRepository groupChatRoomMemberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GroupChatService groupChatService;

    /** 방 만들기: 제목, 메모, 삭제 비밀번호 필수 + 게임·게임옵션. 그룹 채팅방도 생성해 방 채팅 연동 */
    @Transactional
    public GameRoom create(Long userId, String title, String memo, String deletePassword,
                           String game, String gameOptions) {
        if (userId == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목을 입력해 주세요.");
        if (deletePassword == null || deletePassword.isBlank()) throw new IllegalArgumentException("삭제용 비밀번호를 입력해 주세요.");
        if (game == null || game.isBlank()) game = "LEAGUE_OF_LEGENDS";

        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > 200) trimmedTitle = trimmedTitle.substring(0, 200);

        GroupChatRoom groupRoom = new GroupChatRoom();
        groupRoom.setName(trimmedTitle);
        groupRoom.setCreatedByUserId(userId);
        groupRoom = groupChatRoomRepository.save(groupRoom);

        GroupChatRoomMember groupMember = new GroupChatRoomMember();
        groupMember.setRoomId(groupRoom.getId());
        groupMember.setUserId(userId);
        groupChatRoomMemberRepository.save(groupMember);

        GameRoom room = new GameRoom();
        room.setTitle(trimmedTitle);
        room.setMemo(memo != null ? memo.trim() : null);
        room.setDeletePassword(deletePassword);
        room.setGame(game);
        room.setGameOptions(gameOptions != null ? gameOptions.trim() : null);
        room.setHostUserId(userId);
        room.setGroupChatRoomId(groupRoom.getId());
        room.setClosed(false);
        room = roomRepository.save(room);

        GameRoomMember member = new GameRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        memberRepository.save(member);

        return room;
    }

    /** 방 목록 조회 (게임·마감 여부 필터. closed=true면 마감된 방만. userId 있으면 isMember 포함) */
    public List<Map<String, Object>> listRooms(String game, Boolean closed, Long userId) {
        boolean closedFilter = Boolean.TRUE.equals(closed);
        List<GameRoom> rooms = game != null && !game.isBlank() && !"ALL".equalsIgnoreCase(game)
                ? roomRepository.findByGameAndClosedOrderByCreatedAtDesc(game, closedFilter)
                : roomRepository.findByClosedOrderByCreatedAtDesc(closedFilter);
        return rooms.stream().map(r -> toRoomMap(r, userId)).collect(Collectors.toList());
    }

    private Map<String, Object> toRoomMap(GameRoom r, Long requestUserId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("title", r.getTitle());
        map.put("memo", r.getMemo());
        map.put("game", r.getGame());
        map.put("gameOptions", r.getGameOptions());
        map.put("hostUserId", r.getHostUserId());
        map.put("groupChatRoomId", r.getGroupChatRoomId());
        map.put("closed", r.isClosed());
        map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(ISO) : "");
        map.put("memberCount", memberRepository.countByRoomId(r.getId()));
        map.put("isMember", requestUserId != null && memberRepository.existsByRoomIdAndUserId(r.getId(), requestUserId));
        map.put("isHost", requestUserId != null && r.getHostUserId().equals(requestUserId));
        userRepository.findById(r.getHostUserId()).ifPresent(u ->
                map.put("hostNickname", (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername()));
        return map;
    }

    /** 참가 */
    @Transactional
    public String join(Long roomId, Long userId) {
        if (roomId == null || userId == null) return "invalid";
        Optional<GameRoom> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return "not_found";
        GameRoom room = opt.get();
        if (room.isClosed()) return "closed";
        if (memberRepository.existsByRoomIdAndUserId(roomId, userId)) return "already_member";

        GameRoomMember member = new GameRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        memberRepository.save(member);

        if (room.getGroupChatRoomId() != null) {
            if (!groupChatRoomMemberRepository.existsByRoomIdAndUserId(room.getGroupChatRoomId(), userId)) {
                GroupChatRoomMember gm = new GroupChatRoomMember();
                gm.setRoomId(room.getGroupChatRoomId());
                gm.setUserId(userId);
                groupChatRoomMemberRepository.save(gm);
            }
        }

        /* 다른 유저(방장·기존 멤버)에게 참가 알림 */
        String joinerNickname = userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("알 수 없음");
        String roomTitle = room.getTitle() != null ? room.getTitle() : "게임방";
        Set<Long> toNotify = new HashSet<>();
        if (!room.getHostUserId().equals(userId)) toNotify.add(room.getHostUserId());
        memberRepository.findByRoomId(roomId).stream()
                .map(GameRoomMember::getUserId)
                .filter(id -> !id.equals(userId))
                .forEach(toNotify::add);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "GAME_ROOM_JOIN");
        payload.put("roomId", roomId);
        payload.put("roomTitle", roomTitle);
        payload.put("joinerNickname", joinerNickname);
        payload.put("joinerUserId", userId);
        for (Long uid : toNotify) {
            messagingTemplate.convertAndSend("/topic/user/" + uid, payload);
        }

        /* 채팅방에 "OOO님이 들어왔습니다" 표시용 */
        if (room.getGroupChatRoomId() != null) {
            groupChatService.sendSystemMessage(room.getGroupChatRoomId(), joinerNickname + "님이 들어왔습니다.");
            Map<String, Object> chatPayload = new LinkedHashMap<>();
            chatPayload.put("type", "MEMBER_JOINED");
            chatPayload.put("userId", userId);
            chatPayload.put("nickname", joinerNickname);
            messagingTemplate.convertAndSend("/topic/group-room/" + room.getGroupChatRoomId(), chatPayload);
        }

        return "ok";
    }

    /** 나가기 (방장은 나가기 불가) */
    @Transactional
    public String leave(Long roomId, Long userId) {
        if (roomId == null || userId == null) return "invalid";
        Optional<GameRoom> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return "not_found";
        GameRoom room = opt.get();
        if (room.getHostUserId().equals(userId)) return "host_cannot_leave";
        Optional<GameRoomMember> m = memberRepository.findByRoomIdAndUserId(roomId, userId);
        if (m.isEmpty()) return "not_member";
        String leaverNickname = userRepository.findById(userId)
                .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                .orElse("알 수 없음");
        memberRepository.delete(m.get());
        if (room.getGroupChatRoomId() != null) {
            groupChatRoomMemberRepository.findByRoomIdAndUserId(room.getGroupChatRoomId(), userId)
                    .ifPresent(groupChatRoomMemberRepository::delete);
            groupChatService.sendSystemMessage(room.getGroupChatRoomId(), leaverNickname + "님이 나갔습니다.");
            /* 채팅방에 "OOO님이 나갔습니다" 표시용 */
            Map<String, Object> chatPayload = new LinkedHashMap<>();
            chatPayload.put("type", "MEMBER_LEFT");
            chatPayload.put("userId", userId);
            chatPayload.put("nickname", leaverNickname);
            messagingTemplate.convertAndSend("/topic/group-room/" + room.getGroupChatRoomId(), chatPayload);
        }
        return "ok";
    }

    /** 방장 전용: 마감 */
    @Transactional
    public String close(Long roomId, Long userId) {
        if (roomId == null || userId == null) return "invalid";
        Optional<GameRoom> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return "not_found";
        GameRoom room = opt.get();
        if (!room.getHostUserId().equals(userId)) return "not_host";
        room.setClosed(true);
        roomRepository.save(room);
        return "ok";
    }

    /** 방장 전용: 삭제 (비밀번호 확인) */
    @Transactional
    public String delete(Long roomId, Long userId, String password) {
        if (roomId == null || userId == null) return "invalid";
        Optional<GameRoom> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return "not_found";
        GameRoom room = opt.get();
        if (!room.getHostUserId().equals(userId)) return "not_host";
        if (password == null || !password.equals(room.getDeletePassword())) return "wrong_password";
        memberRepository.findByRoomId(roomId).forEach(memberRepository::delete);
        roomRepository.delete(room);
        return "ok";
    }

    public boolean isMember(Long roomId, Long userId) {
        return roomId != null && userId != null && memberRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    public boolean isHost(Long roomId, Long userId) {
        return roomId != null && userId != null
                && roomRepository.findById(roomId).map(r -> r.getHostUserId().equals(userId)).orElse(false);
    }

    public Optional<GameRoom> getRoom(Long roomId) {
        return roomRepository.findById(roomId);
    }

    /** 참가한 방의 그룹 채팅방 ID (방 채팅용) */
    public Optional<Long> getGroupChatRoomId(Long gameRoomId, Long userId) {
        if (!isMember(gameRoomId, userId)) return Optional.empty();
        return roomRepository.findById(gameRoomId).map(GameRoom::getGroupChatRoomId);
    }
}
