package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;

public record AiRegionResponse(
        String name,

        @JsonProperty("full_address")
        String fullAddress,

        String code,

        GeographicCoordinates coordinates
) {
}