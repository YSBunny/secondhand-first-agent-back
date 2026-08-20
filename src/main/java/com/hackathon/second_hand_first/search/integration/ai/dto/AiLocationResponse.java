package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;

import java.util.List;

public record AiLocationResponse(
        @JsonAlias("displayName")
        String name,

        @JsonProperty("full_address")
        String fullAddress,

        ProductLocationGeocodeRequest.Precision precision,

        List<AiRegionResponse> regions,

        GeographicCoordinates coordinates,

        Double latitude,

        Double longitude
) {
    /** 통합 스키마 기반 위치 DTO를 사용하는 기존 내부 코드와의 호환 생성자. */
    public AiLocationResponse(
            String name,
            String fullAddress,
            ProductLocationGeocodeRequest.Precision precision,
            List<AiRegionResponse> regions,
            GeographicCoordinates coordinates
    ) {
        this(name, fullAddress, precision, regions, coordinates, null, null);
    }

    /**
     * 최종 AI 계약은 위도와 경도를 location 바로 아래에 둔다.
     * 기존 지오코딩 흐름은 coordinates 객체를 사용하므로 여기서 호환한다.
     */
    @Override
    public GeographicCoordinates coordinates() {
        if (coordinates != null) {
            return coordinates;
        }
        if (latitude == null || longitude == null) {
            return null;
        }
        return new GeographicCoordinates(latitude, longitude);
    }

    /** 최종 계약에 전체 주소가 없으면 표시 지역명을 저장용 위치로 사용한다. */
    @Override
    public String fullAddress() {
        return fullAddress == null || fullAddress.isBlank() ? name : fullAddress;
    }
}
