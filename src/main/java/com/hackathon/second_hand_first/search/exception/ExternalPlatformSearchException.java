package com.hackathon.second_hand_first.search.exception;

public class ExternalPlatformSearchException extends RuntimeException {

    public ExternalPlatformSearchException(String message) {
        super(message);
    }

    public ExternalPlatformSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
