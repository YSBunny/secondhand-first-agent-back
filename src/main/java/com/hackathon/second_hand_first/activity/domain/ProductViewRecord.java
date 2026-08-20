package com.hackathon.second_hand_first.activity.domain;

import com.hackathon.second_hand_first.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "product_view_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_views_user_product_date",
                columnNames = {"user_id", "product_id", "viewed_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductViewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "viewed_date", nullable = false)
    private LocalDate viewedDate;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(nullable = false)
    private boolean counted;

    private ProductViewRecord(
            Long userId,
            Product product,
            LocalDate viewedDate,
            LocalDateTime viewedAt,
            boolean counted
    ) {
        if (userId == null || userId <= 0 || product == null || viewedDate == null || viewedAt == null) {
            throw new IllegalArgumentException("상품 조회 기록의 필수 값이 누락되었습니다.");
        }
        this.userId = userId;
        this.product = product;
        this.viewedDate = viewedDate;
        this.viewedAt = viewedAt;
        this.counted = counted;
    }

    public static ProductViewRecord create(
            Long userId,
            Product product,
            LocalDate viewedDate,
            LocalDateTime viewedAt,
            boolean counted
    ) {
        return new ProductViewRecord(userId, product, viewedDate, viewedAt, counted);
    }
}
