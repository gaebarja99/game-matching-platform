package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPangWithdrawal is a Querydsl query type for PangWithdrawal
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPangWithdrawal extends EntityPathBase<PangWithdrawal> {

    private static final long serialVersionUID = -314798673L;

    public static final QPangWithdrawal pangWithdrawal = new QPangWithdrawal("pangWithdrawal");

    public final NumberPath<Long> amountPang = createNumber("amountPang", Long.class);

    public final NumberPath<Long> commissionPang = createNumber("commissionPang", Long.class);

    public final NumberPath<Integer> commissionPercent = createNumber("commissionPercent", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> netPang = createNumber("netPang", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPangWithdrawal(String variable) {
        super(PangWithdrawal.class, forVariable(variable));
    }

    public QPangWithdrawal(Path<? extends PangWithdrawal> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPangWithdrawal(PathMetadata metadata) {
        super(PangWithdrawal.class, metadata);
    }

}

