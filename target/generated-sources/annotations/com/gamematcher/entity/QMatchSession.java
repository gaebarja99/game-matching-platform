package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatchSession is a Querydsl query type for MatchSession
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchSession extends EntityPathBase<MatchSession> {

    private static final long serialVersionUID = 1367143169L;

    public static final QMatchSession matchSession = new QMatchSession("matchSession");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath game = createString("game");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QMatchSession(String variable) {
        super(MatchSession.class, forVariable(variable));
    }

    public QMatchSession(Path<? extends MatchSession> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatchSession(PathMetadata metadata) {
        super(MatchSession.class, metadata);
    }

}

