package com.hackathon.second_hand_first.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,
        @Size(max = 1000, message = "프로필 이미지 URL은 1000자 이하여야 합니다.")
        String profileImageUrl
) {
}
