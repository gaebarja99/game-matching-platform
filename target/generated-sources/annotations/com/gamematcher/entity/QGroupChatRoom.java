package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroupChatRoom is a Querydsl query type for GroupChatRoom
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupChatRoom extends EntityPathBase<GroupChatRoom> {

    private static final long serialVersionUID = -1298301950L;

    public static final QGroupChatRoom groupChatRoom = new QGroupChatRoom("groupChatRoom");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> createdByUserId = createNumber("createdByUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public QGroupChatRoom(String variable) {
        super(GroupChatRoom.class, forVariable(variable));
    }

    public QGroupChatRoom(Path<? extends GroupChatRoom> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroupChatRoom(PathMetadata metadata) {
        super(GroupChatRoom.class, metadata);
    }

}

