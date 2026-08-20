package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCondition;

import java.util.List;

/**
 * AI가 돌려주는 상품 한 건. 크롤러 통합 스키마 그대로다.
 *
 * <p>필드 정의 원본은 data-analysis/docs/통합_스키마_정의.md 이며,
 * 여기에는 백엔드가 실제로 쓰는 것만 담는다. 나머지는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGraphItem(
        Platform platform,
        @JsonProperty("platform_product_id")
        String platformProductId,
        String url,
        String title,
        Long price,
        String description,
        List<String> images,
        @JsonProperty("condition_level")
        ProductCondition conditionLevel,
        @JsonProperty("trade_method")
        List<String> tradeMethod,
        @JsonProperty("delivery_fee")
        AiDeliveryFeeResponse deliveryFee,
        AiGraphLocation location,
        // rerank가 붙이는 값. 상위 4건에만 reasoning이 있다.
        @JsonProperty("_score_breakdown")
        AiGraphScoreBreakdown scoreBreakdown,
        String reasoning
) {

    /** 통합 스키마의 item_id. 없으면 platform:platform_product_id 로 만든다. */
    @JsonProperty("item_id")
    public String itemId() {
        return platform + ":" + platformProductId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiGraphLocation(
            String name,
            @JsonProperty("full_address")
            String fullAddress,
            String precision,
            List<AiGraphRegion> regions
    ) {
    }

    /** 거래 가능 지역. N플리마켓은 최대 3곳까지 온다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiGraphRegion(
            String name,
            @JsonProperty("full_address")
            String fullAddress,
            String code
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiGraphScoreBreakdown(
            @JsonProperty("best_deal_score")
            Double bestDealScore
    ) {
    }
}
