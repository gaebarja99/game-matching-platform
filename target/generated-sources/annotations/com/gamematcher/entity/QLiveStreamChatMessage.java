package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLiveStreamChatMessage is a Querydsl query type for LiveStreamChatMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLiveStreamChatMessage extends EntityPathBase<LiveStreamChatMessage> {

    private static final long serialVersionUID = 706781811L;

    public static final QLiveStreamChatMessage liveStreamChatMessage = new QLiveStreamChatMessage("liveStreamChatMessage");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final StringPath text = createString("text");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QLiveStreamChatMessage(String variable) {
        super(LiveStreamChatMessage.class, forVariable(variable));
    }

    public QLiveStreamChatMessage(Path<? extends LiveStreamChatMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLiveStreamChatMessage(PathMetadata metadata) {
        super(LiveStreamChatMessage.class, metadata);
    }

}

