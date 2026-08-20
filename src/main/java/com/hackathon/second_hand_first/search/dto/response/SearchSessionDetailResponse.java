package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.search.domain.SearchMessage;
import com.hackathon.second_hand_first.search.domain.SearchSession;

import java.util.List;

public record SearchSessionDetailResponse(
        String sessionId,
        String originalQuery,
        ParsedConditionsResponse parsedConditions,
        List<SearchMessageResponse> messages
) {
    public static SearchSessionDetailResponse of(
            SearchSession session,
            List<SearchMessage> messages
    ) {
        return new SearchSessionDetailResponse(
                session.getSessionId(),
                session.getOriginalQuery(),
                ParsedConditionsResponse.from(session),
                messages.stream().map(SearchMessageResponse::from).toList()
        );
    }
}
