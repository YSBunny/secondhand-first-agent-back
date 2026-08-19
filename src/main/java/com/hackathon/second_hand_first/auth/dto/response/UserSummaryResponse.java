package com.hackathon.second_hand_first.auth.dto.response;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        String profileImageUrl
) {
}
