package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGroupChatMessage is a Querydsl query type for GroupChatMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupChatMessage extends EntityPathBase<GroupChatMessage> {

    private static final long serialVersionUID = -1957765248L;

    public static final QGroupChatMessage groupChatMessage = new QGroupChatMessage("groupChatMessage");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> roomId = createNumber("roomId", Long.class);

    public final StringPath text = createString("text");

    public QGroupChatMessage(String variable) {
        super(GroupChatMessage.class, forVariable(variable));
    }

    public QGroupChatMessage(Path<? extends GroupChatMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGroupChatMessage(PathMetadata metadata) {
        super(GroupChatMessage.class, metadata);
    }

}

