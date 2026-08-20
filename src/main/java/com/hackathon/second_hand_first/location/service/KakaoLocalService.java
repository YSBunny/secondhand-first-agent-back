package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.location.dto.response.KakaoAddressResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KakaoLocalService {

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalService(
            RestClient.Builder restClientBuilder,
            @Value("${kakao.local.base-url}") String baseUrl,
            @Value("${kakao.local.rest-api-key}") String restApiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.restApiKey = restApiKey;
    }

    public CoordinateResponse getCoordinate(String address) {
        KakaoAddressResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .queryParam("size", 1)
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .body(KakaoAddressResponse.class);

        if (response == null || response.documents().isEmpty()) {
            throw new IllegalArgumentException(
                    "입력한 주소의 좌표를 찾을 수 없습니다: " + address
            );
        }

        KakaoAddressResponse.Document document =
                response.documents().getFirst();

        return new CoordinateResponse(
                document.addressName(),
                Double.parseDouble(document.y()), // 위도
                Double.parseDouble(document.x())  // 경도
        );
    }
}