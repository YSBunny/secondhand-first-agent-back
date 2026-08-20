package com.hackathon.second_hand_first.location.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateLocationRequest(
        @NotBlank(message = "지역은 필수입니다.")
        String address
) {
}