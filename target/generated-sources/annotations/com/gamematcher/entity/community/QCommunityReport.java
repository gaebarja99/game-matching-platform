package com.gamematcher.entity.community;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommunityReport is a Querydsl query type for CommunityReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommunityReport extends EntityPathBase<CommunityReport> {

    private static final long serialVersionUID = 1880288904L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommunityReport communityReport = new QCommunityReport("communityReport");

    public final StringPath adminNote = createString("adminNote");

    public final NumberPath<Long> commentId = createNumber("commentId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> postId = createNumber("postId", Long.class);

    public final EnumPath<com.gamematcher.constant.ReportReason> reason = createEnum("reason", com.gamematcher.constant.ReportReason.class);

    public final com.gamematcher.entity.QUser reporter;

    public final DateTimePath<java.time.LocalDateTime> resolvedAt = createDateTime("resolvedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.gamematcher.constant.ReportStatus> status = createEnum("status", com.gamematcher.constant.ReportStatus.class);

    public final EnumPath<com.gamematcher.constant.community.ReportTargetType> targetType = createEnum("targetType", com.gamematcher.constant.community.ReportTargetType.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QCommunityReport(String variable) {
        this(CommunityReport.class, forVariable(variable), INITS);
    }

    public QCommunityReport(Path<? extends CommunityReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommunityReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommunityReport(PathMetadata metadata, PathInits inits) {
        this(CommunityReport.class, metadata, inits);
    }

    public QCommunityReport(Class<? extends CommunityReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reporter = inits.isInitialized("reporter") ? new com.gamematcher.entity.QUser(forProperty("reporter")) : null;
    }

}

