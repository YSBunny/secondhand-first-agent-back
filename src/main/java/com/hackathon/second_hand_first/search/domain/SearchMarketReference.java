package com.hackathon.second_hand_first.search.domain;

import com.hackathon.second_hand_first.product.domain.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "search_market_references")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchMarketReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_session_id", nullable = false, unique = true)
    private SearchSession searchSession;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", nullable = false, length = 30)
    private Platform sourcePlatform;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "reference_type", nullable = false, length = 100)
    private String referenceType;

    @Column(name = "median_price", nullable = false)
    private long medianPrice;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "source_url", length = 1_000)
    private String sourceUrl;

    private SearchMarketReference(
            SearchSession searchSession,
            String productName,
            Platform sourcePlatform,
            String sourceName,
            String referenceType,
            long medianPrice,
            int sampleCount,
            LocalDateTime calculatedAt,
            String sourceUrl
    ) {
        if (searchSession == null || sourcePlatform == null || calculatedAt == null) {
            throw new IllegalArgumentException("시세 기준의 세션, 플랫폼, 계산 시각은 필수입니다.");
        }
        if (medianPrice < 0 || sampleCount < 1) {
            throw new IllegalArgumentException("시세 가격은 0 이상이고 표본 수는 1 이상이어야 합니다.");
        }
        this.searchSession = searchSession;
        this.productName = requireText(productName, "기준 상품명은 필수입니다.", 255);
        this.sourcePlatform = sourcePlatform;
        this.sourceName = requireText(sourceName, "시세 출처명은 필수입니다.", 100);
        this.referenceType = requireText(referenceType, "시세 기준 유형은 필수입니다.", 100);
        this.medianPrice = medianPrice;
        this.sampleCount = sampleCount;
        this.calculatedAt = calculatedAt;
        this.sourceUrl = normalizeNullableText(sourceUrl, 1_000);
    }

    public static SearchMarketReference create(
            SearchSession searchSession,
            String productName,
            Platform sourcePlatform,
            String sourceName,
            String referenceType,
            long medianPrice,
            int sampleCount,
            LocalDateTime calculatedAt,
            String sourceUrl
    ) {
        return new SearchMarketReference(
                searchSession,
                productName,
                sourcePlatform,
                sourceName,
                referenceType,
                medianPrice,
                sampleCount,
                calculatedAt,
                sourceUrl
        );
    }

    private static String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("입력값은 " + maxLength + "자를 넘을 수 없습니다.");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, "", maxLength);
    }
}
