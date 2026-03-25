package com.gamematcher.entity.ai.evaluation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchRecordEvaluation is a Querydsl query type for MatchRecordEvaluation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchRecordEvaluation extends EntityPathBase<MatchRecordEvaluation> {

    private static final long serialVersionUID = 1108262454L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchRecordEvaluation matchRecordEvaluation = new QMatchRecordEvaluation("matchRecordEvaluation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath detailedComment = createString("detailedComment");

    public final DateTimePath<java.time.LocalDateTime> evaluatedAt = createDateTime("evaluatedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.Grade> grade = createEnum("grade", com.gamematcher.constant.ai.evaluation.Grade.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMatchRecordParticipant participant;

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.EvaluationStatus> status = createEnum("status", com.gamematcher.constant.ai.evaluation.EvaluationStatus.class);

    public final StringPath summary = createString("summary");

    public QMatchRecordEvaluation(String variable) {
        this(MatchRecordEvaluation.class, forVariable(variable), INITS);
    }

    public QMatchRecordEvaluation(Path<? extends MatchRecordEvaluation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchRecordEvaluation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchRecordEvaluation(PathMetadata metadata, PathInits inits) {
        this(MatchRecordEvaluation.class, metadata, inits);
    }

    public QMatchRecordEvaluation(Class<? extends MatchRecordEvaluation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.participant = inits.isInitialized("participant") ? new QMatchRecordParticipant(forProperty("participant"), inits.get("participant")) : null;
    }

}

