package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.location.dto.response.ProductLocationGeocodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLocationGeocodeService {

    private final KakaoLocalService kakaoLocalService;

    public ProductLocationGeocodeResponse geocode(
            ProductLocationGeocodeRequest request
    ) {
        if (request.precision() == ProductLocationGeocodeRequest.Precision.NONE) {
            return new ProductLocationGeocodeResponse(
                    request.name(), request.fullAddress(), request.precision(),
                    clearRegionCoordinates(request.regions()), null
            );
        }

        Map<String, GeographicCoordinates> cache = new HashMap<>();
        GeographicCoordinates coordinates = geocode(request.fullAddress(), cache);
        List<ProductLocationGeocodeRequest.Region> regions = request.regions()
                .stream()
                .map(region -> geocodeRegion(region, cache))
                .toList();

        return new ProductLocationGeocodeResponse(
                request.name(),
                request.fullAddress(),
                request.precision(),
                regions,
                coordinates
        );
    }

    private ProductLocationGeocodeRequest.Region geocodeRegion(
            ProductLocationGeocodeRequest.Region region,
            Map<String, GeographicCoordinates> cache
    ) {
        if (region == null) return null;
        GeographicCoordinates coordinates = region.coordinates() == null
                ? geocode(region.fullAddress(), cache)
                : region.coordinates();
        return new ProductLocationGeocodeRequest.Region(
                region.name(), region.fullAddress(), region.code(), coordinates
        );
    }

    private GeographicCoordinates geocode(
            String address,
            Map<String, GeographicCoordinates> cache
    ) {
        String normalized = normalize(address);
        if (normalized == null) return null;
        if (cache.containsKey(normalized)) return cache.get(normalized);
        GeographicCoordinates result = kakaoLocalService
                .findCoordinates(normalized)
                .orElse(null);
        cache.put(normalized, result);
        return result;
    }

    private List<ProductLocationGeocodeRequest.Region> clearRegionCoordinates(
            List<ProductLocationGeocodeRequest.Region> regions
    ) {
        return regions.stream()
                .map(region -> region == null ? null
                        : new ProductLocationGeocodeRequest.Region(
                                region.name(), region.fullAddress(),
                                region.code(), null
                        ))
                .toList();
    }

    private String normalize(String address) {
        if (address == null || address.isBlank()) return null;
        return address.trim().replaceAll("\\s+", " ");
    }
}
