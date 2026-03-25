package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDmMessage is a Querydsl query type for DmMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDmMessage extends EntityPathBase<DmMessage> {

    private static final long serialVersionUID = 1117031246L;

    public static final QDmMessage dmMessage = new QDmMessage("dmMessage");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath text = createString("text");

    public final NumberPath<Long> toUserId = createNumber("toUserId", Long.class);

    public QDmMessage(String variable) {
        super(DmMessage.class, forVariable(variable));
    }

    public QDmMessage(Path<? extends DmMessage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDmMessage(PathMetadata metadata) {
        super(DmMessage.class, metadata);
    }

}

