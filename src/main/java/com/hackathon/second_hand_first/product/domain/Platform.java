package com.hackathon.second_hand_first.product.domain;

/**
 * 수집 대상 플랫폼.
 *
 * <p>값은 크롤러가 내보내는 통합 스키마와 동일하게 맞춘다.
 * 정의 원본은 data-analysis/docs/통합_스키마_정의.md 이며,
 * 이 enum이 그와 어긋나면 크롤러 데이터를 그대로 받을 수 없다.
 *
 * <p>앞의 셋은 중고, {@link #ELEVENST}는 새상품 비교 기준이다.
 * 당근마켓은 수집 대상이 아니다.
 */
public enum Platform {
    BUNJANG,
    JOONGNA,
    NAVER_FLEAMARKET,
    ELEVENST
}
