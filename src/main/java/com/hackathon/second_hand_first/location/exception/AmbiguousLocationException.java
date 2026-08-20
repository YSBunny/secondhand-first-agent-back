package com.hackathon.second_hand_first.location.exception;

public class AmbiguousLocationException extends RuntimeException {

    public AmbiguousLocationException(String message) {
        super(message);
    }
}