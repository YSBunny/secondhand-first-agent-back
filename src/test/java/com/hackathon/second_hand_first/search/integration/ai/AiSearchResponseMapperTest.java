package com.hackathon.second_hand_first.search.integration.ai;

import tools.jackson.databind.json.JsonMapper;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiGraphSearchResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRecommendedProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiSearchResponseMapperTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    /** AI가 실제로 내려주는 형태 그대로. 크롤러 통합 스키마를 담고 있다. */
    private static final String AI_RESPONSE = """
            {
              "request_id": "req-1",
              "query_parsed": {
                "product": "에어팟 프로 2", "budget": 200000,
                "purpose": "출퇴근용", "spec": null, "used_allowed": true
              },
              "items": [
                {
                  "platform": "BUNJANG", "platform_product_id": "111",
                  "url": "https://m.bunjang.co.kr/products/111",
                  "title": "에어팟 프로 2", "price": 150000,
                  "description": "상태 좋아요", "images": ["https://img/1.jpg"],
                  "condition_level": "LIGHTLY_USED",
                  "trade_method": ["PARCEL", "MEET"],
                  "location": {"name": "상암동", "full_address": "서울특별시 마포구 상암동", "precision": "FULL"},
                  "_score_breakdown": {"best_deal_score": 91},
                  "reasoning": "가격이 저렴합니다."
                },
                {
                  "platform": "ELEVENST", "platform_product_id": "222",
                  "url": "https://www.11st.co.kr/products/222",
                  "title": "에어팟 프로 2 새상품", "price": 279000,
                  "description": null, "images": [],
                  "condition_level": "NEW",
                  "trade_method": ["PARCEL"],
                  "location": {"name": null, "full_address": null, "precision": "NONE"},
                  "_score_breakdown": {"best_deal_score": 78}
                }
              ],
              "top_recommendation_ids": ["BUNJANG:111", "ELEVENST:222"]
            }
            """;

    private AiSearchResponse map() throws Exception {
        AiGraphSearchResponse graph = objectMapper.readValue(AI_RESPONSE, AiGraphSearchResponse.class);
        return AiSearchResponseMapper.toSearchResponse(graph);
    }

    @Test
    @DisplayName("AI가 주지 않는 안내 문구를 플랫폼과 건수로 조립한다")
    void buildsAssistantMessage() throws Exception {
        AiSearchResponse response = map();
        assertThat(response.assistantMessage()).isEqualTo("번개장터·11번가에서 2개 매물을 찾았어요.");
        assertThat(response.resultCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 조건은 query_parsed 를 그대로 옮긴다")
    void mapsParsedConditions() throws Exception {
        AiSearchResponse response = map();
        assertThat(response.parsedConditions().keyword()).isEqualTo("에어팟 프로 2");
        assertThat(response.parsedConditions().maxPrice()).isEqualTo(200000L);
        assertThat(response.parsedConditions().querySummary())
                .isEqualTo("에어팟 프로 2 · 200,000원 이하 · 출퇴근용");
    }

    @Test
    @DisplayName("중고를 허용하면 상태를 좁히지 않는다 — UNSPECIFIED/UNKNOWN 도 남긴다")
    void keepsAllConditionsWhenUsedAllowed() throws Exception {
        AiSearchResponse response = map();
        assertThat(response.parsedConditions().conditions())
                .contains(ProductCondition.UNSPECIFIED, ProductCondition.UNKNOWN);
    }

    @Test
    @DisplayName("순위는 top_recommendation_ids 순서를 그대로 따른다")
    void keepsAiRanking() throws Exception {
        List<AiRecommendedProductResponse> products = map().products();
        assertThat(products).hasSize(2);
        assertThat(products.get(0).rank()).isEqualTo(1);
        assertThat(products.get(0).product().externalProductId()).isEqualTo("111");
        assertThat(products.get(0).recommendationScore()).isEqualTo(91.0);
        assertThat(products.get(0).recommendationReason()).isEqualTo("가격이 저렴합니다.");
    }

    @Test
    @DisplayName("거래방식에서 직거래·택배 가능 여부를 끌어낸다")
    void derivesTradeFlags() throws Exception {
        List<AiRecommendedProductResponse> products = map().products();
        AiProductResponse bunjang = products.get(0).product();
        AiProductResponse elevenst = products.get(1).product();

        assertThat(bunjang.directTradeAvailable()).isTrue();
        assertThat(bunjang.shippingAvailable()).isTrue();
        assertThat(elevenst.directTradeAvailable()).isFalse();
        assertThat(elevenst.shippingAvailable()).isTrue();
    }

    @Test
    @DisplayName("새상품은 탄소 절감 대상이 아니다")
    void marksCarbonEligibility() throws Exception {
        List<AiRecommendedProductResponse> products = map().products();
        assertThat(products.get(0).product().carbonReductionEligible()).isTrue();
        assertThat(products.get(1).product().carbonReductionEligible()).isFalse();
    }

    @Test
    @DisplayName("위치를 객체 그대로 옮기고 좌표는 비워 둔다")
    void mapsLocation() throws Exception {
        var location = map().products().get(0).product().location();
        assertThat(location.fullAddress()).isEqualTo("서울특별시 마포구 상암동");
        assertThat(location.name()).isEqualTo("상암동");
        // 지오코딩은 백엔드가 따로 한다. 여기서 좌표를 지어내면 안 된다.
        assertThat(location.coordinates()).isNull();
    }

    @Test
    @DisplayName("거래 가능 지역이 여러 곳이면 전부 옮긴다")
    void keepsAllRegions() throws Exception {
        var graph = objectMapper.readValue("""
                {"request_id":"r","query_parsed":{"product":"x","used_allowed":true},
                 "items":[{"platform":"NAVER_FLEAMARKET","platform_product_id":"1","price":1000,
                           "condition_level":"USED","trade_method":["MEET"],
                           "location":{"name":"남천동","full_address":"부산광역시 수영구 남천동",
                                       "precision":"FULL",
                                       "regions":[{"name":"남천동","full_address":"부산광역시 수영구 남천동"},
                                                  {"name":"광안동","full_address":"부산광역시 수영구 광안동"}]}}],
                 "top_recommendation_ids":["NAVER_FLEAMARKET:1"]}
                """, AiGraphSearchResponse.class);
        var location = AiSearchResponseMapper.toSearchResponse(graph)
                .products().get(0).product().location();
        // 거리 계산이 가장 가까운 곳을 고르므로 하나만 넘기면 판매자에게 불리하다.
        assertThat(location.regions()).hasSize(2);
        assertThat(location.regions().get(1).fullAddress()).isEqualTo("부산광역시 수영구 광안동");
    }

    @Test
    @DisplayName("ProductUpsertService 가 요구하는 필수 값이 모두 채워진다")
    void fillsRequiredFields() throws Exception {
        for (AiRecommendedProductResponse recommendation : map().products()) {
            AiProductResponse product = recommendation.product();
            assertThat(product.platform()).isNotNull();
            assertThat(product.price()).isNotNull();
            assertThat(product.category()).isNotNull();
            assertThat(product.condition()).isNotNull();
            assertThat(product.status()).isEqualTo(ProductStatus.SELLING);
            assertThat(product.directTradeAvailable()).isNotNull();
            assertThat(product.shippingAvailable()).isNotNull();
            assertThat(product.carbonReductionEligible()).isNotNull();
            assertThat(product.externalViewCount()).isNotNull();
        }
    }

    @Test
    @DisplayName("AI가 추론한 카테고리를 그대로 쓴다")
    void usesInferredCategory() throws Exception {
        var graph = objectMapper.readValue("""
                {"request_id":"r","query_parsed":{"product":"원목 책상","used_allowed":true,
                                                  "category":"FURNITURE"},
                 "items":[{"platform":"BUNJANG","platform_product_id":"1","price":50000,
                           "condition_level":"USED","trade_method":["MEET"]}],
                 "top_recommendation_ids":["BUNJANG:1"]}
                """, AiGraphSearchResponse.class);
        assertThat(AiSearchResponseMapper.toSearchResponse(graph)
                .products().get(0).product().category())
                .isEqualTo(ProductCategory.FURNITURE);
    }

    @Test
    @DisplayName("카테고리를 추론하지 못했거나 모르는 값이면 OTHER 로 둔다")
    void fallsBackToOtherCategory() throws Exception {
        for (String value : new String[] {"null", "\"\"", "\"NOT_A_CATEGORY\""}) {
            var graph = objectMapper.readValue("""
                    {"request_id":"r","query_parsed":{"product":"x","used_allowed":true,
                                                      "category":%s},
                     "items":[{"platform":"BUNJANG","platform_product_id":"1","price":1000,
                               "condition_level":"USED","trade_method":["MEET"]}],
                     "top_recommendation_ids":["BUNJANG:1"]}
                    """.formatted(value), AiGraphSearchResponse.class);
            // 임의로 가까운 카테고리에 밀어 넣으면 탄소 계산이 엉뚱한 ISIC4 를 쓴다.
            assertThat(AiSearchResponseMapper.toSearchResponse(graph)
                    .products().get(0).product().category())
                    .isEqualTo(ProductCategory.OTHER);
        }
    }

    @Test
    @DisplayName("줄 수 없는 값은 지어내지 않고 비운다")
    void leavesUnknownFieldsNull() throws Exception {
        AiProductResponse product = map().products().get(0).product();
        assertThat(product.referencePrice()).isNull();
        assertThat(product.publishedAt()).isNull();
        assertThat(product.seller()).isNull();
    }

    @Test
    @DisplayName("결과가 없으면 그렇게 말한다")
    void handlesEmptyResult() throws Exception {
        AiGraphSearchResponse graph = objectMapper.readValue(
                """
                {"request_id":"r","query_parsed":{"product":"없는상품","used_allowed":true},
                 "items":[],"top_recommendation_ids":[]}
                """, AiGraphSearchResponse.class);
        AiSearchResponse response = AiSearchResponseMapper.toSearchResponse(graph);
        assertThat(response.assistantMessage()).isEqualTo("조건에 맞는 매물을 찾지 못했어요.");
        assertThat(response.resultCount()).isZero();
        assertThat(response.products()).isEmpty();
    }

    @Test
    @DisplayName("추천 id가 items 에 없으면 건너뛴다")
    void skipsUnknownRecommendationId() throws Exception {
        AiGraphSearchResponse graph = objectMapper.readValue(
                """
                {"request_id":"r","query_parsed":{"product":"x","used_allowed":true},
                 "items":[{"platform":"BUNJANG","platform_product_id":"1","price":1000,
                           "condition_level":"USED","trade_method":["PARCEL"]}],
                 "top_recommendation_ids":["BUNJANG:1","JOONGNA:없는것"]}
                """, AiGraphSearchResponse.class);
        assertThat(AiSearchResponseMapper.toSearchResponse(graph).products()).hasSize(1);
    }

    @Test
    @DisplayName("중고만 원하지 않으면 새것으로 좁힌다")
    void narrowsToNewWhenUsedNotAllowed() throws Exception {
        AiGraphSearchResponse graph = objectMapper.readValue(
                """
                {"request_id":"r","query_parsed":{"product":"x","used_allowed":false},
                 "items":[],"top_recommendation_ids":[]}
                """, AiGraphSearchResponse.class);
        assertThat(AiSearchResponseMapper.toSearchResponse(graph).parsedConditions().conditions())
                .containsExactly(ProductCondition.NEW);
    }
}
