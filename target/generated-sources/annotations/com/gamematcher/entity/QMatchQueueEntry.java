package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatchQueueEntry is a Querydsl query type for MatchQueueEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchQueueEntry extends EntityPathBase<MatchQueueEntry> {

    private static final long serialVersionUID = -376883818L;

    public static final QMatchQueueEntry matchQueueEntry = new QMatchQueueEntry("matchQueueEntry");

    public final StringPath game = createString("game");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final StringPath position = createString("position");

    public final StringPath tier = createString("tier");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QMatchQueueEntry(String variable) {
        super(MatchQueueEntry.class, forVariable(variable));
    }

    public QMatchQueueEntry(Path<? extends MatchQueueEntry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatchQueueEntry(PathMetadata metadata) {
        super(MatchQueueEntry.class, metadata);
    }

}

