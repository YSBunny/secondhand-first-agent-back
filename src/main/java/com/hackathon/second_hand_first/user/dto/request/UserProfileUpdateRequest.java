package com.hackathon.second_hand_first.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,
        String profileImageUrl
) {
}
