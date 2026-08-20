package com.hackathon.second_hand_first.carbon.service;

import com.hackathon.second_hand_first.carbon.dto.CarbonSavingResult;
import com.hackathon.second_hand_first.carbon.infrastructure.ClimatiqClient;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonSavingServiceTest {

    @Mock
    private ClimatiqClient climatiqClient;

    private CarbonSavingService service;

    @BeforeEach
    void setUp() {
        service = new CarbonSavingService(climatiqClient);
    }

    // 테스트 1: 에어팟 → EPA WARM, 약 2.04 kg CO2e
    @Test
    void 에어팟_WARM_경로_계산() {
        CarbonSavingResult result = service.calculate(
                "AirPods Pro 2 (USB-C)",
                ProductCategory.EARPHONES,
                180_000L,
                Platform.NAVER_FLEAMARKET,
                ProductCondition.LIKE_NEW
        );

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.source()).isEqualTo("EPA_WARM");
        assertThat(result.co2eKg()).isCloseTo(2.04, offset(0.01));
    }

    // 테스트 2: 닌텐도 스위치 → EPA WARM, 약 9.66 kg CO2e
    @Test
    void 닌텐도_스위치_WARM_경로_계산() {
        CarbonSavingResult result = service.calculate(
                "닌텐도 스위치 OLED 화이트",
                ProductCategory.GAME_CONSOLE,
                200_000L,
                Platform.NAVER_FLEAMARKET,
                ProductCondition.LIGHTLY_USED
        );

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.source()).isEqualTo("EPA_WARM");
        assertThat(result.co2eKg()).isCloseTo(9.66, offset(0.01));
    }

    // 테스트 3: 테이블에 없는 상품 + API 키 없음 → Climatiq 경로, NOT_AVAILABLE 반환
    @Test
    void 테이블_없는_상품_API키_없으면_NOT_AVAILABLE() {
        // 가구는 ISIC4 31 이다. 전자기기(26)로 보내면 틀린 배출량이 나온다.
        when(climatiqClient.estimate(50_000L, "consumer_goods-type_upholstered_household_furniture-price_purchaser"))
                .thenReturn(CarbonSavingResult.notAvailable("API_ERROR"));

        CarbonSavingResult result = service.calculate(
                "책상",
                ProductCategory.FURNITURE,
                50_000L,
                Platform.NAVER_FLEAMARKET,
                ProductCondition.LIGHTLY_USED
        );

        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        verify(climatiqClient, times(1)).estimate(50_000L, "consumer_goods-type_upholstered_household_furniture-price_purchaser");
    }

    // 테스트 3-1: OTHER 는 ISIC4 매핑이 없다 — Climatiq 을 부르지 않는다
    @Test
    void category_OTHER_이면_Climatiq_미호출() {
        CarbonSavingResult result = service.calculate(
                "정체를 알 수 없는 물건",
                ProductCategory.OTHER,
                50_000L,
                Platform.NAVER_FLEAMARKET,
                ProductCondition.LIGHTLY_USED
        );

        // OTHER 는 "분류하지 못했다"는 뜻이라 특정 품목군이 아니다.
        // 임의의 코드로 계산하면 틀린 배출량이 조용히 나온다.
        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        assertThat(result.reason()).isEqualTo("NO_CATEGORY_MAPPING");
        verifyNoInteractions(climatiqClient);
    }

    // 테스트 3-2: 비전자기기 카테고리가 각자의 배출계수로 간다
    @Test
    void 카테고리별_배출계수로_보낸다() {
        record Case(ProductCategory category, String activityId) { }
        // 전부 한국(KR) CEDA 2025 계수이며 구매자가격 기준이다.
        // 우리가 넣는 price 는 소비자가 실제로 내는 값이라, 생산자가격 계수를 쓰면
        // 유통 마진만큼 배출량이 과대평가된다.
        List<Case> cases = List.of(
                new Case(ProductCategory.CLOTHING,
                        "general_retail-type_clothing_and_clothing_accessories_stores-price_purchaser"),
                new Case(ProductCategory.BAG_SHOES,
                        "consumer_goods-type_leather_and_related_product_manufacturing-price_purchaser"),
                new Case(ProductCategory.FURNITURE,
                        "consumer_goods-type_upholstered_household_furniture-price_purchaser"),
                new Case(ProductCategory.SPORTS_TOYS,
                        "consumer_goods-type_sporting_and_athletic_goods_manufacturing-price_purchaser"),
                new Case(ProductCategory.BOOKS,
                        "paper_products-type_book_publishers-price_purchaser"),
                new Case(ProductCategory.WATCH_JEWELRY,
                        "consumer_goods-type_jewelry_and_silverware_manufacturing-price_purchaser")
        );
        for (Case each : cases) {
            when(climatiqClient.estimate(10_000L, each.activityId()))
                    .thenReturn(CarbonSavingResult.available(1.0, "CLIMATIQ"));

            CarbonSavingResult result = service.calculate(
                    "상품-" + each.category(),
                    each.category(),
                    10_000L,
                    Platform.BUNJANG,
                    ProductCondition.USED
            );

            assertThat(result.status()).isEqualTo("AVAILABLE");
        }

        // 카테고리마다 계수가 다르므로 이제 하나씩 정확히 확인할 수 있다.
        // ISIC4 시절에는 SPORTS_TOYS 와 WATCH_JEWELRY 가 둘 다 32 라 구분되지 않았다.
        for (Case each : cases) {
            verify(climatiqClient, times(1)).estimate(10_000L, each.activityId());
        }
    }

    // 테스트 4: category가 null → NO_CATEGORY_MAPPING, Climatiq 미호출
    @Test
    void category_null_이면_Climatiq_미호출() {
        CarbonSavingResult result = service.calculate(
                "알 수 없는 상품",
                null,
                30_000L,
                Platform.BUNJANG,
                ProductCondition.USED
        );

        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        assertThat(result.reason()).isEqualTo("NO_CATEGORY_MAPPING");
        verify(climatiqClient, never()).estimate(30_000L, "electronics-type_electronic_computer-price_purchaser");
    }

    // 테스트 5: ELEVENST + NEW → NOT_APPLICABLE
    @Test
    void 일레브는트_새상품_NOT_APPLICABLE() {
        CarbonSavingResult result = service.calculate(
                "AirPods Pro 2",
                ProductCategory.EARPHONES,
                250_000L,
                Platform.ELEVENST,
                ProductCondition.NEW
        );

        assertThat(result.status()).isEqualTo("NOT_APPLICABLE");
        assertThat(result.co2eKg()).isNull();
    }

    // 테스트 6: ELEVENST + USED → 정상 계산 (에어팟 WARM 경로)
    @Test
    void 일레브는트_중고_정상_계산() {
        CarbonSavingResult result = service.calculate(
                "에어팟 프로 2세대",
                ProductCategory.EARPHONES,
                150_000L,
                Platform.ELEVENST,
                ProductCondition.USED
        );

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.source()).isEqualTo("EPA_WARM");
        assertThat(result.co2eKg()).isCloseTo(2.04, offset(0.01));
    }

    // 테스트 7: 캐시 동작 — 같은 입력으로 2번 호출 시 Climatiq는 1번만 호출
    @Test
    void 캐시_동작_두번째_호출은_API_미호출() {
        when(climatiqClient.estimate(45_000L, "electronics-type_electronic_computer-price_purchaser"))
                .thenReturn(CarbonSavingResult.notAvailable("API_ERROR"));

        service.calculate("노트북", ProductCategory.LAPTOP, 45_000L, Platform.NAVER_FLEAMARKET, ProductCondition.LIGHTLY_USED);
        service.calculate("노트북", ProductCategory.LAPTOP, 45_000L, Platform.NAVER_FLEAMARKET, ProductCondition.LIGHTLY_USED);

        verify(climatiqClient, times(1)).estimate(45_000L, "electronics-type_electronic_computer-price_purchaser");
    }
}