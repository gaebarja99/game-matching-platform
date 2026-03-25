package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantKillAssistant is a Querydsl query type for ValorantKillAssistant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantKillAssistant extends EntityPathBase<ValorantKillAssistant> {

    private static final long serialVersionUID = -784487125L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantKillAssistant valorantKillAssistant = new QValorantKillAssistant("valorantKillAssistant");

    public final StringPath assistantDisplayName = createString("assistantDisplayName");

    public final StringPath assistantPuuid = createString("assistantPuuid");

    public final StringPath assistantTeam = createString("assistantTeam");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QValorantKillEvent killEvent;

    public QValorantKillAssistant(String variable) {
        this(ValorantKillAssistant.class, forVariable(variable), INITS);
    }

    public QValorantKillAssistant(Path<? extends ValorantKillAssistant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantKillAssistant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantKillAssistant(PathMetadata metadata, PathInits inits) {
        this(ValorantKillAssistant.class, metadata, inits);
    }

    public QValorantKillAssistant(Class<? extends ValorantKillAssistant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.killEvent = inits.isInitialized("killEvent") ? new QValorantKillEvent(forProperty("killEvent"), inits.get("killEvent")) : null;
    }

}

