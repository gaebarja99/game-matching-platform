package com.gamematcher.repository;

import com.gamematcher.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    List<GameRoom> findByGameAndClosedOrderByCreatedAtDesc(String game, boolean closed);

    List<GameRoom> findByClosedOrderByCreatedAtDesc(boolean closed);
}
