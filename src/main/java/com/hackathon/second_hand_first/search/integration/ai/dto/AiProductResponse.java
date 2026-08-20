package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI가 수집한 외부 매물을 백엔드 Product로 변환하기 위한 임시 계약입니다.
 */
public record AiProductResponse(
        Platform platform,
        String externalProductId,
        String title,
        String description,
        ProductCategory category,
        Long price,
        Long referencePrice,
        ProductCondition condition,
        ProductStatus status,
        AiLocationResponse location,
        Boolean directTradeAvailable,
        Boolean shippingAvailable,
        AiDeliveryFeeResponse deliveryFee,
        Boolean carbonReductionEligible,
        String platformUrl,
        Long externalViewCount,
        OffsetDateTime publishedAt,
        List<String> imageUrls,
        AiSellerResponse seller
) {
    public AiProductResponse withLocation(AiLocationResponse newLocation) {
        return new AiProductResponse(
                platform,
                externalProductId,
                title,
                description,
                category,
                price,
                referencePrice,
                condition,
                status,
                newLocation,
                directTradeAvailable,
                shippingAvailable,
                deliveryFee,
                carbonReductionEligible,
                platformUrl,
                externalViewCount,
                publishedAt,
                imageUrls,
                seller
        );
    }
}
