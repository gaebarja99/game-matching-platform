package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMileagePurchase is a Querydsl query type for MileagePurchase
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMileagePurchase extends EntityPathBase<MileagePurchase> {

    private static final long serialVersionUID = -840658949L;

    public static final QMileagePurchase mileagePurchase = new QMileagePurchase("mileagePurchase");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> mileageCost = createNumber("mileageCost", Long.class);

    public final NumberPath<Integer> pangAmount = createNumber("pangAmount", Integer.class);

    public final NumberPath<Long> targetUserId = createNumber("targetUserId", Long.class);

    public final EnumPath<com.gamematcher.constant.MileagePurchaseType> type = createEnum("type", com.gamematcher.constant.MileagePurchaseType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QMileagePurchase(String variable) {
        super(MileagePurchase.class, forVariable(variable));
    }

    public QMileagePurchase(Path<? extends MileagePurchase> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMileagePurchase(PathMetadata metadata) {
        super(MileagePurchase.class, metadata);
    }

}

