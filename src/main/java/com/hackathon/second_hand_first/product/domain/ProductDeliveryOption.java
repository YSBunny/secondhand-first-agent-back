package com.hackathon.second_hand_first.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "product_delivery_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_delivery_options_order",
                columnNames = {"delivery_id", "display_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDeliveryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private ProductDelivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryMethod method;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeliveryCarrier carrier;

    @Column(name = "requires_pickup_point")
    private Boolean requiresPickupPoint;

    @Column
    private Long fee;

    @Column(name = "raw_code_json", length = 1_000)
    private String rawCodeJson;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private ProductDeliveryOption(
            ProductDelivery delivery,
            DeliveryMethod method,
            DeliveryCarrier carrier,
            Boolean requiresPickupPoint,
            Long fee,
            String rawCodeJson,
            int displayOrder
    ) {
        if (delivery == null) {
            throw new IllegalArgumentException("배송 정보는 필수입니다.");
        }
        if (method == null) {
            throw new IllegalArgumentException("배송 방식은 필수입니다.");
        }
        if (fee != null && fee < 0) {
            throw new IllegalArgumentException("배송비는 0 이상이어야 합니다.");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("배송 옵션 순서는 0 이상이어야 합니다.");
        }
        this.delivery = delivery;
        this.method = method;
        this.carrier = carrier;
        this.requiresPickupPoint = requiresPickupPoint;
        this.fee = fee;
        this.rawCodeJson = normalizeNullableText(rawCodeJson, 1_000);
        this.displayOrder = displayOrder;
    }

    static ProductDeliveryOption create(
            ProductDelivery delivery,
            DeliveryMethod method,
            DeliveryCarrier carrier,
            Boolean requiresPickupPoint,
            Long fee,
            String rawCodeJson,
            int displayOrder
    ) {
        return new ProductDeliveryOption(
                delivery,
                method,
                carrier,
                requiresPickupPoint,
                fee,
                rawCodeJson,
                displayOrder
        );
    }

    private static String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("입력값은 " + maxLength + "자를 넘을 수 없습니다.");
        }
        return normalized;
    }
}
