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

    // ProductCategory → ISIC4 코드 (Climatiq spend-based 경로)
    private static final Map<ProductCategory, Integer> ISIC4_TABLE = Map.of(
            ProductCategory.EARPHONES, 26,
            ProductCategory.LAPTOP, 26,
            ProductCategory.SMARTPHONE, 26,
            ProductCategory.SMARTWATCH, 26,
            ProductCategory.TABLET, 26,
            ProductCategory.MONITOR, 26,
            ProductCategory.GAME_CONSOLE, 26,
            ProductCategory.OTHER, 26
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

        // 2단계: category → ISIC4 → Climatiq API
        if (category == null) {
            return CarbonSavingResult.notAvailable("NO_CATEGORY_MAPPING");
        }
        Integer isic4 = ISIC4_TABLE.get(category);
        if (isic4 == null) {
            return CarbonSavingResult.notAvailable("NO_CATEGORY_MAPPING");
        }
        return climatiqClient.estimate(price, isic4);
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