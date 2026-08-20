package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiParsedConditionsResponse;

import java.util.List;

public record ParsedConditionsResponse(
        String keyword,
        Long maxPrice,
        List<ProductCondition> condition,
        SearchPriority priority
) {
    public static ParsedConditionsResponse from(AiParsedConditionsResponse analysis) {
        return new ParsedConditionsResponse(
                analysis.keyword(),
                analysis.maxPrice(),
                analysis.conditions(),
                analysis.priority()
        );
    }

    public static ParsedConditionsResponse from(com.hackathon.second_hand_first.search.domain.SearchSession session) {
        return new ParsedConditionsResponse(
                session.getKeyword(),
                session.getMaxPrice(),
                session.getConditions().stream().map(condition -> condition.getCondition()).toList(),
                session.getPriority()
        );
    }
}
