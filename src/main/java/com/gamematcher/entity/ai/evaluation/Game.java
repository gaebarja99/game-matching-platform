package com.gamematcher.entity.ai.evaluation;

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

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "stats_schema", length = 2000)
    private String statsSchema;

    public Game(String name, String code, String statsSchema) {
        this.name = name;
        this.code = code;
        this.statsSchema = statsSchema;
    }
}
