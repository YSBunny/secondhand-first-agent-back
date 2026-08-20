package com.hackathon.second_hand_first.product.domain;

/**
 * 상품 카테고리.
 *
 * <p>AI의 parse_query 가 사용자 요청에서 추론해 내려준다. 크롤러 통합 스키마에는
 * 카테고리가 없다 — 3사 체계(1단/3단/가변)를 매핑할 시간이 부족해 제외했다.
 *
 * <p>탄소 절감량 계산이 이 값을 ISIC4 코드로 매핑해 쓴다. 값을 추가하면
 * {@code CarbonSavingService.ISIC4_TABLE} 도 함께 채워야 한다. 매핑이 없으면
 * 그 카테고리는 탄소 계산에서 빠진다.
 *
 * <p>{@link #OTHER}는 <b>"분류하지 못했다"</b>는 뜻이다. 특정 품목군이 아니므로
 * ISIC4 매핑을 두지 않는다. 임의의 코드로 계산하면 틀린 배출량이 조용히 나온다.
 */
public enum ProductCategory {

    // 전자기기 — EPA WARM 계수표로 계산한다(Climatiq 없이도 동작)
    EARPHONES,
    LAPTOP,
    SMARTPHONE,
    SMARTWATCH,
    TABLET,
    MONITOR,
    GAME_CONSOLE,

    // 그 외 — Climatiq spend-based API 로 계산한다
    CLOTHING,
    BAG_SHOES,
    FURNITURE,
    SPORTS_TOYS,
    BOOKS,
    WATCH_JEWELRY,

    /** 분류하지 못한 경우. 탄소 계산 대상이 아니다. */
    OTHER
}
