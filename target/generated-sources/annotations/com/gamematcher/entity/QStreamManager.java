package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStreamManager is a Querydsl query type for StreamManager
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStreamManager extends EntityPathBase<StreamManager> {

    private static final long serialVersionUID = 815062045L;

    public static final QStreamManager streamManager = new QStreamManager("streamManager");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QStreamManager(String variable) {
        super(StreamManager.class, forVariable(variable));
    }

    public QStreamManager(Path<? extends StreamManager> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStreamManager(PathMetadata metadata) {
        super(StreamManager.class, metadata);
    }

}

