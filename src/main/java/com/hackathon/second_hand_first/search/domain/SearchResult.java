package com.hackathon.second_hand_first.search.domain;

import com.hackathon.second_hand_first.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "search_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_search_results_session_product",
                        columnNames = {"search_session_id", "product_id"}
                ),
                @UniqueConstraint(
                        name = "uk_search_results_session_rank",
                        columnNames = {"search_session_id", "recommendation_rank"}
                )
        },
        indexes = {
                @Index(name = "idx_search_results_session_rank", columnList = "search_session_id,recommendation_rank")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_session_id", nullable = false)
    private SearchSession searchSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "recommendation_rank", nullable = false)
    private int rank;

    @Column(name = "recommendation_score")
    private Double recommendationScore;

    @Column(name = "recommendation_reason", length = 1_000)
    private String recommendationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SearchResult(
            SearchSession searchSession,
            Product product,
            int rank,
            Double recommendationScore,
            String recommendationReason
    ) {
        if (searchSession == null) {
            throw new IllegalArgumentException("검색 세션은 필수입니다.");
        }
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("추천 순위는 1 이상이어야 합니다.");
        }
        if (recommendationScore != null && recommendationScore < 0) {
            throw new IllegalArgumentException("추천 점수는 0 이상이어야 합니다.");
        }
        this.searchSession = searchSession;
        this.product = product;
        this.rank = rank;
        this.recommendationScore = recommendationScore;
        this.recommendationReason = normalizeNullableText(recommendationReason, 1_000);
    }

    public static SearchResult create(
            SearchSession searchSession,
            Product product,
            int rank,
            Double recommendationScore,
            String recommendationReason
    ) {
        return new SearchResult(
                searchSession,
                product,
                rank,
                recommendationScore,
                recommendationReason
        );
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
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
