package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRecruitPost is a Querydsl query type for RecruitPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecruitPost extends EntityPathBase<RecruitPost> {

    private static final long serialVersionUID = -807174898L;

    public static final QRecruitPost recruitPost = new QRecruitPost("recruitPost");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath findPosition = createString("findPosition");

    public final StringPath game = createString("game");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath mainPosition = createString("mainPosition");

    public final StringPath memo = createString("memo");

    public final StringPath mode = createString("mode");

    public final StringPath region = createString("region");

    public final StringPath summonerName = createString("summonerName");

    public final StringPath tier = createString("tier");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRecruitPost(String variable) {
        super(RecruitPost.class, forVariable(variable));
    }

    public QRecruitPost(Path<? extends RecruitPost> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRecruitPost(PathMetadata metadata) {
        super(RecruitPost.class, metadata);
    }

}

