package com.hackathon.second_hand_first.carbon.service;

import com.hackathon.second_hand_first.carbon.dto.CarbonSavingResult;
import com.hackathon.second_hand_first.carbon.infrastructure.ClimatiqClient;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
                Platform.DAANGN,
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
                Platform.DAANGN,
                ProductCondition.GOOD
        );

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.source()).isEqualTo("EPA_WARM");
        assertThat(result.co2eKg()).isCloseTo(9.66, offset(0.01));
    }

    // 테스트 3: 테이블에 없는 상품 + API 키 없음 → Climatiq 경로, NOT_AVAILABLE 반환
    @Test
    void 테이블_없는_상품_API키_없으면_NOT_AVAILABLE() {
        when(climatiqClient.estimate(50_000L, 26))
                .thenReturn(CarbonSavingResult.notAvailable("API_ERROR"));

        CarbonSavingResult result = service.calculate(
                "책상",
                ProductCategory.OTHER,
                50_000L,
                Platform.DAANGN,
                ProductCondition.GOOD
        );

        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        verify(climatiqClient, times(1)).estimate(50_000L, 26);
    }

    // 테스트 4: category가 null → NO_CATEGORY_MAPPING, Climatiq 미호출
    @Test
    void category_null_이면_Climatiq_미호출() {
        CarbonSavingResult result = service.calculate(
                "알 수 없는 상품",
                null,
                30_000L,
                Platform.BUNGJANG,
                ProductCondition.USED
        );

        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        assertThat(result.reason()).isEqualTo("NO_CATEGORY_MAPPING");
        verify(climatiqClient, never()).estimate(30_000L, 26);
    }

    // 테스트 5: ELEVENST + NEW → NOT_APPLICABLE
    @Test
    void 일레브는트_새상품_NOT_APPLICABLE() {
        CarbonSavingResult result = service.calculate(
                "AirPods Pro 2",
                ProductCategory.EARPHONES,
                250_000L,
                Platform.ELEVENST,
                ProductCondition.UNOPENED
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
        when(climatiqClient.estimate(45_000L, 26))
                .thenReturn(CarbonSavingResult.notAvailable("API_ERROR"));

        service.calculate("노트북", ProductCategory.LAPTOP, 45_000L, Platform.DAANGN, ProductCondition.GOOD);
        service.calculate("노트북", ProductCategory.LAPTOP, 45_000L, Platform.DAANGN, ProductCondition.GOOD);

        verify(climatiqClient, times(1)).estimate(45_000L, 26);
    }
}