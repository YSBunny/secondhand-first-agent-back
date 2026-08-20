package com.hackathon.second_hand_first.search.exception;

public class SearchSessionNotFoundException extends RuntimeException {

    public SearchSessionNotFoundException() {
        super("요청한 리소스를 찾을 수 없습니다.");
    }
}
