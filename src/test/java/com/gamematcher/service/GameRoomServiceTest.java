package com.gamematcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamematcher.entity.GameRoom;
import com.gamematcher.entity.GameRoomMember;
import com.gamematcher.entity.GroupChatRoom;
import com.gamematcher.entity.GroupChatRoomMember;
import com.gamematcher.entity.User;
import com.gamematcher.repository.GameRoomMemberRepository;
import com.gamematcher.repository.GameRoomRepository;
import com.gamematcher.repository.GroupChatRoomMemberRepository;
import com.gamematcher.repository.GroupChatRoomRepository;
import com.gamematcher.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRoomServiceTest {

    @Mock
    GameRoomRepository roomRepository;
    @Mock
    GameRoomMemberRepository memberRepository;
    @Mock
    GroupChatRoomRepository groupChatRoomRepository;
    @Mock
    GroupChatRoomMemberRepository groupChatRoomMemberRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    GroupChatService groupChatService;

    @Test
    @DisplayName("마지막 유저가 나가면 랜덤 매칭방과 그룹 채팅방을 함께 삭제한다")
    void leaveDeletesRoomWhenLastMemberLeaves() {
        GameRoomService gameRoomService = new GameRoomService(
                roomRepository,
                memberRepository,
                groupChatRoomRepository,
                groupChatRoomMemberRepository,
                userRepository,
                messagingTemplate,
                groupChatService,
                new ObjectMapper()
        );
        GameRoom room = new GameRoom();
        room.setId(10L);
        room.setHostUserId(1L);
        room.setGroupChatRoomId(99L);

        GameRoomMember member = new GameRoomMember();
        member.setRoomId(10L);
        member.setUserId(1L);

        User user = new User();
        user.setUsername("host");

        GroupChatRoom groupChatRoom = new GroupChatRoom();
        groupChatRoom.setId(99L);

        GroupChatRoomMember chatMember = new GroupChatRoomMember();
        chatMember.setRoomId(99L);
        chatMember.setUserId(1L);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(memberRepository.findByRoomIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(memberRepository.countByRoomId(10L)).thenReturn(0L);
        when(memberRepository.findByRoomId(10L)).thenReturn(List.of());
        when(groupChatRoomMemberRepository.findByRoomIdAndUserId(99L, 1L)).thenReturn(Optional.of(chatMember));
        when(groupChatRoomMemberRepository.findByRoomId(99L)).thenReturn(List.of(chatMember));
        when(groupChatRoomRepository.findById(99L)).thenReturn(Optional.of(groupChatRoom));

        String result = gameRoomService.leave(10L, 1L);

        assertThat(result).isEqualTo("ok");
        verify(memberRepository).delete(member);
        verify(groupChatRoomMemberRepository, atLeastOnce()).delete(chatMember);
        verify(groupChatRoomRepository).delete(groupChatRoom);
        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("다른 유저가 남아 있으면 방은 유지한다")
    void leaveKeepsRoomWhenMembersRemain() {
        GameRoomService gameRoomService = new GameRoomService(
                roomRepository,
                memberRepository,
                groupChatRoomRepository,
                groupChatRoomMemberRepository,
                userRepository,
                messagingTemplate,
                groupChatService,
                new ObjectMapper()
        );
        GameRoom room = new GameRoom();
        room.setId(10L);
        room.setHostUserId(1L);
        room.setGroupChatRoomId(99L);

        GameRoomMember member = new GameRoomMember();
        member.setRoomId(10L);
        member.setUserId(2L);

        User user = new User();
        user.setUsername("guest");

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(memberRepository.findByRoomIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(memberRepository.countByRoomId(10L)).thenReturn(1L);
        when(groupChatRoomMemberRepository.findByRoomIdAndUserId(99L, 2L)).thenReturn(Optional.empty());

        String result = gameRoomService.leave(10L, 2L);

        assertThat(result).isEqualTo("ok");
        verify(roomRepository, never()).delete(any(GameRoom.class));
        verify(groupChatRoomRepository, never()).delete(any(GroupChatRoom.class));
    }
}
