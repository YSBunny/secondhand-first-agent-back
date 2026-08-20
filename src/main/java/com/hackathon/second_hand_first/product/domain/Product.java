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
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_platform_external_id",
                        columnNames = {"platform", "external_product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_products_category_status", columnList = "category,status"),
                @Index(name = "idx_products_price", columnList = "price"),
                @Index(name = "idx_products_published_at", columnList = "published_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Platform platform;

    @Column(name = "external_product_id", nullable = false, length = 255)
    private String externalProductId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(nullable = false)
    private long price;

    /**
     * 새상품 기준 가격입니다. 기준가를 알 수 없는 상품은 null입니다.
     */
    @Column(name = "reference_price")
    private Long referencePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 30)
    private ProductCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Column(length = 100)
    private String location;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "direct_trade_available", nullable = false)
    private boolean directTradeAvailable;

    @Column(name = "shipping_available", nullable = false)
    private boolean shippingAvailable;

    @Column(name = "carbon_reduction_eligible", nullable = false)
    private boolean carbonReductionEligible;

    @Column(name = "platform_url", nullable = false, length = 1_000)
    private String platformUrl;

    @Column(name = "external_view_count", nullable = false)
    private long externalViewCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_refreshed_at", nullable = false)
    private LocalDateTime lastRefreshedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private final List<ProductImage> images = new ArrayList<>();

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerSnapshot sellerSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Product(
            Platform platform,
            String externalProductId,
            String title,
            String description,
            ProductCategory category,
            long price,
            Long referencePrice,
            ProductCondition condition,
            ProductStatus status,
            String location,
            boolean directTradeAvailable,
            boolean shippingAvailable,
            boolean carbonReductionEligible,
            String platformUrl,
            long externalViewCount,
            LocalDateTime publishedAt,
            LocalDateTime lastRefreshedAt
    ) {
        this.platform = requireNonNull(platform, "플랫폼은 필수입니다.");
        this.externalProductId = requireText(externalProductId, "외부 상품 ID는 필수입니다.", 255);
        this.title = requireText(title, "상품명은 필수입니다.", 255);
        this.description = normalizeNullableText(description);
        this.category = requireNonNull(category, "상품 카테고리는 필수입니다.");
        this.price = requireNonNegative(price, "상품 가격은 0 이상이어야 합니다.");
        this.referencePrice = requireNullableNonNegative(referencePrice, "기준 가격은 0 이상이어야 합니다.");
        this.condition = requireNonNull(condition, "상품 상태는 필수입니다.");
        this.status = requireNonNull(status, "판매 상태는 필수입니다.");
        this.location = normalizeNullableText(location);
        this.directTradeAvailable = directTradeAvailable;
        this.shippingAvailable = shippingAvailable;
        this.carbonReductionEligible = carbonReductionEligible;
        this.platformUrl = requireText(platformUrl, "플랫폼 상품 URL은 필수입니다.", 1_000);
        this.externalViewCount = requireNonNegative(externalViewCount, "조회수는 0 이상이어야 합니다.");
        this.publishedAt = publishedAt;
        this.lastRefreshedAt = requireNonNull(lastRefreshedAt, "마지막 갱신 시각은 필수입니다.");
    }

    public static Product create(
            Platform platform,
            String externalProductId,
            String title,
            String description,
            ProductCategory category,
            long price,
            Long referencePrice,
            ProductCondition condition,
            ProductStatus status,
            String location,
            boolean directTradeAvailable,
            boolean shippingAvailable,
            boolean carbonReductionEligible,
            String platformUrl,
            long externalViewCount,
            LocalDateTime publishedAt,
            LocalDateTime lastRefreshedAt
    ) {
        return new Product(
                platform,
                externalProductId,
                title,
                description,
                category,
                price,
                referencePrice,
                condition,
                status,
                location,
                directTradeAvailable,
                shippingAvailable,
                carbonReductionEligible,
                platformUrl,
                externalViewCount,
                publishedAt,
                lastRefreshedAt
        );
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public ProductImage addImage(String imageUrl, int displayOrder) {
        if (images.stream().anyMatch(image -> image.getDisplayOrder() == displayOrder)) {
            throw new IllegalArgumentException("같은 이미지 노출 순서를 중복해서 사용할 수 없습니다.");
        }
        ProductImage image = ProductImage.create(this, imageUrl, displayOrder);
        images.add(image);
        return image;
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
    }

    public SellerSnapshot updateSellerSnapshot(
            String externalSellerId,
            String sellerName,
            int trustScore,
            int tradeCount,
            Double mannerTemperature,
            LocalDateTime capturedAt
    ) {
        if (sellerSnapshot == null) {
            sellerSnapshot = SellerSnapshot.create(
                    this,
                    externalSellerId,
                    sellerName,
                    trustScore,
                    tradeCount,
                    mannerTemperature,
                    capturedAt
            );
        } else {
            sellerSnapshot.update(
                    externalSellerId,
                    sellerName,
                    trustScore,
                    tradeCount,
                    mannerTemperature,
                    capturedAt
            );
        }
        return sellerSnapshot;
    }

    public void clearSellerSnapshot() {
        this.sellerSnapshot = null;
    }

    public void refresh(
            String title,
            String description,
            ProductCategory category,
            long price,
            Long referencePrice,
            ProductCondition condition,
            ProductStatus status,
            String location,
            boolean directTradeAvailable,
            boolean shippingAvailable,
            boolean carbonReductionEligible,
            String platformUrl,
            long externalViewCount,
            LocalDateTime publishedAt,
            LocalDateTime lastRefreshedAt
    ) {
        this.title = requireText(title, "상품명은 필수입니다.", 255);
        this.description = normalizeNullableText(description);
        this.category = requireNonNull(category, "상품 카테고리는 필수입니다.");
        this.price = requireNonNegative(price, "상품 가격은 0 이상이어야 합니다.");
        this.referencePrice = requireNullableNonNegative(referencePrice, "기준 가격은 0 이상이어야 합니다.");
        this.condition = requireNonNull(condition, "상품 상태는 필수입니다.");
        this.status = requireNonNull(status, "판매 상태는 필수입니다.");
        this.location = normalizeNullableText(location);
        this.directTradeAvailable = directTradeAvailable;
        this.shippingAvailable = shippingAvailable;
        this.carbonReductionEligible = carbonReductionEligible;
        this.platformUrl = requireText(platformUrl, "플랫폼 상품 URL은 필수입니다.", 1_000);
        this.externalViewCount = requireNonNegative(externalViewCount, "조회수는 0 이상이어야 합니다.");
        this.publishedAt = publishedAt;
        this.lastRefreshedAt = requireNonNull(lastRefreshedAt, "마지막 갱신 시각은 필수입니다.");
    }

    public void replaceImages(List<String> imageUrls) {
        List<String> normalizedUrls = imageUrls == null ? List.of() : imageUrls;
        images.sort(Comparator.comparingInt(ProductImage::getDisplayOrder));
        int commonSize = Math.min(images.size(), normalizedUrls.size());

        for (int index = 0; index < commonSize; index++) {
            images.get(index).updateImageUrl(normalizedUrls.get(index));
        }
        while (images.size() > normalizedUrls.size()) {
            images.remove(images.size() - 1);
        }
        for (int index = commonSize; index < normalizedUrls.size(); index++) {
            addImage(normalizedUrls.get(index), index);
        }
    }

    public void updateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "위도와 경도는 함께 존재하거나 함께 없어야 합니다."
            );
        }

        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long calculateSavingsAmount() {
        if (referencePrice == null || referencePrice <= price) {
            return 0L;
        }
        return referencePrice - price;
    }

    public int calculateSavingsRate() {
        if (referencePrice == null || referencePrice == 0L || referencePrice <= price) {
            return 0;
        }
        return (int) Math.round((double) calculateSavingsAmount() * 100 / referencePrice);
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

    private static String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("입력값은 " + maxLength + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static long requireNonNegative(long value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Long requireNullableNonNegative(Long value, String message) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
