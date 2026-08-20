package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductTradeRegion;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRecommendedProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLocationEnrichmentService {

    private final ProductRepository productRepository;
    private final KakaoLocalService kakaoLocalService;

    public AiProductResponse enrich(AiProductResponse source) {
        if (source == null || source.location() == null) {
            return source;
        }

        AiLocationResponse location = source.location();

        if (location.precision()
                == ProductLocationGeocodeRequest.Precision.NONE) {
            return source.withLocation(new AiLocationResponse(
                    location.name(),
                    location.fullAddress(),
                    location.precision(),
                    clearRegionCoordinates(location.regions()),
                    null
            ));
        }

        Map<String, GeographicCoordinates> coordinatesByAddress =
                new HashMap<>();
        rememberSavedRegionCoordinates(source, coordinatesByAddress);
        GeographicCoordinates coordinates =
                resolveCoordinates(source, location, coordinatesByAddress);
        rememberCoordinates(
                coordinatesByAddress,
                location.fullAddress(),
                coordinates
        );
        List<AiRegionResponse> enrichedRegions = enrichRegions(
                location.regions(),
                coordinatesByAddress
        );

        AiLocationResponse enrichedLocation = new AiLocationResponse(
                location.name(),
                location.fullAddress(),
                location.precision(),
                enrichedRegions,
                coordinates
        );

        return source.withLocation(enrichedLocation);
    }

    private GeographicCoordinates resolveCoordinates(
            AiProductResponse source,
            AiLocationResponse location,
            Map<String, GeographicCoordinates> coordinatesByAddress
    ) {
        // AI 응답에 이미 좌표가 있다면 그대로 사용
        if (location.coordinates() != null) {
            return location.coordinates();
        }

        String address = normalizeAddress(location.fullAddress());
        if (address == null) {
            return null;
        }
        if (coordinatesByAddress.containsKey(address)) {
            return coordinatesByAddress.get(address);
        }

        // 같은 상품·같은 주소의 DB 좌표 재사용
        GeographicCoordinates savedCoordinates = productRepository
                .findByPlatformAndExternalProductId(
                        source.platform(),
                        source.externalProductId()
                )
                .filter(product -> hasSameAddress(product, address))
                .filter(this::hasCoordinates)
                .map(product -> new GeographicCoordinates(
                        product.getLatitude(),
                        product.getLongitude()
                ))
                .orElse(null);

        if (savedCoordinates != null) {
            return savedCoordinates;
        }

        // 저장 좌표가 없을 때만 카카오 호출
        return kakaoLocalService
                .findCoordinates(address)
                .orElse(null);
    }

    private void rememberSavedRegionCoordinates(
            AiProductResponse source,
            Map<String, GeographicCoordinates> coordinatesByAddress
    ) {
        productRepository.findByPlatformAndExternalProductId(
                        source.platform(), source.externalProductId()
                )
                .ifPresent(product -> product.getTradeRegions().stream()
                        .filter(region -> region.getFullAddress() != null)
                        .filter(region -> region.getLatitude() != null
                                && region.getLongitude() != null)
                        .forEach(region -> rememberRegionCoordinates(
                                coordinatesByAddress, region
                        )));
    }

    private void rememberRegionCoordinates(
            Map<String, GeographicCoordinates> coordinatesByAddress,
            ProductTradeRegion region
    ) {
        coordinatesByAddress.put(
                normalizeAddress(region.getFullAddress()),
                new GeographicCoordinates(
                        region.getLatitude(), region.getLongitude()
                )
        );
    }

    private List<AiRegionResponse> enrichRegions(
            List<AiRegionResponse> regions,
            Map<String, GeographicCoordinates> coordinatesByAddress
    ) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }

        return regions.stream()
                .map(region -> enrichRegion(region, coordinatesByAddress))
                .toList();
    }

    private AiRegionResponse enrichRegion(
            AiRegionResponse region,
            Map<String, GeographicCoordinates> coordinatesByAddress
    ) {
        if (region == null) {
            return null;
        }

        GeographicCoordinates coordinates = region.coordinates();
        String address = normalizeAddress(region.fullAddress());

        if (coordinates == null && address != null) {
            if (coordinatesByAddress.containsKey(address)) {
                coordinates = coordinatesByAddress.get(address);
            } else {
                coordinates = kakaoLocalService
                        .findCoordinates(address)
                        .orElse(null);
                coordinatesByAddress.put(address, coordinates);
            }
        }

        return new AiRegionResponse(
                region.name(),
                region.fullAddress(),
                region.code(),
                coordinates
        );
    }

    private List<AiRegionResponse> clearRegionCoordinates(
            List<AiRegionResponse> regions
    ) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }

        return regions.stream()
                .map(region -> region == null
                        ? null
                        : new AiRegionResponse(
                                region.name(),
                                region.fullAddress(),
                                region.code(),
                                null
                        ))
                .toList();
    }

    private void rememberCoordinates(
            Map<String, GeographicCoordinates> coordinatesByAddress,
            String address,
            GeographicCoordinates coordinates
    ) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress != null) {
            coordinatesByAddress.put(normalizedAddress, coordinates);
        }
    }

    private boolean hasSameAddress(Product product, String address) {
        String savedAddress = normalizeAddress(product.getLocation());
        return savedAddress != null && savedAddress.equals(address);
    }

    private boolean hasCoordinates(Product product) {
        return product.getLatitude() != null
                && product.getLongitude() != null;
    }

    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        return address.trim().replaceAll("\\s+", " ");
    }

    public List<AiRecommendedProductResponse> enrichRecommendations(
            List<AiRecommendedProductResponse> recommendations
    ) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }

        return recommendations.stream()
                .map(recommendation -> new AiRecommendedProductResponse(
                        recommendation.rank(),
                        recommendation.recommendationScore(),
                        recommendation.recommendationReason(),
                        recommendation.scoreBreakdown(),
                        recommendation.distanceKm(),
                        enrich(recommendation.product())
                ))
                .toList();
    }
}
