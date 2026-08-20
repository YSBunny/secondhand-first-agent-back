package com.hackathon.second_hand_first.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "product_deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeliveryPayer payer;

    @Column(name = "min_fee")
    private Long minFee;

    @Column(name = "home_delivery_fee")
    private Long homeDeliveryFee;

    @Column(name = "jeju_fee")
    private Long jejuFee;

    @Column(name = "remote_area_fee")
    private Long remoteAreaFee;

    @Column(name = "extra_cost_description", length = 1_000)
    private String extraCostDescription;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private final List<ProductDeliveryOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProductDelivery(
            Product product,
            DeliveryStatus status,
            DeliveryPayer payer,
            Long minFee,
            Long homeDeliveryFee,
            Long jejuFee,
            Long remoteAreaFee,
            String extraCostDescription
    ) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("배송 상태는 필수입니다.");
        }
        this.product = product;
        this.status = status;
        this.payer = payer;
        this.minFee = requireNullableNonNegative(minFee, "최저 배송비");
        this.homeDeliveryFee = requireNullableNonNegative(homeDeliveryFee, "일반 배송비");
        this.jejuFee = requireNullableNonNegative(jejuFee, "제주 추가 배송비");
        this.remoteAreaFee = requireNullableNonNegative(remoteAreaFee, "도서산간 추가 배송비");
        this.extraCostDescription = normalizeNullableText(extraCostDescription, 1_000);
    }

    public static ProductDelivery create(
            Product product,
            DeliveryStatus status,
            DeliveryPayer payer,
            Long minFee,
            Long homeDeliveryFee,
            Long jejuFee,
            Long remoteAreaFee,
            String extraCostDescription
    ) {
        return new ProductDelivery(
                product,
                status,
                payer,
                minFee,
                homeDeliveryFee,
                jejuFee,
                remoteAreaFee,
                extraCostDescription
        );
    }

    /**
     * 기존 행의 값을 갱신한다. <b>새 행을 만들어 갈아끼우지 않는다.</b>
     *
     * <p>{@code product_deliveries.product_id} 에 유니크 제약이 있어, 새 인스턴스를
     * 연결하면 Hibernate 가 <b>옛 행을 지우기 전에 새 행을 넣어</b> 제약을 위반한다.
     * 같은 상품을 다시 적재할 때마다 500 이 났다.
     */
    public void update(
            DeliveryStatus status,
            DeliveryPayer payer,
            Long minFee,
            Long homeDeliveryFee,
            Long jejuFee,
            Long remoteAreaFee,
            String extraCostDescription
    ) {
        if (status == null) {
            throw new IllegalArgumentException("배송 상태는 필수입니다.");
        }
        this.status = status;
        this.payer = payer;
        this.minFee = requireNullableNonNegative(minFee, "최저 배송비");
        this.homeDeliveryFee = requireNullableNonNegative(homeDeliveryFee, "일반 배송비");
        this.jejuFee = requireNullableNonNegative(jejuFee, "제주 추가 배송비");
        this.remoteAreaFee = requireNullableNonNegative(remoteAreaFee, "도서산간 추가 배송비");
        this.extraCostDescription = normalizeNullableText(extraCostDescription, 1_000);
        // 옵션은 매번 다시 만든다. 배송 수단이 바뀌면 옛 옵션이 남으면 안 된다.
        this.options.clear();
    }

    public ProductDeliveryOption addOption(
            DeliveryMethod method,
            DeliveryCarrier carrier,
            Boolean requiresPickupPoint,
            Long fee,
            String rawCodeJson,
            int displayOrder
    ) {
        ProductDeliveryOption option = ProductDeliveryOption.create(
                this,
                method,
                carrier,
                requiresPickupPoint,
                fee,
                rawCodeJson,
                displayOrder
        );
        options.add(option);
        return option;
    }

    public List<ProductDeliveryOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    private static Long requireNullableNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + "는 0 이상이어야 합니다.");
        }
        return value;
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

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
