package com.hackathon.second_hand_first.product.domain;

/** 배송비 부담 주체. 판단할 수 없는 경우에는 enum을 만들지 않고 null을 사용한다. */
public enum DeliveryPayer {
    SELLER,
    BUYER
}
