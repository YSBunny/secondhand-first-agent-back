package com.hackathon.second_hand_first.product.domain;

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
        name = "product_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_images_product_order",
                        columnNames = {"product_id", "display_order"}
                )
        },
        indexes = {
                @Index(name = "idx_product_images_product_id", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 1_000)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ProductImage(Product product, String imageUrl, int displayOrder) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("상품 이미지 URL은 필수입니다.");
        }
        if (imageUrl.trim().length() > 1_000) {
            throw new IllegalArgumentException("상품 이미지 URL은 1000자를 넘을 수 없습니다.");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("이미지 노출 순서는 0 이상이어야 합니다.");
        }
        this.product = product;
        this.imageUrl = imageUrl.trim();
        this.displayOrder = displayOrder;
    }

    static ProductImage create(Product product, String imageUrl, int displayOrder) {
        return new ProductImage(product, imageUrl, displayOrder);
    }

    void updateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("상품 이미지 URL은 필수입니다.");
        }
        String normalized = imageUrl.trim();
        if (normalized.length() > 1_000) {
            throw new IllegalArgumentException("상품 이미지 URL은 1000자를 넘을 수 없습니다.");
        }
        this.imageUrl = normalized;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
