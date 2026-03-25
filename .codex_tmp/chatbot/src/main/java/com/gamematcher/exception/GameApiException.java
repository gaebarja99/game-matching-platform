package com.gamematcher.exception;

import org.springframework.http.HttpStatus;

/**
 * 게임 API 관련 예외
 */
public class GameApiException extends RuntimeException {

    private final HttpStatus status;

    public GameApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
