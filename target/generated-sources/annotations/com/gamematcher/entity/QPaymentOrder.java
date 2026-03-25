package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPaymentOrder is a Querydsl query type for PaymentOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPaymentOrder extends EntityPathBase<PaymentOrder> {

    private static final long serialVersionUID = 1827603160L;

    public static final QPaymentOrder paymentOrder = new QPaymentOrder("paymentOrder");

    public final NumberPath<Long> amountWon = createNumber("amountWon", Long.class);

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath impUid = createString("impUid");

    public final EnumPath<com.gamematcher.constant.PaymentOrderKind> kind = createEnum("kind", com.gamematcher.constant.PaymentOrderKind.class);

    public final StringPath orderId = createString("orderId");

    public final NumberPath<Integer> pangAmount = createNumber("pangAmount", Integer.class);

    public final StringPath status = createString("status");

    public final NumberPath<Long> targetUserId = createNumber("targetUserId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPaymentOrder(String variable) {
        super(PaymentOrder.class, forVariable(variable));
    }

    public QPaymentOrder(Path<? extends PaymentOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPaymentOrder(PathMetadata metadata) {
        super(PaymentOrder.class, metadata);
    }

}

