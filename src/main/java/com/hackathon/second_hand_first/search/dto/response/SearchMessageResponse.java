package com.hackathon.second_hand_first.search.dto.response;

import com.hackathon.second_hand_first.search.domain.SearchMessage;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record SearchMessageResponse(
        String id,
        String content,
        OffsetDateTime createdAt
) {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static SearchMessageResponse from(SearchMessage message) {
        return new SearchMessageResponse(
                message.getMessageId(),
                message.getContent(),
                message.getCreatedAt().atZone(SEOUL).toOffsetDateTime()
        );
    }
}
