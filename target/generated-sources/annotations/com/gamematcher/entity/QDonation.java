package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDonation is a Querydsl query type for Donation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDonation extends EntityPathBase<Donation> {

    private static final long serialVersionUID = -1265743486L;

    public static final QDonation donation = new QDonation("donation");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromUserId = createNumber("fromUserId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath message = createString("message");

    public final NumberPath<Long> streamId = createNumber("streamId", Long.class);

    public final NumberPath<Long> toUserId = createNumber("toUserId", Long.class);

    public QDonation(String variable) {
        super(Donation.class, forVariable(variable));
    }

    public QDonation(Path<? extends Donation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDonation(PathMetadata metadata) {
        super(Donation.class, metadata);
    }

}

