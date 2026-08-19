package com.hackathon.second_hand_first.auth.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final LoginErrorCode errorCode;

    public UnauthorizedException(LoginErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UnauthorizedException(String message) {
        super(message);
        this.errorCode = LoginErrorCode.INVALID_CREDENTIALS;
    }
}
