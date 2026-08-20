package com.hackathon.second_hand_first.location.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAddressResponse(
        List<Document> documents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("address_name")
            String addressName,

            String x,
            String y,

            Address address
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            @JsonProperty("region_1depth_name")
            String region1DepthName,

            @JsonProperty("region_2depth_name")
            String region2DepthName,

            @JsonProperty("region_3depth_name")
            String region3DepthName,

            @JsonProperty("region_3depth_h_name")
            String administrativeRegion3DepthName,

            @JsonProperty("h_code")
            String regionCode
    ) {
    }
}
