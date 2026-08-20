package com.hackathon.second_hand_first.product.dto.response;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;

import java.time.OffsetDateTime;
import java.util.List;

public record ProductDetailResponse(
        String id,
        Platform platform,
        String platformProductId,
        String title,
        long price,
        long officialPrice,
        long savingsAmount,
        int savingsRate,
        List<String> images,
        String description,
        ProductCategory category,
        ProductCondition condition,
        List<String> tradeTypes,
        String location,
        Double distanceKm,
        Long viewCount,
        SellerInfo seller,
        Integer rank,
        String recommendationReason,
        String externalUrl,
        boolean isFavorite,
        boolean changedSinceLastViewed,
        OffsetDateTime updatedAt
) {
    public record SellerInfo(int tradeCount, double temperature) {
    }
}
