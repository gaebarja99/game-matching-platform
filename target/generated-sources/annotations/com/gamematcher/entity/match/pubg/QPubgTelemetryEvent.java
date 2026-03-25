package com.gamematcher.entity.match.pubg;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPubgTelemetryEvent is a Querydsl query type for PubgTelemetryEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPubgTelemetryEvent extends EntityPathBase<PubgTelemetryEvent> {

    private static final long serialVersionUID = 806925462L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPubgTelemetryEvent pubgTelemetryEvent = new QPubgTelemetryEvent("pubgTelemetryEvent");

    public final StringPath accountId = createString("accountId");

    public final NumberPath<Integer> eventSequence = createNumber("eventSequence", Integer.class);

    public final DateTimePath<java.time.Instant> eventTimestamp = createDateTime("eventTimestamp", java.time.Instant.class);

    public final StringPath eventType = createString("eventType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isInBlueZone = createBoolean("isInBlueZone");

    public final StringPath itemCategory = createString("itemCategory");

    public final StringPath itemId = createString("itemId");

    public final NumberPath<Double> locationX = createNumber("locationX", Double.class);

    public final NumberPath<Double> locationY = createNumber("locationY", Double.class);

    public final NumberPath<Double> locationZ = createNumber("locationZ", Double.class);

    public final QPubgMatch match;

    public final StringPath payloadJson = createString("payloadJson");

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public QPubgTelemetryEvent(String variable) {
        this(PubgTelemetryEvent.class, forVariable(variable), INITS);
    }

    public QPubgTelemetryEvent(Path<? extends PubgTelemetryEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPubgTelemetryEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPubgTelemetryEvent(PathMetadata metadata, PathInits inits) {
        this(PubgTelemetryEvent.class, metadata, inits);
    }

    public QPubgTelemetryEvent(Class<? extends PubgTelemetryEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QPubgMatch(forProperty("match")) : null;
    }

}

