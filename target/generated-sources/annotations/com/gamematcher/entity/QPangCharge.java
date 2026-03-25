package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPangCharge is a Querydsl query type for PangCharge
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPangCharge extends EntityPathBase<PangCharge> {

    private static final long serialVersionUID = 193511598L;

    public static final QPangCharge pangCharge = new QPangCharge("pangCharge");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath impUid = createString("impUid");

    public final StringPath orderId = createString("orderId");

    public final NumberPath<Integer> pangAmount = createNumber("pangAmount", Integer.class);

    public final NumberPath<Long> priceWon = createNumber("priceWon", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPangCharge(String variable) {
        super(PangCharge.class, forVariable(variable));
    }

    public QPangCharge(Path<? extends PangCharge> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPangCharge(PathMetadata metadata) {
        super(PangCharge.class, metadata);
    }

}

