package com.hackathon.second_hand_first.search.integration.ai.dto;

import java.util.List;

/**
 * AI 팀과 계약 확정 전 사용하는 임시 통합 검색 응답입니다.
 */
public record AiSearchResponse(
        String requestId,
        String sessionId,
        AiScoringResponse scoring,
        AiParsedConditionsResponse parsedConditions,
        String assistantMessage,
        AiMarketReferenceResponse marketReference,
        int totalResultCount,
        List<AiRecommendedProductResponse> products
) {
}
