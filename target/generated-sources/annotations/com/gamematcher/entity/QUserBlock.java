package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserBlock is a Querydsl query type for UserBlock
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserBlock extends EntityPathBase<UserBlock> {

    private static final long serialVersionUID = -1820980334L;

    public static final QUserBlock userBlock = new QUserBlock("userBlock");

    public final NumberPath<Long> blockedId = createNumber("blockedId", Long.class);

    public final NumberPath<Long> blockerId = createNumber("blockerId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QUserBlock(String variable) {
        super(UserBlock.class, forVariable(variable));
    }

    public QUserBlock(Path<? extends UserBlock> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserBlock(PathMetadata metadata) {
        super(UserBlock.class, metadata);
    }

}

