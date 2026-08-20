package com.hackathon.second_hand_first.activity.exception;

public class RedirectTargetNotFoundException extends RuntimeException {

    public RedirectTargetNotFoundException() {
        super("요청한 리소스를 찾을 수 없습니다.");
    }
}
