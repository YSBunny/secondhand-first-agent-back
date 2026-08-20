package com.hackathon.second_hand_first.carbon.infrastructure;

import com.hackathon.second_hand_first.carbon.dto.CarbonSavingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class ClimatiqClient {

    private static final Logger log = LoggerFactory.getLogger(ClimatiqClient.class);
    private static final String BASE_URL = "https://api.climatiq.io";
    private static final String SPEND_PATH = "/procurement/v1/spend";
    // Climatiq는 배출계수 DB가 당해 연도를 지원하지 않을 수 있어, 2025로 고정
    // 키 발급 후 CLIMATIQ_확인사항.md 의 확인 방법에 따라 검증 필요
    private static final int SPEND_YEAR = 2025;

    private final String apiKey;
    private final RestClient restClient;

    public ClimatiqClient(@Value("${CLIMATIQ_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public CarbonSavingResult estimate(long priceKrw, int isic4Code) {
        if (apiKey == null || apiKey.isBlank()) {
            return CarbonSavingResult.notAvailable("API_ERROR");
        }

        Map<String, Object> requestBody = Map.of(
                "activity", Map.of(
                        "classification_code", String.valueOf(isic4Code),
                        "classification_type", "isic4"
                ),
                "spend_year", SPEND_YEAR,
                "spend_region", "KR",
                "money", priceKrw,
                "money_unit", "krw"
        );

        try {
            ClimatiqSpendResponse response = restClient.post()
                    .uri(SPEND_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ClimatiqSpendResponse.class);

            if (response == null || response.estimate() == null) {
                return CarbonSavingResult.notAvailable("API_ERROR");
            }
            return CarbonSavingResult.available(response.estimate().co2e(), "CLIMATIQ");

        } catch (ResourceAccessException e) {
            log.warn("Climatiq API timeout: {}", e.getMessage());
            return CarbonSavingResult.notAvailable("API_TIMEOUT");
        } catch (RestClientResponseException e) {
            log.warn("Climatiq API error {}: {}", e.getStatusCode(), e.getMessage());
            return CarbonSavingResult.notAvailable("API_ERROR");
        }
    }

    record ClimatiqSpendResponse(Estimate estimate) {
        record Estimate(double co2e) {}
    }
}