package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroupChatRoomMember is a Querydsl query type for GroupChatRoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupChatRoomMember extends EntityPathBase<GroupChatRoomMember> {

    private static final long serialVersionUID = 8677692L;

    public static final QGroupChatRoomMember groupChatRoomMember = new QGroupChatRoomMember("groupChatRoomMember");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QGroupChatRoomMember(String variable) {
        super(GroupChatRoomMember.class, forVariable(variable));
    }

    public QGroupChatRoomMember(Path<? extends GroupChatRoomMember> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroupChatRoomMember(PathMetadata metadata) {
        super(GroupChatRoomMember.class, metadata);
    }

}

