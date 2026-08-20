package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.product.domain.DeliveryPayer;

/**
 * AI가 넘겨주는 배송비. 크롤러 통합 스키마의 {@code delivery_fee} 그대로다.
 *
 * <p>정의 원본은 data-analysis/docs/통합_스키마_정의.md 4장이다.
 * options 와 raw 는 백엔드가 쓰지 않아 받지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDeliveryFeeResponse(
        /** AVAILABLE 이면 택배로 받을 수 있다. NOT_AVAILABLE 은 판매자가 택배를 받지 않는다는 뜻이다. */
        String status,
        DeliveryPayer payer,
        @JsonProperty("min_fee")
        Long minFee,
        @JsonProperty("home_delivery_fee")
        Long homeDeliveryFee
) {
}
