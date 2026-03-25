package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStreamChatBan is a Querydsl query type for StreamChatBan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStreamChatBan extends EntityPathBase<StreamChatBan> {

    private static final long serialVersionUID = 718888455L;

    public static final QStreamChatBan streamChatBan = new QStreamChatBan("streamChatBan");

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QStreamChatBan(String variable) {
        super(StreamChatBan.class, forVariable(variable));
    }

    public QStreamChatBan(Path<? extends StreamChatBan> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStreamChatBan(PathMetadata metadata) {
        super(StreamChatBan.class, metadata);
    }

}

