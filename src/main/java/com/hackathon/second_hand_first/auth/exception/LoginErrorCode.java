package com.hackathon.second_hand_first.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LoginErrorCode {

    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "이메일 또는 비밀번호가 일치하지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    LoginErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}