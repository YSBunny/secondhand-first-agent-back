package com.hackathon.second_hand_first.location.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductLocationGeocodeRequest(
        String name,

        @JsonProperty("full_address")
        @Size(max = 100, message = "전체 주소는 100자 이하여야 합니다.")
        String fullAddress,

        @NotNull(message = "위치 정밀도는 필수입니다.")
        Precision precision,

        @NotNull(message = "거래 가능 지역 목록은 필수입니다.")
        List<@Valid Region> regions,

        GeographicCoordinates coordinates
) {
    public enum Precision {
        FULL,
        DONG_ONLY,
        NONE
    }

    public record Region(
            String name,

            @JsonProperty("full_address")
            String fullAddress,

            String code,

            GeographicCoordinates coordinates
    ) {
    }
}
