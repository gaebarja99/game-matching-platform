package com.gamematcher.entity.community;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostAttachment is a Querydsl query type for PostAttachment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostAttachment extends EntityPathBase<PostAttachment> {

    private static final long serialVersionUID = -1171257224L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostAttachment postAttachment = new QPostAttachment("postAttachment");

    public final StringPath contentType = createString("contentType");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath fileName = createString("fileName");

    public final StringPath filePath = createString("filePath");

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPost post;

    public QPostAttachment(String variable) {
        this(PostAttachment.class, forVariable(variable), INITS);
    }

    public QPostAttachment(Path<? extends PostAttachment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostAttachment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostAttachment(PathMetadata metadata, PathInits inits) {
        this(PostAttachment.class, metadata, inits);
    }

    public QPostAttachment(Class<? extends PostAttachment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPost(forProperty("post"), inits.get("post")) : null;
    }

}

