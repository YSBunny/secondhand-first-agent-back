package com.hackathon.second_hand_first.location.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;

import java.util.List;

public record ProductLocationGeocodeResponse(
        String name,

        @JsonProperty("full_address")
        String fullAddress,

        ProductLocationGeocodeRequest.Precision precision,

        List<ProductLocationGeocodeRequest.Region> regions,

        GeographicCoordinates coordinates
) {
}
