package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.TradeType;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 크롤러 상품 한 건을 {@link AiProductResponse} 로 옮긴다.
 *
 * <p>저장은 {@code ProductUpsertService.upsert()} 가 한다. 그쪽이 이미 필수값 검증과
 * 시간대 변환을 하므로 여기서는 형태만 맞춘다.
 *
 * <p><b>크롤러에 없는 값은 지어내지 않는다.</b> 다만 {@code ProductUpsertService} 가
 * 필수로 요구하는 값은 비워 둘 수 없어, 아래 것들은 «모른다»는 뜻으로 채우고
 * 그 사실을 각 자리에 적어 둔다.
 */
public final class CrawlerItemMapper {

    /**
     * 크롤러는 판매 상태를 수집하지 않는다. 대신 수집 시점에 판매중만 거른다.
     *
     * <p>{@code AiSearchResponseMapper} 도 같은 가정을 쓴다. 캐시가 오래되면 이미 팔린
     * 상품이 남을 수 있다는 한계도 같다.
     */
    private static final ProductStatus ASSUMED_STATUS = ProductStatus.SELLING;

    /**
     * 크롤러는 조회수를 수집하지 않는다. 컬럼이 {@code nullable = false} 라 비울 수 없어 0을 넣는다.
     *
     * <p><b>«조회 0회»가 아니라 «모른다»는 뜻이다.</b> 이 값으로 인기를 판단하면
     * 크롤러가 넣은 상품이 전부 최하위가 된다.
     */
    private static final long UNKNOWN_VIEW_COUNT = 0L;

    private static final String TRADE_MEET = "MEET";
    private static final String TRADE_PARCEL = "PARCEL";

    private CrawlerItemMapper() {
    }

    /**
     * @param category 검색어에서 판정한 카테고리. 상품마다가 아니라 <b>파일 하나에 하나</b>다.
     *                 판정하지 못했으면 {@link ProductCategory#OTHER} 가 들어온다.
     */
    public static AiProductResponse toProduct(CrawlerItem item, ProductCategory category) {
        Platform platform = platformOf(item.platform());
        ProductCondition condition = conditionOf(item.conditionLevel());
        return new AiProductResponse(
                platform,
                item.platformProductId(),
                item.title(),
                item.description(),
                category,
                item.price(),
                condition,
                ASSUMED_STATUS,
                item.location(),
                tradeTypes(item),
                deliveryFee(item),
                item.url(),
                UNKNOWN_VIEW_COUNT,
                // collected_at 은 수집 시각이지 매물 등록 시각이 아니다.
                // 등록일로 쓰면 «방금 올라온 매물»이 전부 거짓이 된다.
                null,
                item.imagesOrEmpty(),
                // 판매자 정보는 통합 스키마에 없다. 플랫폼마다 제공 수준이 크게 달라
                // 그대로 넣으면 플랫폼 간 비교가 불공정해진다.
                null
        );
    }

    /**
     * 배송비는 아직 옮기지 않는다.
     *
     * <p>크롤러는 {@code delivery_fee} 를 주고 저장할 컬럼도 있다. 다만 어떤 값을
     * 어디에 넣을지 정리하는 작업이 따로 진행 중이라, 정해질 때까지 비워 둔다.
     *
     * <p><b>채울 때 이 메서드 하나만 고치면 된다.</b> 검색 경로(AI 응답 → 저장)에서는
     * 이미 배송비가 저장되고 있으므로, 적재 경로만 맞추면 두 경로가 같아진다.
     */
    private static AiDeliveryFeeResponse deliveryFee(CrawlerItem item) {
        return null;
    }

    /**
     * 거래방식을 계약 enum 으로 옮긴다.
     *
     * <p>통합 스키마의 {@code MEET} · {@code PARCEL} 이 각각 직거래 · 택배다.
     * 모르는 값은 넣지 않는다 — 크롤러가 새 값을 내보내면 조용히 사라지는 대신
     * 목록에서 빠져 눈에 띈다.
     */
    private static List<TradeType> tradeTypes(CrawlerItem item) {
        List<TradeType> types = new ArrayList<>();
        if (item.tradeMethodOrEmpty().contains(TRADE_MEET)) {
            types.add(TradeType.DIRECT);
        }
        if (item.tradeMethodOrEmpty().contains(TRADE_PARCEL)) {
            types.add(TradeType.DELIVERY);
        }
        return types;
    }

    private static Platform platformOf(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("platform 이 비어 있습니다.");
        }
        try {
            return Platform.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("모르는 platform 입니다: " + value, exception);
        }
    }

    /**
     * 모르는 상태값은 {@link ProductCondition#UNKNOWN} 으로 둔다.
     *
     * <p>가까운 값으로 밀어 넣으면 «상태 미상»이 «중고»로 둔갑해 점수가 달라진다.
     * 크롤러가 새 상태값을 내보내면 여기가 UNKNOWN 을 늘리며 알려 준다.
     */
    private static ProductCondition conditionOf(String value) {
        if (value == null || value.isBlank()) {
            return ProductCondition.UNKNOWN;
        }
        try {
            return ProductCondition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ProductCondition.UNKNOWN;
        }
    }
}
