package com.gamematcher.entity.match.valorant;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QValorantRoundPlayerDamageEvent is a Querydsl query type for ValorantRoundPlayerDamageEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QValorantRoundPlayerDamageEvent extends EntityPathBase<ValorantRoundPlayerDamageEvent> {

    private static final long serialVersionUID = 1092286257L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValorantRoundPlayerDamageEvent valorantRoundPlayerDamageEvent = new QValorantRoundPlayerDamageEvent("valorantRoundPlayerDamageEvent");

    public final NumberPath<Integer> bodyshots = createNumber("bodyshots", Integer.class);

    public final NumberPath<Integer> damage = createNumber("damage", Integer.class);

    public final NumberPath<Integer> headshots = createNumber("headshots", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> legshots = createNumber("legshots", Integer.class);

    public final StringPath receiverDisplayName = createString("receiverDisplayName");

    public final StringPath receiverPuuid = createString("receiverPuuid");

    public final StringPath receiverTeam = createString("receiverTeam");

    public final QValorantMatchRoundPlayer roundPlayer;

    public QValorantRoundPlayerDamageEvent(String variable) {
        this(ValorantRoundPlayerDamageEvent.class, forVariable(variable), INITS);
    }

    public QValorantRoundPlayerDamageEvent(Path<? extends ValorantRoundPlayerDamageEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QValorantRoundPlayerDamageEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QValorantRoundPlayerDamageEvent(PathMetadata metadata, PathInits inits) {
        this(ValorantRoundPlayerDamageEvent.class, metadata, inits);
    }

    public QValorantRoundPlayerDamageEvent(Class<? extends ValorantRoundPlayerDamageEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.roundPlayer = inits.isInitialized("roundPlayer") ? new QValorantMatchRoundPlayer(forProperty("roundPlayer"), inits.get("roundPlayer")) : null;
    }

}

