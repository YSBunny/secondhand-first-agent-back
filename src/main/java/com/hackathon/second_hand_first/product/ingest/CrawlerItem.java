package com.hackathon.second_hand_first.product.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;

import java.util.List;

/**
 * 통합 스키마의 상품 한 건.
 *
 * <p>필드 이름은 크롤러 출력(snake_case) 그대로 받는다. 이름을 바꾸면 스키마 문서와
 * 대조하기 어려워진다.
 *
 * <p>{@code location} 은 {@link AiLocationResponse} 를 그대로 쓴다. AI 가 보내는 위치와
 * 크롤러가 만드는 위치가 같은 형태이므로, 별도 타입을 두면 같은 구조가 두 벌이 된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlerItem(
        String platform,

        @JsonProperty("platform_product_id")
        String platformProductId,

        String url,
        String title,
        String description,
        Long price,

        @JsonProperty("condition_level")
        String conditionLevel,

        @JsonProperty("trade_method")
        List<String> tradeMethod,

        @JsonProperty("delivery_fee")
        CrawlerDeliveryFee deliveryFee,

        List<String> images,

        AiLocationResponse location
) {
    public List<String> tradeMethodOrEmpty() {
        return tradeMethod == null ? List.of() : tradeMethod;
    }

    public List<String> imagesOrEmpty() {
        return images == null ? List.of() : images;
    }
}
