package com.hackathon.second_hand_first.search.integration.ai.dto;

public record AiScoreBreakdownResponse(
        Double priceScore,
        Double qualityScore,
        Double convenienceScore
) {
}
