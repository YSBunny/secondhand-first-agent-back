package com.hackathon.second_hand_first.location.exception;

import org.springframework.http.HttpStatus;

public class KakaoLocalException extends RuntimeException {

    private final HttpStatus status;

    public KakaoLocalException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}