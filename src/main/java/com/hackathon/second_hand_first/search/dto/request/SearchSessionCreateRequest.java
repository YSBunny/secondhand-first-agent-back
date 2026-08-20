package com.hackathon.second_hand_first.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchSessionCreateRequest(
        @NotBlank(message = "요청 값이 올바르지 않습니다.")
        @Size(max = 1_000, message = "요청 값이 올바르지 않습니다.")
        String query
) {
}
