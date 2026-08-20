package com.hackathon.second_hand_first.product.dto.request;

import jakarta.validation.constraints.NotEmpty;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record ProductDistanceRankingRequest(
        @NotEmpty(message = "상품 목록은 비어 있을 수 없습니다.")
        List<JsonNode> products
) {
}
