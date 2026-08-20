package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;

import java.util.List;

public record SearchSessionCreateResponse(
        String sessionId,
        SearchSessionStatus status,
        ParsedConditionsResponse parsedConditions,
        String assistantMessage,
        int resultCount,
        List<SearchResultItemResponse> recommendations
) {
    public static SearchSessionCreateResponse of(
            SearchSession session,
            AiSearchResponse aiResponse,
            List<SearchResultItemResponse> recommendations
    ) {
        return new SearchSessionCreateResponse(
                session.getSessionId(),
                session.getStatus(),
                ParsedConditionsResponse.from(aiResponse.parsedConditions()),
                aiResponse.assistantMessage(),
                aiResponse.resultCount(),
                recommendations
        );
    }
}
