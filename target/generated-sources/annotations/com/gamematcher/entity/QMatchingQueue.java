package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatchingQueue is a Querydsl query type for MatchingQueue
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchingQueue extends EntityPathBase<MatchingQueue> {

    private static final long serialVersionUID = -1858115804L;

    public static final QMatchingQueue matchingQueue = new QMatchingQueue("matchingQueue");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath matched = createBoolean("matched");

    public final EnumPath<com.gamematcher.constant.LolPosition> position = createEnum("position", com.gamematcher.constant.LolPosition.class);

    public final EnumPath<com.gamematcher.constant.LolTier> tier = createEnum("tier", com.gamematcher.constant.LolTier.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QMatchingQueue(String variable) {
        super(MatchingQueue.class, forVariable(variable));
    }

    public QMatchingQueue(Path<? extends MatchingQueue> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatchingQueue(PathMetadata metadata) {
        super(MatchingQueue.class, metadata);
    }

}

