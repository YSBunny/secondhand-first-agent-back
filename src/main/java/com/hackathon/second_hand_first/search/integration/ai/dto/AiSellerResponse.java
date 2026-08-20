package com.hackathon.second_hand_first.search.integration.ai.dto;

public record AiSellerResponse(
        String externalSellerId,
        String name,
        Integer trustScore,
        Integer tradeCount,
        Double mannerTemperature
) {
}
