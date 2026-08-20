package com.hackathon.second_hand_first.search.integration.ai.dto;

/**
 * AI 팀과 계약 확정 전 사용하는 임시 요청 형식입니다.
 */
public record AiSearchRequest(
        String sessionId,
        String query
) {
}
