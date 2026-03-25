package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGameRoom is a Querydsl query type for GameRoom
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGameRoom extends EntityPathBase<GameRoom> {

    private static final long serialVersionUID = 101628381L;

    public static final QGameRoom gameRoom = new QGameRoom("gameRoom");

    public final BooleanPath closed = createBoolean("closed");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath deletePassword = createString("deletePassword");

    public final StringPath game = createString("game");

    public final StringPath gameOptions = createString("gameOptions");

    public final NumberPath<Long> groupChatRoomId = createNumber("groupChatRoomId", Long.class);

    public final NumberPath<Long> hostUserId = createNumber("hostUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final StringPath title = createString("title");

    public QGameRoom(String variable) {
        super(GameRoom.class, forVariable(variable));
    }

    public QGameRoom(Path<? extends GameRoom> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGameRoom(PathMetadata metadata) {
        super(GameRoom.class, metadata);
    }

}

