package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.Platform;

import java.time.OffsetDateTime;

public record AiMarketReferenceResponse(
        String productName,
        Platform sourcePlatform,
        String sourceName,
        String referenceType,
        Long medianPrice,
        Integer sampleCount,
        OffsetDateTime calculatedAt,
        String sourceUrl
) {
}
