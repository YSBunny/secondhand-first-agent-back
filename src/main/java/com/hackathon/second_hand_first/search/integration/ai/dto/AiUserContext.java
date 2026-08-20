package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 추천 계산에 필요한 사용자 사정.
 *
 * <p>모르는 값은 채우지 않는다. {@code canUseConveniencePickup}을 기본 true로 두면
 * 편의점 반값택배를 포함한 최저 배송비가 쓰이는데, 주변에 그 편의점이 없는 사용자에게는
 * 존재하지 않는 가격이다. null이면 AI 쪽에서 보수적으로 계산한다.
 *
 * <p>계약 정의: ai/docs/백엔드_연동_계약.md 2장
 */
public record AiUserContext(
        @JsonProperty("location")
        AiUserLocation location,
        @JsonProperty("can_use_convenience_pickup")
        Boolean canUseConveniencePickup
) {
}
