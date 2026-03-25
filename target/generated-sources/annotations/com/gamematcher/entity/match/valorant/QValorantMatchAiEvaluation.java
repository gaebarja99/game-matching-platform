package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantMatchAiEvaluation is a Querydsl query type for ValorantMatchAiEvaluation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantMatchAiEvaluation extends EntityPathBase<ValorantMatchAiEvaluation> {

    private static final long serialVersionUID = -404096396L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantMatchAiEvaluation valorantMatchAiEvaluation = new QValorantMatchAiEvaluation("valorantMatchAiEvaluation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath detailedComment = createString("detailedComment");

    public final DateTimePath<java.time.LocalDateTime> evaluatedAt = createDateTime("evaluatedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.Grade> grade = createEnum("grade", com.gamematcher.constant.ai.evaluation.Grade.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final EnumPath<com.gamematcher.constant.ai.evaluation.EvaluationStatus> status = createEnum("status", com.gamematcher.constant.ai.evaluation.EvaluationStatus.class);

    public final StringPath summary = createString("summary");

    public final QValorantMatchPlayer valorantMatchPlayer;

    public QValorantMatchAiEvaluation(String variable) {
        this(ValorantMatchAiEvaluation.class, forVariable(variable), INITS);
    }

    public QValorantMatchAiEvaluation(Path<? extends ValorantMatchAiEvaluation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantMatchAiEvaluation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantMatchAiEvaluation(PathMetadata metadata, PathInits inits) {
        this(ValorantMatchAiEvaluation.class, metadata, inits);
    }

    public QValorantMatchAiEvaluation(Class<? extends ValorantMatchAiEvaluation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.valorantMatchPlayer = inits.isInitialized("valorantMatchPlayer") ? new QValorantMatchPlayer(forProperty("valorantMatchPlayer"), inits.get("valorantMatchPlayer")) : null;
    }

}

