package com.gamematcher.exception;

/** 이미 LoL 매칭 대기열에 참가 중일 때 */
public class AlreadyInMatchingQueueException extends IllegalStateException {
    public AlreadyInMatchingQueueException(String message) {
        super(message);
    }
}
