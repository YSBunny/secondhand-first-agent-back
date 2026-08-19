package com.hackathon.second_hand_first.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {

    public record UserInfo(
            Long id,
            String name,
            String email,
            String profileImageUrl
    ) {
    }
}