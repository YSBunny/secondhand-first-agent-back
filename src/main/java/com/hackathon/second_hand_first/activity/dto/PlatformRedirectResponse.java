package com.hackathon.second_hand_first.activity.dto;

import com.hackathon.second_hand_first.activity.domain.PlatformRedirectHistory;
import com.hackathon.second_hand_first.product.domain.Platform;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PlatformRedirectResponse(
        Platform platform,
        String redirectUrl,
        OffsetDateTime redirectedAt
) {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static PlatformRedirectResponse from(PlatformRedirectHistory history) {
        return new PlatformRedirectResponse(
                history.getPlatform(),
                history.getRedirectUrl(),
                history.getRedirectedAt().atZone(SEOUL).toOffsetDateTime()
        );
    }
}
