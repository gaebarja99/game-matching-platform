package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStreamBlacklist is a Querydsl query type for StreamBlacklist
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStreamBlacklist extends EntityPathBase<StreamBlacklist> {

    private static final long serialVersionUID = -897672819L;

    public static final QStreamBlacklist streamBlacklist = new QStreamBlacklist("streamBlacklist");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QStreamBlacklist(String variable) {
        super(StreamBlacklist.class, forVariable(variable));
    }

    public QStreamBlacklist(Path<? extends StreamBlacklist> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStreamBlacklist(PathMetadata metadata) {
        super(StreamBlacklist.class, metadata);
    }

}

