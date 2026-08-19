package com.hackathon.second_hand_first.user.dto.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long userId,
        String name,
        String email,
        String profileImageUrl,
        LocalDateTime createdAt
) {
}