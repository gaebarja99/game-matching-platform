package com.gamematcher.entity;

import com.gamematcher.constant.GameList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private GameList code;

    @Column(name = "stats_schema", length = 2000)
    private String statsSchema;

    public Game(String name, GameList code, String statsSchema) {
        this.name = name;
        this.code = code;
        this.statsSchema = statsSchema;
    }
}
