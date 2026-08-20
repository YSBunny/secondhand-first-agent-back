package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;

public record SearchSessionCreateResponse(
        String sessionId,
        SearchSessionStatus status,
        ParsedConditionsResponse parsedConditions,
        String assistantMessage,
        int resultCount
) {
    public static SearchSessionCreateResponse of(
            SearchSession session,
            AiSearchResponse aiResponse
    ) {
        return new SearchSessionCreateResponse(
                session.getSessionId(),
                session.getStatus(),
                ParsedConditionsResponse.from(aiResponse.parsedConditions()),
                aiResponse.assistantMessage(),
                aiResponse.resultCount()
        );
    }
}
