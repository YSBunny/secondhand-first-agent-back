package com.hackathon.second_hand_first.auth.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}