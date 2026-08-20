package com.hackathon.second_hand_first.product.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * 통합 스키마의 {@code delivery_fee}.
 *
 * <p>정의는 data-analysis/docs/통합_스키마_정의.md 4장이다.
 *
 * <p>{@code raw} 는 플랫폼 원본이라 형태가 제각각이다 — 11번가는 과반이 문자열
 * ({@code "무료"}), 중고나라는 배열과 객체가 섞인다. 그래서 {@link JsonNode} 로 받아
 * <b>객체일 때만</b> 안을 들여다본다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlerDeliveryFee(
        String status,
        String payer,

        @JsonProperty("min_fee")
        Long minFee,

        @JsonProperty("home_delivery_fee")
        Long homeDeliveryFee,

        List<Option> options,
        JsonNode raw
) {
    public List<Option> optionsOrEmpty() {
        return options == null ? List.of() : options;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(
            String method,
            String carrier,

            @JsonProperty("requires_pickup_point")
            Boolean requiresPickupPoint,

            Long fee,

            @JsonProperty("remote_fee")
            Long remoteFee,

            @JsonProperty("raw_code")
            String rawCode
    ) {
    }
}
