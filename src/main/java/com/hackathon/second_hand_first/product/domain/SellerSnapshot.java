package com.hackathon.second_hand_first.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "seller_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "external_seller_id", length = 255)
    private String externalSellerId;

    @Column(name = "seller_name", length = 100)
    private String sellerName;

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(name = "manner_temperature")
    private Double mannerTemperature;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    private SellerSnapshot(
            Product product,
            String externalSellerId,
            String sellerName,
            int trustScore,
            int tradeCount,
            Double mannerTemperature,
            LocalDateTime capturedAt
    ) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
        this.product = product;
        update(externalSellerId, sellerName, trustScore, tradeCount, mannerTemperature, capturedAt);
    }

    static SellerSnapshot create(
            Product product,
            String externalSellerId,
            String sellerName,
            int trustScore,
            int tradeCount,
            Double mannerTemperature,
            LocalDateTime capturedAt
    ) {
        return new SellerSnapshot(
                product,
                externalSellerId,
                sellerName,
                trustScore,
                tradeCount,
                mannerTemperature,
                capturedAt
        );
    }

    void update(
            String externalSellerId,
            String sellerName,
            int trustScore,
            int tradeCount,
            Double mannerTemperature,
            LocalDateTime capturedAt
    ) {
        if (trustScore < 0 || trustScore > 100) {
            throw new IllegalArgumentException("판매자 신뢰도는 0에서 100 사이여야 합니다.");
        }
        if (tradeCount < 0) {
            throw new IllegalArgumentException("판매자 거래 횟수는 0 이상이어야 합니다.");
        }
        if (mannerTemperature != null && mannerTemperature < 0) {
            throw new IllegalArgumentException("판매자 매너온도는 0 이상이어야 합니다.");
        }
        if (capturedAt == null) {
            throw new IllegalArgumentException("판매자 정보 수집 시각은 필수입니다.");
        }
        this.externalSellerId = normalizeNullableText(externalSellerId, 255);
        this.sellerName = normalizeNullableText(sellerName, 100);
        this.trustScore = trustScore;
        this.tradeCount = tradeCount;
        this.mannerTemperature = mannerTemperature;
        this.capturedAt = capturedAt;
    }

    private static String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("입력값은 " + maxLength + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }
}
