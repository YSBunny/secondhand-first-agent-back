package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.DeliveryCarrier;
import com.hackathon.second_hand_first.product.domain.DeliveryMethod;
import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.DeliveryStatus;
import com.hackathon.second_hand_first.product.domain.TradeType;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryExtraCostResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryOptionResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

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
     * 통합 스키마의 배송비를 계약 형태로 옮긴다.
     *
     * <p>검색 경로(AI 응답 → 저장)와 <b>같은 값이 들어가야 한다.</b> 같은 매물이
     * 들어온 경로에 따라 배송비가 다르면 총 지불액 비교가 어긋난다.
     */
    private static AiDeliveryFeeResponse deliveryFee(CrawlerItem item) {
        CrawlerDeliveryFee fee = item.deliveryFee();
        if (fee == null) {
            return null;
        }
        DeliveryStatus status = deliveryStatus(fee.status());
        if (status == null) {
            // 상태를 모르면 배송비 전체를 넣지 않는다. ProductUpsertService 가
            // 상태를 필수로 요구하기도 하고, 모르는 것을 AVAILABLE 로 가정하면
            // 직거래 전용 매물이 «택배 가능»으로 둔갑한다.
            return null;
        }
        return new AiDeliveryFeeResponse(
                status,
                deliveryPayer(fee.payer()),
                fee.minFee(),
                fee.homeDeliveryFee(),
                extraCost(fee),
                fee.optionsOrEmpty().stream()
                        .filter(option -> option != null)
                        .map(CrawlerItemMapper::deliveryOption)
                        .toList()
        );
    }

    /**
     * 도서산간·제주 추가 배송비.
     *
     * <p>통합 스키마는 둘을 나누지 않는다. {@code option.remote_fee} 가 도서산간
     * 금액이고 제주 금액은 원문 문장에만 있다. <b>모르는 쪽을 0 으로 채우지 않는다</b> —
     * 0 이면 «제주는 추가금이 없다»로 읽힌다.
     */
    private static AiDeliveryExtraCostResponse extraCost(CrawlerDeliveryFee fee) {
        Long remote = fee.optionsOrEmpty().stream()
                .filter(option -> option != null && option.remoteFee() != null)
                .map(CrawlerDeliveryFee.Option::remoteFee)
                .max(Long::compareTo)
                .orElse(null);
        String description = extraCostDescription(fee.raw());
        if (remote == null && description == null) {
            return null;
        }
        return new AiDeliveryExtraCostResponse(null, remote, description);
    }

    /**
     * {@code raw} 는 플랫폼 원본이라 형태가 제각각이다 — 11번가는 과반이 문자열이고
     * 중고나라는 배열과 객체가 섞인다. <b>객체일 때만</b> 안내 문장을 꺼낸다.
     */
    private static String extraCostDescription(JsonNode raw) {
        if (raw == null || !raw.isObject()) {
            return null;
        }
        JsonNode value = raw.path("extraCost");
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private static AiDeliveryOptionResponse deliveryOption(CrawlerDeliveryFee.Option option) {
        return new AiDeliveryOptionResponse(
                deliveryMethod(option.method()),
                deliveryCarrier(option.carrier()),
                option.requiresPickupPoint(),
                option.fee(),
                // 원본 코드를 그대로 남긴다. 나중에 «이게 무슨 배송이었나»를 추적할 수 있다.
                option.rawCode() == null ? null : StringNode.valueOf(option.rawCode())
        );
    }

    private static DeliveryStatus deliveryStatus(String value) {
        return parseEnum(DeliveryStatus.class, value);
    }

    private static DeliveryPayer deliveryPayer(String value) {
        return parseEnum(DeliveryPayer.class, value);
    }

    /**
     * 모르는 배송 수단은 {@link DeliveryMethod#UNKNOWN} 이다.
     *
     * <p>{@code null} 로 두면 {@code ProductDeliveryOption} 이 거부한다. 통합 스키마에도
     * 같은 뜻의 값이 있어 그대로 대응한다.
     */
    private static DeliveryMethod deliveryMethod(String value) {
        DeliveryMethod method = parseEnum(DeliveryMethod.class, value);
        return method == null ? DeliveryMethod.UNKNOWN : method;
    }

    /** 모르는 택배사는 지어내지 않고 비운다. 계약에 없는 값은 저장할 수 없다. */
    private static DeliveryCarrier deliveryCarrier(String value) {
        return parseEnum(DeliveryCarrier.class, value);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
