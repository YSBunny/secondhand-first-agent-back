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
            String y
    ) {
    }
}