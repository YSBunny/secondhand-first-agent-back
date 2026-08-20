package com.hackathon.second_hand_first.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구매자가 실제로 낼 배송비.
 *
 * <p>총 지불액은 {@code price + 배송비}다. 이 값이 없으면 가격만으로 비교하게 되고,
 * 그러면 배송비가 비싼 매물이 실제보다 싸 보인다.
 *
 * <p>크롤러 통합 스키마의 {@code delivery_fee} 에서 대표 금액 두 개를 가져온다.
 * 정의 원본은 data-analysis/docs/통합_스키마_정의.md 4장이다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryFee {

    /**
     * 모든 배송 수단 중 최저 금액. 편의점 반값택배를 포함한다.
     *
     * <p>모르면 null 이다. <b>0으로 채우면 안 된다</b> — "배송비를 모른다"와
     * "무료배송"이 구분되지 않아 그 매물이 총액 1위로 올라간다.
     */
    @Column(name = "delivery_min_fee")
    private Long minFee;

    /**
     * 편의점 픽업이 필요 없는 수단 중 최저 금액.
     *
     * <p>null 이면 <b>편의점 픽업 외에 받을 방법이 없다</b>는 뜻이다. 주변에 그
     * 편의점이 없는 사용자에게는 사실상 구매 불가다.
     *
     * <p>minFee 와 나눠 둔 이유가 이것이다. 편의점 택배가 가장 싸지만 모두가
     * 쓸 수 있는 선택지는 아니다. 실측 140건 중 12건에서 두 값이 달랐다.
     */
    @Column(name = "delivery_home_fee")
    private Long homeFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_payer", length = 20)
    private DeliveryPayer payer;

    private DeliveryFee(Long minFee, Long homeFee, DeliveryPayer payer) {
        this.minFee = minFee;
        this.homeFee = homeFee;
        this.payer = payer;
    }

    public static DeliveryFee of(Long minFee, Long homeFee, DeliveryPayer payer) {
        return new DeliveryFee(minFee, homeFee, payer);
    }

    /** 택배로 받을 수 없는 매물. 직거래 전용이거나 배송 정보가 없다. */
    public static DeliveryFee unavailable() {
        return new DeliveryFee(null, null, null);
    }

    /** 편의점 픽업 외에 방법이 없는가. */
    public boolean requiresPickupPoint() {
        return minFee != null && homeFee == null;
    }
}
