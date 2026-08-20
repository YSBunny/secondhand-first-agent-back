package com.hackathon.second_hand_first.search.integration.ai;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiGraphItem;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRegionResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiGraphQueryParsed;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiGraphSearchResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiParsedConditionsResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRecommendedProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 그래프 응답을 백엔드 내부 형식으로 옮긴다.
 *
 * <p>두 형태가 다른 이유는 만든 쪽이 다르기 때문이다. AI는 크롤러 통합 스키마를
 * 그대로 들고 오고, 백엔드는 프론트 화면에 맞춘 형태를 쓴다.
 *
 * <p><b>AI가 주지 않는 값을 여기서 채운다.</b> 무엇을 어떤 근거로 채웠는지
 * 아래 상수와 주석에 남긴다. 근거 없이 채운 값은 나중에 화면에 거짓으로 뜬다.
 */
public final class AiSearchResponseMapper {

    /**
     * 크롤러는 판매 중인 매물만 수집한다. 네 크롤러 모두 수집 단계에서
     * 판매 완료·예약을 걸러내므로, 들어온 것은 전부 판매 중으로 본다.
     *
     * <p>다만 통합 스키마에 판매 상태 필드가 없어 캐시가 오래되면 이미 팔린 매물이
     * 남을 수 있다. 그건 재조회로 풀어야 할 문제다.
     */
    private static final ProductStatus ASSUMED_STATUS = ProductStatus.SELLING;

    /**
     * AI가 카테고리를 추론하지 못했을 때 쓴다.
     *
     * <p>ProductUpsertService 가 category 를 필수로 요구해서 null 을 둘 수 없다.
     * OTHER 는 ISIC4 매핑이 없으므로 탄소 계산에서 정직하게 빠진다.
     */
    private static final ProductCategory UNKNOWN_CATEGORY = ProductCategory.OTHER;

    /**
     * 크롤러가 조회수를 수집하지 않는다. 필수 값이라 0을 넣지만
     * <b>"조회 0회"라는 뜻이 아니라 "모른다"는 뜻이다.</b> 화면에 조회수를
     * 표시할 거면 이 값을 쓰면 안 된다.
     */
    private static final long UNKNOWN_VIEW_COUNT = 0L;

    /** 정렬 기준은 아직 사용자가 고르지 않는다. AI 추천순이 기본이다. */
    private static final SearchPriority DEFAULT_PRIORITY = SearchPriority.BEST_VALUE;

    private static final String TRADE_MEET = "MEET";
    private static final String TRADE_PARCEL = "PARCEL";

    private AiSearchResponseMapper() {
    }

    public static AiSearchResponse toSearchResponse(AiGraphSearchResponse graph) {
        List<AiGraphItem> items = graph.items() == null ? List.of() : graph.items();
        // 카테고리는 상품마다가 아니라 검색 한 건에 하나다. 사용자가 무엇을
        // 찾는지에서 나오는 값이라 후보마다 달라질 이유가 없다.
        ProductCategory category = categoryOf(
                graph.queryParsed() == null ? null : graph.queryParsed().category()
        );
        List<AiRecommendedProductResponse> products = toRecommendations(
                items, graph.topRecommendationIds(), category
        );
        return new AiSearchResponse(
                toParsedConditions(graph.queryParsed()),
                buildAssistantMessage(items),
                items.size(),
                products
        );
    }

    private static AiParsedConditionsResponse toParsedConditions(AiGraphQueryParsed parsed) {
        if (parsed == null) {
            return new AiParsedConditionsResponse(null, null, List.of(), DEFAULT_PRIORITY, null);
        }
        return new AiParsedConditionsResponse(
                parsed.product(),
                parsed.budget(),
                toConditions(parsed.usedAllowed()),
                DEFAULT_PRIORITY,
                buildQuerySummary(parsed)
        );
    }

    /**
     * AI는 중고 허용 여부를 boolean 하나로 준다. 프론트는 상태 목록을 받는다.
     *
     * <p>중고를 원하지 않으면 새것만 남기고, 허용하면 전 상태를 남긴다.
     * UNSPECIFIED / UNKNOWN 을 빼지 않는 것이 중요하다 — 둘 다 "상태가 나쁘다"는
     * 뜻이 아니라 각각 "판매자가 안 적음", "우리가 해석 못 함"이다.
     */
    private static List<ProductCondition> toConditions(Boolean usedAllowed) {
        if (Boolean.FALSE.equals(usedAllowed)) {
            return List.of(ProductCondition.NEW);
        }
        return List.of(ProductCondition.values());
    }

    private static String buildQuerySummary(AiGraphQueryParsed parsed) {
        List<String> parts = new ArrayList<>();
        if (parsed.product() != null) {
            parts.add(parsed.product());
        }
        if (parsed.budget() != null) {
            parts.add(String.format("%,d원 이하", parsed.budget()));
        }
        if (parsed.purpose() != null) {
            parts.add(parsed.purpose());
        }
        if (parsed.spec() != null) {
            parts.add(parsed.spec());
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /**
     * AI는 안내 문구를 만들지 않는다. 화면에 띄울 한 줄이라 백엔드가 조립한다.
     *
     * <p>어느 플랫폼에서 몇 건을 찾았는지만 말한다. 없는 사실을 덧붙이지 않는다.
     */
    private static String buildAssistantMessage(List<AiGraphItem> items) {
        if (items.isEmpty()) {
            return "조건에 맞는 매물을 찾지 못했어요.";
        }
        List<String> names = items.stream()
                .map(AiGraphItem::platform)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(AiSearchResponseMapper::platformName)
                .toList();
        return String.format("%s에서 %d개 매물을 찾았어요.", String.join("·", names), items.size());
    }

    private static String platformName(Platform platform) {
        return switch (platform) {
            case BUNJANG -> "번개장터";
            case JOONGNA -> "중고나라";
            case NAVER_FLEAMARKET -> "N플리마켓";
            case ELEVENST -> "11번가";
        };
    }

    /**
     * 추천 순위를 매긴다.
     *
     * <p>순위는 AI의 top_recommendation_ids 순서를 그대로 따른다. 백엔드가 다시
     * 정렬하면 AI가 계산한 순위와 어긋나 두 곳에서 다른 답이 나온다.
     */
    /**
     * AI가 준 카테고리 문자열을 enum 으로 옮긴다.
     *
     * <p>추론하지 못했거나 우리가 모르는 값이면 OTHER 로 둔다. 임의로 가까운
     * 카테고리에 밀어 넣으면 탄소 계산이 엉뚱한 ISIC4 코드를 쓰게 된다.
     */
    private static ProductCategory categoryOf(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_CATEGORY;
        }
        try {
            return ProductCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UNKNOWN_CATEGORY;
        }
    }

    private static List<AiRecommendedProductResponse> toRecommendations(
            List<AiGraphItem> items,
            List<String> topIds,
            ProductCategory category
    ) {
        if (topIds == null || topIds.isEmpty()) {
            return List.of();
        }
        Map<String, AiGraphItem> byId = new LinkedHashMap<>();
        for (AiGraphItem item : items) {
            byId.put(item.itemId(), item);
        }

        List<AiRecommendedProductResponse> result = new ArrayList<>();
        int rank = 1;
        for (String id : topIds) {
            AiGraphItem item = byId.get(id);
            if (item == null) {
                // AI가 준 id가 items에 없으면 조용히 건너뛴다. 빈 상품을 만들어
                // 넣으면 SearchSessionService 검증에서 터진다.
                continue;
            }
            result.add(new AiRecommendedProductResponse(
                    rank++,
                    item.scoreBreakdown() == null ? null : item.scoreBreakdown().bestDealScore(),
                    item.reasoning(),
                    toProduct(item, category)
            ));
        }
        return result;
    }

    private static AiProductResponse toProduct(AiGraphItem item, ProductCategory category) {
        List<String> tradeMethod = item.tradeMethod() == null ? List.of() : item.tradeMethod();
        return new AiProductResponse(
                item.platform(),
                item.platformProductId(),
                item.title(),
                item.description(),
                category,
                item.price(),
                // 공식 스토어 가격 개념이 파이프라인에 없다. 11번가는 오픈마켓
                // 판매자가지 정가가 아니므로 정가 대비 절감률을 만들 근거가 없다.
                null,
                item.conditionLevel() == null ? ProductCondition.UNKNOWN : item.conditionLevel(),
                ASSUMED_STATUS,
                locationOf(item),
                tradeMethod.contains(TRADE_MEET),
                tradeMethod.contains(TRADE_PARCEL),
                isCarbonReductionEligible(item),
                item.url(),
                UNKNOWN_VIEW_COUNT,
                // 크롤러가 등록 시각을 수집하지 않는다. collected_at 은 수집 시각이지
                // 매물이 올라온 시각이 아니라서 대신 쓸 수 없다.
                null,
                item.images() == null ? List.of() : item.images(),
                // 판매자 정보는 통합 스키마에 없다. 번개장터만 별점이 풍부해
                // 그대로 넣으면 플랫폼 간 비교가 불공정해진다.
                null
        );
    }

    /**
     * 위치를 그대로 옮긴다.
     *
     * <p>좌표는 채우지 않는다. 크롤러는 행정동 텍스트만 주고 지오코딩은
     * 백엔드가 따로 한다. 여기서 null 이 아닌 값을 지어내면 안 된다.
     *
     * <p>regions 를 함께 옮기는 것이 중요하다. 거래 가능 지역이 여러 곳인
     * 매물이 있고(N플리마켓 최대 3곳), 거리 계산은 그중 가장 가까운 곳을 쓴다.
     * 대표 주소만 넘기면 판매자에게 불리하게 계산된다.
     */
    private static AiLocationResponse locationOf(AiGraphItem item) {
        AiGraphItem.AiGraphLocation location = item.location();
        if (location == null) {
            return null;
        }
        return new AiLocationResponse(
                location.name(),
                location.fullAddress(),
                parsePrecision(location.precision()),
                toRegions(location.regions()),
                null
        );
    }

    private static ProductLocationGeocodeRequest.Precision parsePrecision(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ProductLocationGeocodeRequest.Precision.valueOf(value);
        } catch (IllegalArgumentException exception) {
            // 크롤러가 새 값을 내보내기 시작한 것이다. 임의로 매핑하지 않는다.
            return null;
        }
    }

    private static List<AiRegionResponse> toRegions(List<AiGraphItem.AiGraphRegion> regions) {
        if (regions == null) {
            return List.of();
        }
        return regions.stream()
                .map(region -> new AiRegionResponse(
                        region.name(), region.fullAddress(), region.code(), null))
                .toList();
    }

    /**
     * 탄소 절감 대상인지 본다. 새상품 구매에는 절감이 없다.
     *
     * <p>판정은 상품 상태로 한다. 11번가에도 중고 매물이 실제로 존재하므로
     * 플랫폼만으로 자르면 그런 매물이 빠진다.
     */
    private static boolean isCarbonReductionEligible(AiGraphItem item) {
        if (item.platform() != Platform.ELEVENST) {
            return true;
        }
        return item.conditionLevel() == ProductCondition.USED;
    }
}
