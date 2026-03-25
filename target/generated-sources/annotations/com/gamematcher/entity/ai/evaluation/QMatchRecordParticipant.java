package com.gamematcher.entity.ai.evaluation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchRecordParticipant is a Querydsl query type for MatchRecordParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchRecordParticipant extends EntityPathBase<MatchRecordParticipant> {

    private static final long serialVersionUID = -80603271L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchRecordParticipant matchRecordParticipant = new QMatchRecordParticipant("matchRecordParticipant");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMatchRecord matchRecord;

    public final StringPath rawStats = createString("rawStats");

    public final StringPath role = createString("role");

    public final com.gamematcher.entity.QUser user;

    public QMatchRecordParticipant(String variable) {
        this(MatchRecordParticipant.class, forVariable(variable), INITS);
    }

    public QMatchRecordParticipant(Path<? extends MatchRecordParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchRecordParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchRecordParticipant(PathMetadata metadata, PathInits inits) {
        this(MatchRecordParticipant.class, metadata, inits);
    }

    public QMatchRecordParticipant(Class<? extends MatchRecordParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.matchRecord = inits.isInitialized("matchRecord") ? new QMatchRecord(forProperty("matchRecord"), inits.get("matchRecord")) : null;
        this.user = inits.isInitialized("user") ? new com.gamematcher.entity.QUser(forProperty("user")) : null;
    }

}

