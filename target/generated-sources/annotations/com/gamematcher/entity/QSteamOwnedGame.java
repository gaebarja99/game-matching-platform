package com.gamematcher.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSteamOwnedGame is a Querydsl query type for SteamOwnedGame
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSteamOwnedGame extends EntityPathBase<SteamOwnedGame> {

    private static final long serialVersionUID = -381866313L;

    public static final QSteamOwnedGame steamOwnedGame = new QSteamOwnedGame("steamOwnedGame");

    public final StringPath appId = createString("appId");

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> playtimeMinutes = createNumber("playtimeMinutes", Integer.class);

    public final StringPath steamId = createString("steamId");

    public QSteamOwnedGame(String variable) {
        super(SteamOwnedGame.class, forVariable(variable));
    }

    public QSteamOwnedGame(Path<? extends SteamOwnedGame> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSteamOwnedGame(PathMetadata metadata) {
        super(SteamOwnedGame.class, metadata);
    }

}

