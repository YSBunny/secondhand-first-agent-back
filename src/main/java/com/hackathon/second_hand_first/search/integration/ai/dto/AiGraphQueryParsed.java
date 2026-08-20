package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 그래프가 돌려주는 query_parsed.
 *
 * <p>정의 원본은 ai/docs/노드정의.md 의 parse_query 출력이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGraphQueryParsed(
        String product,
        Long budget,
        String purpose,
        String spec,
        @JsonProperty("used_allowed")
        Boolean usedAllowed,
        // AI가 사용자 요청에서 추론한 카테고리. 확실하지 않으면 null 로 온다.
        // ProductCategory 이름과 같은 문자열이다.
        String category
) {
}
