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

/**
 * Climatiq 로 금액 기반 탄소 배출량을 추정한다.
 *
 * <p><b>{@code /procurement/v1/spend} 를 쓰지 않는다.</b> 그쪽이 ISIC4 코드만 주면 되는
 * 편한 경로지만 <b>유료 전용</b>이라, 무료 키로 부르면 403 이 온다.
 *
 * <pre>
 * "Your API key is valid, but you do not have access to this premium feature."
 * </pre>
 *
 * <p>대신 {@code /data/v1/estimate} 에 배출계수를 직접 지정한다. 무료 키로 동작하며,
 * 한국(KR) 금액 기반 계수가 CEDA 데이터셋으로 제공된다.
 */
@Component
public class ClimatiqClient {

    private static final Logger log = LoggerFactory.getLogger(ClimatiqClient.class);

    private static final String BASE_URL = "https://api.climatiq.io";
    private static final String ESTIMATE_PATH = "/data/v1/estimate";

    /**
     * 배출계수 데이터 버전. {@code ^36} 은 «36 메이저 안에서 최신»이라는 뜻이다.
     *
     * <p>메이저를 고정하는 이유는, 버전이 올라가며 계수가 크게 달라지면 같은 상품의
     * 절감량이 어느 날 갑자기 바뀌기 때문이다. 마이너 갱신은 받아들인다.
     */
    private static final String DATA_VERSION = "^36";

    /**
     * 계수 연도. 배출계수는 산업 통계가 집계된 뒤에 만들어져 <b>당해 연도는 대개 없다.</b>
     * 2025 가 현재 KR 계수의 최신 연도임을 실제 호출로 확인했다.
     */
    private static final int FACTOR_YEAR = 2025;

    private static final String REGION = "KR";
    private static final String MONEY_UNIT = "krw";

    private final String apiKey;
    private final RestClient restClient;

    public ClimatiqClient(@Value("${CLIMATIQ_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * @param activityId Climatiq 배출계수 식별자. {@code CarbonSavingService} 가 카테고리마다 정해 둔다.
     */
    public CarbonSavingResult estimate(long priceKrw, String activityId) {
        if (apiKey == null || apiKey.isBlank()) {
            return CarbonSavingResult.notAvailable("API_ERROR");
        }
        if (activityId == null || activityId.isBlank()) {
            return CarbonSavingResult.notAvailable("NO_CATEGORY_MAPPING");
        }

        Map<String, Object> requestBody = Map.of(
                "emission_factor", Map.of(
                        "activity_id", activityId,
                        "data_version", DATA_VERSION,
                        "region", REGION,
                        "year", FACTOR_YEAR
                ),
                "parameters", Map.of(
                        "money", priceKrw,
                        "money_unit", MONEY_UNIT
                )
        );

        try {
            ClimatiqEstimateResponse response = restClient.post()
                    .uri(ESTIMATE_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ClimatiqEstimateResponse.class);

            if (response == null || response.co2e() == null) {
                return CarbonSavingResult.notAvailable("API_ERROR");
            }
            return CarbonSavingResult.available(response.co2e(), "CLIMATIQ");

        } catch (ResourceAccessException e) {
            log.warn("Climatiq API timeout: {}", e.getMessage());
            return CarbonSavingResult.notAvailable("API_TIMEOUT");
        } catch (RestClientResponseException e) {
            // 403 이면 유료 기능을 부른 것이다. 무료 키로는 /data/v1/estimate 만 된다.
            log.warn("Climatiq API error {} — activityId={} body={}",
                    e.getStatusCode(), activityId, e.getResponseBodyAsString());
            return CarbonSavingResult.notAvailable("API_ERROR");
        }
    }

    /**
     * 응답에서 {@code co2e} 만 읽는다. 나머지 필드(계수 출처·구성 가스 등)는 무시한다.
     *
     * <p>{@code Double} 로 받는 이유는 필드가 없을 때 0.0 이 아니라 null 이 되도록 하기
     * 위해서다. <b>«배출량 0»과 «계산 실패»는 다르다.</b>
     */
    record ClimatiqEstimateResponse(Double co2e) {
    }
}
