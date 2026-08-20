package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.DeliveryCarrier;
import com.hackathon.second_hand_first.product.domain.DeliveryMethod;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.TradeType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AiSearchResponseContractTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 확정된_AI_검색_응답을_역직렬화한다() throws Exception {
        AiSearchResponse response = jsonMapper.readValue("""
                {
                  "requestId": "req_01",
                  "sessionId": "ss_01",
                  "scoring": {"version": "v1"},
                  "parsedConditions": {
                    "keyword": "에어팟 프로 2",
                    "maxPrice": 300000,
                    "conditions": ["LIKE_NEW"],
                    "priority": null,
                    "querySummary": "300,000원 이하"
                  },
                  "assistantMessage": "총 1개의 상품을 추천했어요.",
                  "marketReference": {
                    "productName": "에어팟 프로 2",
                    "sourcePlatform": "ELEVENST",
                    "sourceName": "11번가",
                    "referenceType": "POPULAR_NEW_PRODUCT",
                    "medianPrice": 377825,
                    "sampleCount": 4,
                    "calculatedAt": "2026-08-21T10:00:00+09:00",
                    "sourceUrl": "https://www.11st.co.kr/products/9490377615"
                  },
                  "totalResultCount": 1,
                  "products": [{
                    "rank": 1,
                    "recommendationScore": 88,
                    "recommendationReason": "합리적입니다.",
                    "scoreBreakdown": {
                      "priceScore": 91,
                      "qualityScore": 80,
                      "convenienceScore": 82
                    },
                    "distanceKm": 3.2,
                    "product": {
                      "platform": "BUNJANG",
                      "externalProductId": "101",
                      "title": "에어팟 프로 2",
                      "description": null,
                      "category": "EARPHONES",
                      "price": 180000,
                      "condition": "LIKE_NEW",
                      "status": "SELLING",
                      "location": {
                        "displayName": "판교동",
                        "latitude": 37.3947,
                        "longitude": 127.1111
                      },
                      "tradeTypes": ["DIRECT", "DELIVERY"],
                      "deliveryFee": {
                        "status": "AVAILABLE",
                        "payer": "BUYER",
                        "minFee": 3000,
                        "homeDeliveryFee": 4000,
                        "extraCost": {
                          "jejuFee": 6000,
                          "remoteAreaFee": 6000,
                          "description": "추가 배송비가 발생합니다."
                        },
                        "options": [{
                          "method": "CONVENIENCE_STORE",
                          "carrier": "GS25",
                          "requiresPickupPoint": true,
                          "fee": 3000,
                          "rawCode": "GS_HALF_PRICE"
                        }]
                      },
                      "platformUrl": "https://www.bunjang.co.kr/products/101",
                      "externalViewCount": null,
                      "publishedAt": null,
                      "imageUrls": [],
                      "seller": null
                    }
                  }]
                }
                """, AiSearchResponse.class);

        assertThat(response.requestId()).isEqualTo("req_01");
        assertThat(response.totalResultCount()).isEqualTo(1);
        assertThat(response.marketReference().sourcePlatform()).isEqualTo(Platform.ELEVENST);

        AiRecommendedProductResponse recommendation = response.products().getFirst();
        assertThat(recommendation.scoreBreakdown().convenienceScore()).isEqualTo(82.0);
        assertThat(recommendation.product().tradeTypes())
                .containsExactly(TradeType.DIRECT, TradeType.DELIVERY);
        assertThat(recommendation.product().location().name()).isEqualTo("판교동");
        assertThat(recommendation.product().location().coordinates().latitude())
                .isEqualTo(37.3947);

        AiDeliveryOptionResponse deliveryOption = recommendation.product()
                .deliveryFee().options().getFirst();
        assertThat(deliveryOption.method()).isEqualTo(DeliveryMethod.CONVENIENCE_STORE);
        assertThat(deliveryOption.carrier()).isEqualTo(DeliveryCarrier.GS25);
        assertThat(deliveryOption.rawCode().asString()).isEqualTo("GS_HALF_PRICE");
    }
}
