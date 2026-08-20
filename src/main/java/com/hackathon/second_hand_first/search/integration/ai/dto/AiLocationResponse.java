package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;

import java.util.List;

public record AiLocationResponse(
        String name,

        @JsonProperty("full_address")
        String fullAddress,

        ProductLocationGeocodeRequest.Precision precision,

        List<AiRegionResponse> regions,

        GeographicCoordinates coordinates
) {
}