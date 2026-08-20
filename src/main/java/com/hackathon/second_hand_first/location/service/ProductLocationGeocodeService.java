package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.location.dto.response.ProductLocationGeocodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductLocationGeocodeService {

    private final KakaoLocalService kakaoLocalService;

    public ProductLocationGeocodeResponse geocode(
            ProductLocationGeocodeRequest request
    ) {
        GeographicCoordinates coordinates = null;

        if (request.precision() != ProductLocationGeocodeRequest.Precision.NONE
                && request.fullAddress() != null
                && !request.fullAddress().isBlank()) {
            coordinates = kakaoLocalService
                    .findCoordinates(request.fullAddress())
                    .orElse(null);
        }

        return new ProductLocationGeocodeResponse(
                request.name(),
                request.fullAddress(),
                request.precision(),
                request.regions(),
                coordinates
        );
    }
}
