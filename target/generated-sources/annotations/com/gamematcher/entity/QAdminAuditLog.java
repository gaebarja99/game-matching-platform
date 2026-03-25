package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAdminAuditLog is a Querydsl query type for AdminAuditLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminAuditLog extends EntityPathBase<AdminAuditLog> {

    private static final long serialVersionUID = -829302296L;

    public static final QAdminAuditLog adminAuditLog = new QAdminAuditLog("adminAuditLog");

    public final StringPath actionType = createString("actionType");

    public final NumberPath<Long> adminUserId = createNumber("adminUserId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath detail = createString("detail");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath summary = createString("summary");

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final StringPath targetType = createString("targetType");

    public QAdminAuditLog(String variable) {
        super(AdminAuditLog.class, forVariable(variable));
    }

    public QAdminAuditLog(Path<? extends AdminAuditLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAdminAuditLog(PathMetadata metadata) {
        super(AdminAuditLog.class, metadata);
    }

}

