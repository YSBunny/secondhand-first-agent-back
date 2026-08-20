package com.hackathon.second_hand_first.product.dto.response;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;

public record BestDealItemResponse(
        String productId,
        int rank,
        Platform platform,
        ProductCategory category,
        String title,
        long price,
        long officialPrice,
        long savingsAmount,
        int savingsRate,
        ProductCondition condition,
        String location,
        String recommendationReason,
        int recommendationScore,
        String imageUrl
) {
}
