package com.hackathon.second_hand_first.activity.domain;

import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "platform_redirect_histories",
        indexes = {
                @Index(name = "idx_redirect_histories_user_time", columnList = "user_id,redirected_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformRedirectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Platform platform;

    @Column(name = "redirect_url", nullable = false, length = 1_000)
    private String redirectUrl;

    @Column(name = "redirected_at", nullable = false)
    private LocalDateTime redirectedAt;

    private PlatformRedirectHistory(
            Long userId,
            Product product,
            String redirectUrl,
            LocalDateTime redirectedAt
    ) {
        if (userId == null || userId <= 0 || product == null || redirectedAt == null) {
            throw new IllegalArgumentException("플랫폼 이동 기록의 필수 값이 누락되었습니다.");
        }
        if (redirectUrl == null || redirectUrl.isBlank() || redirectUrl.length() > 1_000) {
            throw new IllegalArgumentException("플랫폼 이동 URL이 올바르지 않습니다.");
        }
        this.userId = userId;
        this.product = product;
        this.platform = product.getPlatform();
        this.redirectUrl = redirectUrl.trim();
        this.redirectedAt = redirectedAt;
    }

    public static PlatformRedirectHistory create(
            Long userId,
            Product product,
            String redirectUrl,
            LocalDateTime redirectedAt
    ) {
        return new PlatformRedirectHistory(userId, product, redirectUrl, redirectedAt);
    }
}
