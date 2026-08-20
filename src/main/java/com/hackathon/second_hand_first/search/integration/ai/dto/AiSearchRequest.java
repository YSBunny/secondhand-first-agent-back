package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * AI 검색 그래프 요청.
 *
 * <p>그래프 진입점이 하나뿐이라, 질의 분석 노드가 직접 쓰지 않는 값도 여기서 함께 넘긴다.
 * 각 노드가 필요한 것만 꺼내 쓰는 봉투 구조다.
 *
 * <p><b>{@code rawQuery}는 가공하지 않은 사용자 원문이어야 한다.</b> 백엔드가 미리
 * 키워드를 뽑거나 예산을 파싱해서 넘기면 구조화 규칙이 두 곳으로 갈라져 어느 쪽이
 * 틀렸는지 추적할 수 없게 된다. 구조화는 AI의 parse_query가 독점한다.
 *
 * <p>계약 정의: ai/docs/백엔드_연동_계약.md 2장
 */
public record AiSearchRequest(
        @JsonProperty("raw_query")
        String rawQuery,
        @JsonProperty("request_id")
        String requestId,
        @JsonProperty("session_id")
        String sessionId,
        // 후속 질문일 때 직전에 확정된 검색 조건. "더 싼 거 없어?"처럼 상품명이 빠진
        // 요청을 해석하는 데 쓴다. 신규 검색이면 null.
        @JsonProperty("previous_query_parsed")
        Map<String, Object> previousQueryParsed,
        @JsonProperty("user_context")
        AiUserContext userContext
) {
}
