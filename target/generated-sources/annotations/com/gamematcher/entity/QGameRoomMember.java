package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGameRoomMember is a Querydsl query type for GameRoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGameRoomMember extends EntityPathBase<GameRoomMember> {

    private static final long serialVersionUID = 1481152215L;

    public static final QGameRoomMember gameRoomMember = new QGameRoomMember("gameRoomMember");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QGameRoomMember(String variable) {
        super(GameRoomMember.class, forVariable(variable));
    }

    public QGameRoomMember(Path<? extends GameRoomMember> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGameRoomMember(PathMetadata metadata) {
        super(GameRoomMember.class, metadata);
    }

}

