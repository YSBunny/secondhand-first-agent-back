package com.hackathon.second_hand_first.activity.exception;

public class RedirectForbiddenException extends RuntimeException {

    public RedirectForbiddenException() {
        super("접근 권한이 없습니다.");
    }
}
