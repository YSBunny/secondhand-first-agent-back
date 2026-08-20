package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI에 넘기는 사용자 위치.
 *
 * <p>프론트는 검색할 때 위치를 보내지 않는다. 로그인 사용자이므로 백엔드가
 * 프로필에서 채운다. 좌표는 이미 보유하고 있으므로 AI 쪽에서 지오코딩을 다시 하지 않는다.
 *
 * <p>계약 정의: ai/docs/백엔드_연동_계약.md 2장
 */
public record AiUserLocation(
        @JsonProperty("region")
        String region,
        @JsonProperty("latitude")
        Double latitude,
        @JsonProperty("longitude")
        Double longitude
) {
}
