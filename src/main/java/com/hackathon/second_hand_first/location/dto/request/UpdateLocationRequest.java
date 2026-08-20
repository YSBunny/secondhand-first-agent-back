package com.hackathon.second_hand_first.location.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLocationRequest(
        @NotBlank(message = "지역은 필수입니다.")
        @Size(max = 100, message = "지역은 100자 이하여야 합니다.")
        String region
) {
    public UpdateLocationRequest {
        if (region != null) {
            region = region.trim().replaceAll("\\s+", " ");
        }
    }
}
