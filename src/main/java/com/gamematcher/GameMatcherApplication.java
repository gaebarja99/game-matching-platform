package com.gamematcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameMatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameMatcherApplication.class, args);
    }
}
