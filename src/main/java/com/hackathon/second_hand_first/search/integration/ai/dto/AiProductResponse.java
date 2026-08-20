package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.TradeType;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI가 수집·추천한 외부 매물을 백엔드 Product로 변환하는 확정 계약입니다.
 */
public record AiProductResponse(
        Platform platform,
        String externalProductId,
        String title,
        String description,
        ProductCategory category,
        Long price,
        ProductCondition condition,
        ProductStatus status,
        AiLocationResponse location,
        List<TradeType> tradeTypes,
        AiDeliveryFeeResponse deliveryFee,
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
                condition,
                status,
                newLocation,
                tradeTypes,
                deliveryFee,
                platformUrl,
                externalViewCount,
                publishedAt,
                imageUrls,
                seller
        );
    }

    public boolean supports(TradeType tradeType) {
        return tradeTypes != null && tradeTypes.contains(tradeType);
    }
}
