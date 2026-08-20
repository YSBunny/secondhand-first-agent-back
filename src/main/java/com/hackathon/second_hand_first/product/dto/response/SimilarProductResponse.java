package com.hackathon.second_hand_first.product.dto.response;

import com.hackathon.second_hand_first.product.domain.Platform;

import java.util.List;

public record SimilarProductResponse(List<SimilarProductItem> products) {

    public record SimilarProductItem(
            String productId,
            int rank,
            Platform platform,
            String title,
            long price,
            String imageUrl,
            int recommendationScore
    ) {
    }
}