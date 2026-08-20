package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.carbon.dto.CarbonSavingResult;
import com.hackathon.second_hand_first.product.domain.Platform;

public record SearchResultItemResponse(
        String productId,
        int rank,
        Platform platform,
        String title,
        long price,
        String imageUrl,
        Double recommendationScore,
        String recommendationReason,
        CarbonSavingResult carbonSaving
) {
}