package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.DeliveryCarrier;
import com.hackathon.second_hand_first.product.domain.DeliveryMethod;
import com.hackathon.second_hand_first.product.domain.DeliveryStatus;
import com.hackathon.second_hand_first.product.domain.TradeType;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 크롤러 출력으로 검증한다. 손으로 만든 JSON 은 스키마가 바뀐 것을 잡지 못한다.
 */
class CrawlerItemMapperTest {

    private CrawlerFile crawled;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream input = getClass()
                .getResourceAsStream("/crawler-sample/unified_에어팟_프로_3.json")) {
            crawled = JsonMapper.builder().build().readValue(input, CrawlerFile.class);
        }
    }

    private CrawlerItem first(Predicate<CrawlerItem> filter) {
        return crawled.itemsOrEmpty().stream().filter(filter).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("실제 크롤러 JSON 을 읽는다 — 검색어와 상품이 함께 들어 있다")
    void readsRealCrawlerOutput() {
        assertThat(crawled.query()).isEqualTo("에어팟 프로 3");
        assertThat(crawled.itemsOrEmpty()).isNotEmpty();
        assertThat(crawled.itemsOrEmpty()).allSatisfy(item -> {
            assertThat(item.platform()).isNotBlank();
            assertThat(item.platformProductId()).isNotBlank();
            assertThat(item.url()).startsWith("http");
        });
    }

    @Test
    @DisplayName("네 플랫폼이 모두 변환된다")
    void mapsEveryPlatform() {
        List<Platform> platforms = crawled.itemsOrEmpty().stream()
                .map(item -> CrawlerItemMapper.toProduct(item, ProductCategory.EARPHONES))
                .map(AiProductResponse::platform)
                .distinct()
                .toList();

        assertThat(platforms).contains(
                Platform.BUNJANG, Platform.JOONGNA,
                Platform.NAVER_FLEAMARKET, Platform.ELEVENST
        );
    }

    @Test
    @DisplayName("거래방식이 계약 enum 으로 옮겨진다")
    void mapsTradeTypes() {
        CrawlerItem meetOnly = first(item -> item.tradeMethodOrEmpty().equals(List.of("MEET")));
        AiProductResponse product = CrawlerItemMapper.toProduct(meetOnly, ProductCategory.EARPHONES);

        assertThat(product.tradeTypes()).containsExactly(TradeType.DIRECT);
        assertThat(product.supports(TradeType.DELIVERY)).isFalse();
    }

    @Test
    @DisplayName("직거래와 택배가 모두 되면 둘 다 담는다")
    void mapsBothTradeTypes() {
        CrawlerItem both = first(item ->
                item.tradeMethodOrEmpty().contains("MEET")
                        && item.tradeMethodOrEmpty().contains("PARCEL"));
        AiProductResponse product = CrawlerItemMapper.toProduct(both, ProductCategory.EARPHONES);

        assertThat(product.tradeTypes())
                .containsExactlyInAnyOrder(TradeType.DIRECT, TradeType.DELIVERY);
    }

    @Test
    @DisplayName("모르는 값을 지어내지 않는다")
    void doesNotInventValues() {
        AiProductResponse product = CrawlerItemMapper.toProduct(
                crawled.itemsOrEmpty().getFirst(), ProductCategory.EARPHONES
        );

        // collected_at 은 수집 시각이지 등록 시각이 아니다.
        assertThat(product.publishedAt()).isNull();
        // 통합 스키마에 판매자 정보가 없다.
        assertThat(product.seller()).isNull();
        // 조회수는 «0회»가 아니라 «모른다»는 뜻으로 0이다.
        assertThat(product.externalViewCount()).isZero();
        // 상태는 수집 시점에 판매중만 걸러졌다는 가정이다.
        assertThat(product.status()).isEqualTo(ProductStatus.SELLING);
    }

    @Test
    @DisplayName("모르는 상태값은 가까운 값으로 밀어 넣지 않고 UNKNOWN 이다")
    void unknownConditionStaysUnknown() {
        CrawlerItem sample = crawled.itemsOrEmpty().getFirst();
        CrawlerItem weird = new CrawlerItem(
                sample.platform(), sample.platformProductId(), sample.url(), sample.title(),
                sample.description(), sample.price(),
                "MINT_CONDITION",      // 크롤러가 내보낸 적 없는 값
                sample.tradeMethod(), sample.deliveryFee(), sample.images(), sample.location()
        );

        assertThat(CrawlerItemMapper.toProduct(weird, ProductCategory.EARPHONES).condition())
                .isEqualTo(ProductCondition.UNKNOWN);
    }

    @Test
    @DisplayName("모르는 플랫폼은 조용히 넘기지 않고 예외를 낸다")
    void unknownPlatformFails() {
        CrawlerItem sample = crawled.itemsOrEmpty().getFirst();
        CrawlerItem weird = new CrawlerItem(
                "DANGGEUN", sample.platformProductId(), sample.url(), sample.title(),
                sample.description(), sample.price(), sample.conditionLevel(),
                sample.tradeMethod(), sample.deliveryFee(), sample.images(), sample.location()
        );

        assertThatThrownBy(() -> CrawlerItemMapper.toProduct(weird, ProductCategory.EARPHONES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DANGGEUN");
    }

    @Test
    @DisplayName("배송비가 옵션까지 옮겨진다")
    void mapsDeliveryFee() {
        CrawlerItem withOptions = first(item ->
                item.deliveryFee() != null && !item.deliveryFee().optionsOrEmpty().isEmpty());

        AiDeliveryFeeResponse fee = CrawlerItemMapper
                .toProduct(withOptions, ProductCategory.EARPHONES).deliveryFee();

        assertThat(fee).isNotNull();
        assertThat(fee.status()).isNotNull();
        assertThat(fee.options()).isNotEmpty();
        assertThat(fee.options()).allSatisfy(option ->
                assertThat(option.method()).isNotNull());
    }

    @Test
    @DisplayName("직거래 전용은 NOT_AVAILABLE 로 남는다 — 결측이 아니다")
    void keepsNotAvailable() {
        CrawlerItem directOnly = first(item ->
                item.deliveryFee() != null
                        && "NOT_AVAILABLE".equals(item.deliveryFee().status()));

        AiDeliveryFeeResponse fee = CrawlerItemMapper
                .toProduct(directOnly, ProductCategory.EARPHONES).deliveryFee();

        assertThat(fee.status())
                .as("«판매자가 택배를 받지 않는다»는 사실이지 «모른다»가 아니다")
                .isEqualTo(DeliveryStatus.NOT_AVAILABLE);
        assertThat(fee.minFee()).isNull();
    }

    @Test
    @DisplayName("택배사와 배송 수단이 계약 enum 으로 옮겨진다")
    void mapsCarrierAndMethod() {
        List<AiDeliveryFeeResponse> fees = crawled.itemsOrEmpty().stream()
                .map(item -> CrawlerItemMapper.toProduct(item, ProductCategory.EARPHONES))
                .map(AiProductResponse::deliveryFee)
                .filter(java.util.Objects::nonNull)
                .toList();

        assertThat(fees).isNotEmpty();
        assertThat(fees).allSatisfy(fee -> assertThat(fee.options()).allSatisfy(option -> {
            assertThat(option.method()).isInstanceOf(DeliveryMethod.class);
            if (option.carrier() != null) {
                assertThat(option.carrier()).isInstanceOf(DeliveryCarrier.class);
            }
        }));
    }

    @Test
    @DisplayName("모르는 배송 수단은 UNKNOWN 이고, 모르는 택배사는 비운다")
    void unknownDeliveryValues() {
        CrawlerItem sample = crawled.itemsOrEmpty().getFirst();
        CrawlerDeliveryFee weird = new CrawlerDeliveryFee(
                "AVAILABLE", "BUYER", 3_000L, 3_000L,
                List.of(new CrawlerDeliveryFee.Option(
                        "DRONE", "KAKAO_T", false, 3_000L, null, "X")),
                null
        );
        CrawlerItem item = new CrawlerItem(
                sample.platform(), sample.platformProductId(), sample.url(), sample.title(),
                sample.description(), sample.price(), sample.conditionLevel(),
                sample.tradeMethod(), weird, sample.images(), sample.location()
        );

        AiDeliveryFeeResponse fee = CrawlerItemMapper
                .toProduct(item, ProductCategory.EARPHONES).deliveryFee();

        assertThat(fee.options().getFirst().method()).isEqualTo(DeliveryMethod.UNKNOWN);
        assertThat(fee.options().getFirst().carrier())
                .as("계약에 없는 택배사를 지어내지 않는다")
                .isNull();
    }

    @Test
    @DisplayName("위치가 있는 매물은 주소가 함께 넘어간다")
    void keepsLocation() {
        CrawlerItem located = first(item ->
                item.location() != null && item.location().fullAddress() != null);

        assertThat(CrawlerItemMapper.toProduct(located, ProductCategory.EARPHONES)
                .location().fullAddress()).isNotBlank();
    }

    @Test
    @DisplayName("카테고리는 파일 하나에 하나 — 넘겨준 값이 모든 상품에 붙는다")
    void categoryIsPerFile() {
        Map<ProductCategory, Long> byCategory = crawled.itemsOrEmpty().stream()
                .map(item -> CrawlerItemMapper.toProduct(item, ProductCategory.EARPHONES))
                .collect(java.util.stream.Collectors.groupingBy(
                        AiProductResponse::category, java.util.stream.Collectors.counting()));

        assertThat(byCategory).hasSize(1).containsKey(ProductCategory.EARPHONES);
    }
}
