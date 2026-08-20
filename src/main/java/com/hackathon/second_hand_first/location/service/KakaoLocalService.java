package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.location.dto.response.KakaoAddressResponse;
import com.hackathon.second_hand_first.location.dto.response.LocationCandidateResponse;
import com.hackathon.second_hand_first.location.exception.KakaoLocalException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KakaoLocalService {

    private static final int CANDIDATE_SEARCH_SIZE = 10;
    private static final int MAX_QUERY_LENGTH = 100;

    private final RestClient restClient;

    public KakaoLocalService(RestClient kakaoRestClient) {
        this.restClient = kakaoRestClient;
    }

    public List<LocationCandidateResponse> searchCandidates(String query) {
        String normalizedQuery = normalizeRequiredValue(query, "검색어는 필수입니다.");
        validateMaxLength(normalizedQuery, MAX_QUERY_LENGTH, "검색어는 100자 이하여야 합니다.");
        KakaoAddressResponse response =
                requestAddressSearch(normalizedQuery, CANDIDATE_SEARCH_SIZE);

        if (response == null ||
                response.documents() == null ||
                response.documents().isEmpty()) {
            return List.of();
        }

        return response.documents().stream()
                .filter(this::hasValidRegion)
                .map(this::toCandidate)
                .toList();
    }

    public CoordinateResponse resolveRegionCoordinates(String region) {
        String requestedRegion = normalizeRequiredValue(region, "지역은 필수입니다.");
        validateMaxLength(requestedRegion, MAX_QUERY_LENGTH, "지역은 100자 이하여야 합니다.");
        KakaoAddressResponse response =
                requestAddressSearch(requestedRegion, CANDIDATE_SEARCH_SIZE);

        if (response == null ||
                response.documents() == null ||
                response.documents().isEmpty()) {
            throw new IllegalArgumentException("입력한 지역의 좌표를 찾을 수 없습니다.");
        }

        List<LocationCandidateResponse> matches = response.documents().stream()
                .filter(this::hasValidRegion)
                .map(this::toCandidate)
                .filter(candidate -> matchesRequestedRegion(requestedRegion, candidate))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "입력한 지역과 정확히 일치하는 검색 결과가 없습니다. 시·도, 시·군·구, 읍·면·동을 확인해 주세요."
            );
        }

        Map<String, LocationCandidateResponse> matchesByRegionCode = matches.stream()
                .collect(Collectors.toMap(
                        LocationCandidateResponse::regionCode,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        if (matchesByRegionCode.size() > 1) {
            throw new IllegalArgumentException(
                    "동일한 이름의 지역이 여러 개입니다. 시·도와 시·군·구를 포함해 입력해 주세요."
            );
        }

        LocationCandidateResponse candidate = matchesByRegionCode.values().iterator().next();
        return new CoordinateResponse(
                candidate.region(),
                candidate.latitude(),
                candidate.longitude()
        );
    }

    private KakaoAddressResponse requestAddressSearch(String query, int size) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", query)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .body(KakaoAddressResponse.class);
        } catch (RestClientResponseException exception) {
            throw convertHttpException(exception);
        } catch (ResourceAccessException exception) {
            throw new KakaoLocalException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "주소 검색 서비스의 응답이 지연되고 있습니다."
            );
        }
    }

    private boolean hasValidRegion(KakaoAddressResponse.Document document) {
        return document != null
                && document.addressName() != null
                && !document.addressName().isBlank()
                && document.x() != null
                && document.y() != null
                && document.address() != null
                && document.address().regionCode() != null
                && !document.address().regionCode().isBlank();
    }

    private LocationCandidateResponse toCandidate(
            KakaoAddressResponse.Document document
    ) {
        KakaoAddressResponse.Address address = document.address();
        String region3DepthName = firstNonBlank(
                address.administrativeRegion3DepthName(),
                address.region3DepthName()
        );
        String region = Stream.of(
                        address.region1DepthName(),
                        address.region2DepthName(),
                        region3DepthName
                )
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElseThrow(() -> new KakaoLocalException(
                        HttpStatus.BAD_GATEWAY,
                        "주소 검색 서비스에서 올바르지 않은 지역 정보를 받았습니다."
                ));

        try {
            return new LocationCandidateResponse(
                    document.addressName().trim(),
                    address.regionCode().trim(),
                    region,
                    Double.parseDouble(document.y()),
                    Double.parseDouble(document.x())
            );
        } catch (NumberFormatException exception) {
            throw new KakaoLocalException(
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스에서 올바르지 않은 좌표를 받았습니다."
            );
        }
    }

    private String normalizeRequiredValue(String value, String message) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null || normalizedValue.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean matchesRequestedRegion(
            String requestedRegion,
            LocationCandidateResponse candidate
    ) {
        String normalizedRequest = normalizeRegionForComparison(requestedRegion);
        return normalizedRequest.equals(normalizeRegionForComparison(candidate.region()))
                || normalizedRequest.equals(normalizeRegionForComparison(candidate.address()));
    }

    private String normalizeRegionForComparison(String region) {
        String normalizedRegion = normalize(region);
        if (normalizedRegion == null || normalizedRegion.isBlank()) {
            return normalizedRegion;
        }

        String[] parts = normalizedRegion.split(" ", 2);
        String province = switch (parts[0]) {
            case "서울특별시" -> "서울";
            case "부산광역시" -> "부산";
            case "대구광역시" -> "대구";
            case "인천광역시" -> "인천";
            case "광주광역시" -> "광주";
            case "대전광역시" -> "대전";
            case "울산광역시" -> "울산";
            case "세종특별자치시" -> "세종";
            case "경기도" -> "경기";
            case "강원도", "강원특별자치도" -> "강원";
            case "충청북도" -> "충북";
            case "충청남도" -> "충남";
            case "전라북도", "전북특별자치도" -> "전북";
            case "전라남도" -> "전남";
            case "경상북도" -> "경북";
            case "경상남도" -> "경남";
            case "제주특별자치도" -> "제주";
            default -> parts[0];
        };

        return parts.length == 1 ? province : province + " " + parts[1];
    }

    private void validateMaxLength(String value, int maxLength, String message) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private KakaoLocalException convertHttpException(
            RestClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();

        if (status == 401 || status == 403) {
            // 사용자의 인증 문제가 아니라 서버의 카카오 API 키 문제
            return new KakaoLocalException(
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스 인증에 실패했습니다."
            );
        }

        if (status == 429) {
            return new KakaoLocalException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "주소 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }

        if (status >= 500) {
            return new KakaoLocalException(
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스를 일시적으로 사용할 수 없습니다."
            );
        }

        return new KakaoLocalException(
                HttpStatus.BAD_GATEWAY,
                "주소 검색 서비스 요청에 실패했습니다."
        );
    }
}
