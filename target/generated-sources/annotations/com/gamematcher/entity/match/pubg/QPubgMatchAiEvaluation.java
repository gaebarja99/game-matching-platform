package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPubgMatchAiEvaluation is a Querydsl query type for PubgMatchAiEvaluation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgMatchAiEvaluation extends EntityPathBase<PubgMatchAiEvaluation> {

    private static final long serialVersionUID = -262637164L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPubgMatchAiEvaluation pubgMatchAiEvaluation = new QPubgMatchAiEvaluation("pubgMatchAiEvaluation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath detailedComment = createString("detailedComment");

    public final DateTimePath<java.time.LocalDateTime> evaluatedAt = createDateTime("evaluatedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.Grade> grade = createEnum("grade", com.gamematcher.constant.ai.evaluation.Grade.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPubgMatchParticipant pubgMatchParticipant;

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.EvaluationStatus> status = createEnum("status", com.gamematcher.constant.ai.evaluation.EvaluationStatus.class);

    public final StringPath summary = createString("summary");

    public QPubgMatchAiEvaluation(String variable) {
        this(PubgMatchAiEvaluation.class, forVariable(variable), INITS);
    }

    public QPubgMatchAiEvaluation(Path<? extends PubgMatchAiEvaluation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPubgMatchAiEvaluation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPubgMatchAiEvaluation(PathMetadata metadata, PathInits inits) {
        this(PubgMatchAiEvaluation.class, metadata, inits);
    }

    public QPubgMatchAiEvaluation(Class<? extends PubgMatchAiEvaluation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.pubgMatchParticipant = inits.isInitialized("pubgMatchParticipant") ? new QPubgMatchParticipant(forProperty("pubgMatchParticipant"), inits.get("pubgMatchParticipant")) : null;
    }

}

