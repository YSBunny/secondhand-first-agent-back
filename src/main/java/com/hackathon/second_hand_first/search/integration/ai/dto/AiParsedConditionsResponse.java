package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.search.domain.SearchPriority;

import java.util.List;

public record AiParsedConditionsResponse(
        String keyword,
        Long maxPrice,
        List<ProductCondition> conditions,
        SearchPriority priority,
        String querySummary
) {
}
