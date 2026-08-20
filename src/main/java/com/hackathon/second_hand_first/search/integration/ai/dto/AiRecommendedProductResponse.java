package com.hackathon.second_hand_first.search.integration.ai.dto;

public record AiRecommendedProductResponse(
        int rank,
        Double recommendationScore,
        String recommendationReason,
        AiProductResponse product
) {
}
