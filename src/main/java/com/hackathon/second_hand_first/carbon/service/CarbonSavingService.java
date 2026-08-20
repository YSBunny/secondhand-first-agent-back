package com.hackathon.second_hand_first.carbon.service;

import com.hackathon.second_hand_first.carbon.dto.CarbonSavingResult;
import com.hackathon.second_hand_first.carbon.infrastructure.ClimatiqClient;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CarbonSavingService {

    // EPA WARM Source Reduction 계수 (MTCO2E/숏톤), 숏톤 = 907.185 kg
    private static final double WARM_PORTABLE_ELECTRONICS = 29.83; // 노트북·스마트폰·이어폰류 (절댓값)
    private static final double WARM_FLAT_PANEL_DISPLAYS = 24.19;  // LED/LCD 모니터·TV
    private static final double WARM_DESKTOP_CPUS = 20.86;         // 게임 콘솔 proxy (EPA 공식)
    private static final double WARM_MIXED_ELECTRONICS = 20.79;    // 구성 불명 또는 기타 전자기기

    // 상품명 키워드 → (WARM 계수, 무게 kg) 매핑
    // 무게 기준: 에어팟 = 이어버드 2개(약 11g) + 케이스(약 51g), 스위치 = 본체 + 조이콘
    // 에어팟: EPA 공식 분류 없음 — Portable Electronic Devices 는 팀 자체 추정 (EPA 비공식)
    private static final Map<String, WarmEntry> WEIGHT_TABLE = Map.of(
            "에어팟", new WarmEntry(WARM_PORTABLE_ELECTRONICS, 0.062),
            "airpod", new WarmEntry(WARM_PORTABLE_ELECTRONICS, 0.062),
            "닌텐도 스위치", new WarmEntry(WARM_DESKTOP_CPUS, 0.42),
            "nintendo switch", new WarmEntry(WARM_DESKTOP_CPUS, 0.42)
    );

    /**
     * 카테고리 → Climatiq 배출계수 식별자.
     *
     * <p><b>ISIC4 코드 표를 대체한 것이다.</b> ISIC4 를 받는 {@code /procurement/v1/spend}
     * 가 유료 전용이라 무료 키로는 403 이 온다. 무료로 되는 {@code /data/v1/estimate} 는
     * 계수를 직접 지정해야 해서, 코드가 아니라 activity_id 를 들고 있는다.
     *
     * <p>전부 <b>한국(KR) CEDA 2025</b> 계수이며, 실제 호출로 값이 나오는 것을 확인했다.
     *
     * <p><b>{@code -price_purchaser} 를 쓴다.</b> 같은 품목에 생산자가격 계수와
     * 구매자가격 계수가 따로 있는데, 우리가 넣는 {@code price} 는 소비자가 실제로 내는
     * 값이다. 생산자가격 계수를 쓰면 유통 마진만큼 배출량이 과대평가된다
     * (5만원 기준 가구 18.3kg 대 14.5kg).
     *
     * <p><b>OTHER 는 일부러 넣지 않는다.</b> "분류하지 못했다"는 뜻이라 특정 품목군이
     * 아니다. 예전에 전자기기로 매핑돼 있어 책상이나 의류가 전자기기로 계산되던 적이
     * 있었다. 매핑이 없으면 NO_CATEGORY_MAPPING 으로 정직하게 실패한다.
     */
    /**
     * 전자기기 공통 계수. 이 경로로 오는 것은 <b>무게를 모르는 전자기기</b>뿐이다 —
     * 아는 것은 앞 단계에서 EPA WARM 무게 테이블로 계산되고 여기까지 오지 않는다.
     */
    private static final String ELECTRONICS_ACTIVITY_ID =
            "electronics-type_electronic_computer-price_purchaser";

    private static final Map<ProductCategory, String> ACTIVITY_ID_TABLE = Map.ofEntries(
            // 전자기기 — WARM 무게 테이블에 없는 것만 여기로 온다
            Map.entry(ProductCategory.EARPHONES, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.LAPTOP, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.SMARTPHONE, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.SMARTWATCH, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.TABLET, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.MONITOR, ELECTRONICS_ACTIVITY_ID),
            Map.entry(ProductCategory.GAME_CONSOLE, ELECTRONICS_ACTIVITY_ID),
            // 그 외
            Map.entry(ProductCategory.CLOTHING,
                    "general_retail-type_clothing_and_clothing_accessories_stores-price_purchaser"),
            Map.entry(ProductCategory.BAG_SHOES,
                    "consumer_goods-type_leather_and_related_product_manufacturing-price_purchaser"),
            Map.entry(ProductCategory.FURNITURE,
                    "consumer_goods-type_upholstered_household_furniture-price_purchaser"),
            Map.entry(ProductCategory.SPORTS_TOYS,
                    "consumer_goods-type_sporting_and_athletic_goods_manufacturing-price_purchaser"),
            Map.entry(ProductCategory.BOOKS,
                    "paper_products-type_book_publishers-price_purchaser"),
            Map.entry(ProductCategory.WATCH_JEWELRY,
                    "consumer_goods-type_jewelry_and_silverware_manufacturing-price_purchaser")
    );

    private final ClimatiqClient climatiqClient;
    // 프로세스 내 캐시 — 해커톤 규모에서 Climatiq 무료 한도(월 2,500회)를 보호
    private final Map<String, CarbonSavingResult> cache = new ConcurrentHashMap<>();

    public CarbonSavingService(ClimatiqClient climatiqClient) {
        this.climatiqClient = climatiqClient;
    }

    public CarbonSavingResult calculate(
            String productName,
            ProductCategory category,
            long price,
            Platform platform,
            ProductCondition condition
    ) {
        // 0단계: 새상품 판정 — ELEVENST + 중고 아님 → 절감 대상 아님
        if (platform == Platform.ELEVENST && condition != ProductCondition.USED) {
            return CarbonSavingResult.notApplicable();
        }

        String cacheKey = buildCacheKey(productName, category, price, platform);
        CarbonSavingResult cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        CarbonSavingResult result = doCalculate(productName, category, price);
        cache.put(cacheKey, result);
        return result;
    }

    private CarbonSavingResult doCalculate(
            String productName,
            ProductCategory category,
            long price
    ) {
        // 1단계: 상품명이 무게 테이블에 있으면 EPA WARM 경로
        if (productName != null) {
            String lowerName = productName.toLowerCase();
            for (Map.Entry<String, WarmEntry> entry : WEIGHT_TABLE.entrySet()) {
                if (lowerName.contains(entry.getKey())) {
                    double co2eKg = warmCalculate(entry.getValue());
                    return CarbonSavingResult.available(co2eKg, "EPA_WARM");
                }
            }
        }

        // 2단계: category → 배출계수 → Climatiq API
        if (category == null) {
            return CarbonSavingResult.notAvailable("NO_CATEGORY_MAPPING");
        }
        String activityId = ACTIVITY_ID_TABLE.get(category);
        if (activityId == null) {
            return CarbonSavingResult.notAvailable("NO_CATEGORY_MAPPING");
        }
        return climatiqClient.estimate(price, activityId);
    }

    // co2e_kg = |계수| × (무게kg / 907.185) × 1000
    private double warmCalculate(WarmEntry entry) {
        return entry.warmCoefficient() * (entry.weightKg() / 907.185) * 1000;
    }

    private String buildCacheKey(
            String productName,
            ProductCategory category,
            long price,
            Platform platform
    ) {
        return platform + "|" + category + "|" + price + "|" + (productName == null ? "" : productName.toLowerCase());
    }

    record WarmEntry(double warmCoefficient, double weightKg) {}
}