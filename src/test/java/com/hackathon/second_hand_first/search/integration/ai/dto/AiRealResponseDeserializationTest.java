package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>AI 서버가 실제로 만든 JSON</b> 이 백엔드 DTO 로 읽히는지 본다.
 *
 * <p>손으로 쓴 예시가 아니라 AI 저장소의 contract.py 가 생성한 출력이다.
 * 어느 한쪽이 필드 이름을 바꾸면 여기가 먼저 깨진다.
 */
class AiRealResponseDeserializationTest {

    private AiSearchResponse response;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        try (InputStream input = getClass().getResourceAsStream("/ai-contract/sample.json")) {
            response = mapper.readValue(input, AiSearchResponse.class);
        }
    }

    @Test
    @DisplayName("최상위 필드가 모두 읽힌다")
    void readsTopLevel() {
        assertThat(response.requestId()).isEqualTo("req_01");
        assertThat(response.sessionId()).isEqualTo("ss_01");
        assertThat(response.scoring()).isNotNull();
        assertThat(response.scoring().version()).isNotBlank();
        assertThat(response.assistantMessage()).isNotBlank();
        assertThat(response.totalResultCount()).isPositive();
        assertThat(response.products()).isNotEmpty();
        assertThat(response.totalResultCount()).isEqualTo(response.products().size());
    }

    @Test
    @DisplayName("검색 조건이 읽힌다")
    void readsParsedConditions() {
        AiParsedConditionsResponse conditions = response.parsedConditions();

        assertThat(conditions).isNotNull();
        assertThat(conditions.keyword()).isNotBlank();
        assertThat(conditions.maxPrice()).isNotNull();
        assertThat(conditions.conditions()).isNotEmpty();
        assertThat(conditions.priority()).isNotNull();
    }

    @Test
    @DisplayName("상품 필드가 enum 까지 읽힌다 — 이름이 하나라도 어긋나면 null 이 된다")
    void readsProduct() {
        AiProductResponse product = response.products().getFirst().product();

        assertThat(product.platform()).isInstanceOf(Platform.class);
        assertThat(product.externalProductId()).isNotBlank();
        assertThat(product.title()).isNotBlank();
        assertThat(product.category()).isEqualTo(ProductCategory.EARPHONES);
        assertThat(product.price()).isPositive();
        assertThat(product.condition()).isNotNull();
        assertThat(product.status()).isNotNull();
        assertThat(product.platformUrl()).startsWith("http");
        assertThat(product.imageUrls()).isNotNull();
    }

    @Test
    @DisplayName("거래 방식이 enum 으로 읽힌다")
    void readsTradeTypes() {
        assertThat(response.products())
                .allSatisfy(item -> assertThat(item.product().tradeTypes())
                        .isNotNull()
                        .allSatisfy(type -> assertThat(type).isInstanceOf(TradeType.class)));
    }

    @Test
    @DisplayName("배송비가 옵션까지 읽힌다")
    void readsDeliveryFee() {
        AiDeliveryFeeResponse fee = response.products().stream()
                .map(item -> item.product().deliveryFee())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow();

        assertThat(fee.status()).isNotNull();
        assertThat(fee.options()).isNotNull();
        assertThat(fee.options()).allSatisfy(option ->
                assertThat(option.method()).isNotNull());
    }

    @Test
    @DisplayName("위치가 읽히고 full_address 가 snake_case 로 매핑된다")
    void readsLocation() {
        AiLocationResponse location = response.products().stream()
                .map(item -> item.product().location())
                .filter(java.util.Objects::nonNull)
                .filter(loc -> loc.fullAddress() != null)
                .findFirst()
                .orElseThrow();

        assertThat(location.fullAddress()).isNotBlank();
        assertThat(location.precision()).isNotNull();
    }

    @Test
    @DisplayName("추천 순위와 점수가 계약을 지킨다")
    void readsRecommendations() {
        List<AiRecommendedProductResponse> products = response.products();

        for (int i = 0; i < products.size(); i++) {
            assertThat(products.get(i).rank()).isEqualTo(i + 1);
            assertThat(products.get(i).recommendationScore()).isBetween(0.0, 100.0);
            if (i > 0) {
                assertThat(products.get(i).recommendationScore())
                        .isLessThanOrEqualTo(products.get(i - 1).recommendationScore());
            }
        }
        assertThat(products.getFirst().scoreBreakdown()).isNotNull();
        assertThat(products.getFirst().recommendationReason()).isNotBlank();
    }

    @Test
    @DisplayName("모르는 값이 0 이 아니라 null 로 온다")
    void keepsUnknownAsNull() {
        AiProductResponse product = response.products().getFirst().product();

        assertThat(product.externalViewCount())
                .as("«조회 0회»와 «수집 못 함»은 다르다")
                .isNull();
        assertThat(product.publishedAt())
                .as("collected_at 은 등록 시각이 아니다")
                .isNull();
        assertThat(product.seller()).isNull();
        assertThat(response.marketReference())
                .as("시세를 구하지 못했으면 빈 객체가 아니라 null")
                .isNull();
    }

    @Test
    @DisplayName("11번가 신품은 최대 1개")
    void limitsNewGoods() {
        long newGoods = response.products().stream()
                .filter(item -> item.product().platform() == Platform.ELEVENST)
                .count();

        assertThat(newGoods).isLessThanOrEqualTo(1);
    }
}
