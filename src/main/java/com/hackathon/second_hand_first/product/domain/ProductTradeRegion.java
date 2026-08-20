package com.hackathon.second_hand_first.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductTradeRegion {

    @Column(name = "region_name", length = 100)
    private String name;

    @Column(name = "full_address", length = 100)
    private String fullAddress;

    @Column(name = "region_code", length = 30)
    private String code;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    private ProductTradeRegion(
            String name, String fullAddress, String code,
            Double latitude, Double longitude
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "거래 지역의 위도와 경도는 함께 존재하거나 함께 없어야 합니다."
            );
        }
        this.name = normalize(name);
        this.fullAddress = normalize(fullAddress);
        this.code = normalize(code);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static ProductTradeRegion create(
            String name, String fullAddress, String code,
            Double latitude, Double longitude
    ) {
        return new ProductTradeRegion(name, fullAddress, code, latitude, longitude);
    }

    ProductTradeRegion withCoordinates(Double latitude, Double longitude) {
        return create(name, fullAddress, code, latitude, longitude);
    }

    boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ");
    }
}
