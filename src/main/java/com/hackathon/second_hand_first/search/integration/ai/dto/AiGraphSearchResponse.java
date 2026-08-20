package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI 서버 POST /internal/search 의 응답 원본.
 *
 * <p>백엔드가 프론트에 내보내는 AiSearchResponse 와 형태가 다르다.
 * 변환은 AiSearchResponseMapper 가 한다.
 *
 * <p>계약 정의: ai/docs/백엔드_연동_계약.md 4장
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGraphSearchResponse(
        @JsonProperty("request_id")
        String requestId,
        @JsonProperty("query_parsed")
        AiGraphQueryParsed queryParsed,
        List<AiGraphItem> items,
        @JsonProperty("top_recommendation_ids")
        List<String> topRecommendationIds
) {
}
