package com.hackathon.second_hand_first.product.domain;

/**
 * 배송비 부담 주체.
 *
 * <p>크롤러 통합 스키마의 {@code delivery_fee.payer} 와 같은 값이다.
 * 배송 수단마다 부담 주체가 다르면 단정하지 않고 null 로 둔다.
 */
public enum DeliveryPayer {
    SELLER,
    BUYER
}
