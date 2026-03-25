package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatchChatMessage is a Querydsl query type for MatchChatMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchChatMessage extends EntityPathBase<MatchChatMessage> {

    private static final long serialVersionUID = 1136219258L;

    public static final QMatchChatMessage matchChatMessage = new QMatchChatMessage("matchChatMessage");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> sessionId = createNumber("sessionId", Long.class);

    public final StringPath text = createString("text");

    public QMatchChatMessage(String variable) {
        super(MatchChatMessage.class, forVariable(variable));
    }

    public QMatchChatMessage(Path<? extends MatchChatMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatchChatMessage(PathMetadata metadata) {
        super(MatchChatMessage.class, metadata);
    }

}

