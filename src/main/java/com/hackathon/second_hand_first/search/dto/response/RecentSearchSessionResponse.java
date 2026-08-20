package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.search.domain.SearchSession;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RecentSearchSessionResponse(
        String sessionId,
        String keyword,
        String querySummary,
        String lastMessage,
        int resultCount,
        OffsetDateTime updatedAt
) {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static RecentSearchSessionResponse from(SearchSession session) {
        return new RecentSearchSessionResponse(
                session.getSessionId(),
                session.getKeyword(),
                session.getQuerySummary(),
                session.getLastMessage(),
                session.getResultCount(),
                session.getUpdatedAt().atZone(SEOUL).toOffsetDateTime()
        );
    }
}
