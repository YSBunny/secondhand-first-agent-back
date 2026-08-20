package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.location.dto.response.KakaoAddressResponse;
import com.hackathon.second_hand_first.location.exception.KakaoLocalException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KakaoLocalService {

    private final RestClient restClient;

    public KakaoLocalService(RestClient kakaoRestClient) {
        this.restClient = kakaoRestClient;
    }

    public CoordinateResponse getCoordinate(String address) {
        try {
            KakaoAddressResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .body(KakaoAddressResponse.class);

            return convertResponse(response);

        } catch (RestClientResponseException exception) {
            throw convertHttpException(exception);

        } catch (ResourceAccessException exception) {
            // 연결 실패, 읽기 시간 초과 등
            throw new KakaoLocalException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "주소 검색 서비스의 응답이 지연되고 있습니다."
            );
        }
    }

    private CoordinateResponse convertResponse(KakaoAddressResponse response) {
        if (response == null ||
                response.documents() == null ||
                response.documents().isEmpty()) {
            throw new IllegalArgumentException(
                    "입력한 주소의 좌표를 찾을 수 없습니다."
            );
        }

        KakaoAddressResponse.Document document =
                response.documents().getFirst();

        if (document == null ||
                document.addressName() == null ||
                document.x() == null ||
                document.y() == null) {
            throw new KakaoLocalException(
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스에서 올바르지 않은 응답을 받았습니다."
            );
        }

        try {
            return new CoordinateResponse(
                    document.addressName(),
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