package com.hackathon.second_hand_first.product.domain;

/**
 * 상품 상태.
 *
 * <p>값은 크롤러가 내보내는 통합 스키마의 condition_level과 동일하게 맞춘다.
 * 정의 원본은 data-analysis/docs/통합_스키마_정의.md 3장이다.
 *
 * <p>{@link #UNSPECIFIED}와 {@link #UNKNOWN}은 "상태가 나쁘다"는 뜻이 아니다.
 * 전자는 판매자가 적지 않은 것이고 후자는 우리가 해석하지 못한 것이다.
 * 이 둘을 {@link #USED}로 뭉뚱그리면 모르는 것을 나쁜 상태로 단정하는 셈이 된다.
 */
public enum ProductCondition {
    NEW,
    LIKE_NEW,
    LIGHTLY_USED,
    USED,
    UNSPECIFIED,
    UNKNOWN
}
