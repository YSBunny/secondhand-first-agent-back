package com.hackathon.second_hand_first.search.domain;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(
        name = "search_sessions",
        indexes = {
                @Index(name = "idx_search_sessions_user_updated", columnList = "user_id,updated_at"),
                @Index(name = "idx_search_sessions_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * API에서 사용하는 공개 식별자입니다. 예: ss_01, ss_550e8400...
     */
    @Column(name = "session_id", nullable = false, unique = true, length = 50)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_query", nullable = false, length = 1_000)
    private String originalQuery;

    @Column(nullable = false, length = 255)
    private String keyword;

    @Column(name = "query_summary", length = 1_000)
    private String querySummary;

    @Column(name = "last_message", length = 2_000)
    private String lastMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SearchSessionStatus status;

    @Column(name = "max_price")
    private Long maxPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SearchPriority priority;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "scoring_version", length = 50)
    private String scoringVersion;

    @OneToMany(mappedBy = "searchSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<SearchSessionCondition> conditions = new ArrayList<>();

    @OneToOne(mappedBy = "searchSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private SearchMarketReference marketReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SearchSession(
            String sessionId,
            Long userId,
            String originalQuery
    ) {
        this.sessionId = requireText(sessionId, "검색 세션 ID는 필수입니다.", 50);
        this.userId = requirePositive(userId, "사용자 ID는 필수입니다.");
        this.originalQuery = requireText(originalQuery, "검색어는 필수입니다.", 1_000);
        this.keyword = this.originalQuery;
        this.status = SearchSessionStatus.PROCESSING;
        this.resultCount = 0;
    }

    public static SearchSession create(
            String sessionId,
            Long userId,
            String originalQuery
    ) {
        return new SearchSession(sessionId, userId, originalQuery);
    }

    public List<SearchSessionCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public void complete(
            String keyword,
            String querySummary,
            String lastMessage,
            Long maxPrice,
            SearchPriority priority,
            Collection<ProductCondition> parsedConditions,
            int resultCount
    ) {
        complete(
                keyword,
                querySummary,
                lastMessage,
                maxPrice,
                priority,
                parsedConditions,
                resultCount,
                null
        );
    }

    public void complete(
            String keyword,
            String querySummary,
            String lastMessage,
            Long maxPrice,
            SearchPriority priority,
            Collection<ProductCondition> parsedConditions,
            int resultCount,
            String scoringVersion
    ) {
        if (status != SearchSessionStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 검색 세션만 완료할 수 있습니다.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("최대 가격은 0 이상이어야 합니다.");
        }
        if (resultCount < 0) {
            throw new IllegalArgumentException("검색 결과 개수는 0 이상이어야 합니다.");
        }

        this.keyword = requireText(keyword, "분석된 검색 키워드는 필수입니다.", 255);
        this.querySummary = normalizeNullableText(querySummary, 1_000);
        this.lastMessage = normalizeNullableText(lastMessage, 2_000);
        this.maxPrice = maxPrice;
        this.priority = priority;
        replaceConditions(parsedConditions);
        this.resultCount = resultCount;
        this.scoringVersion = normalizeNullableText(scoringVersion, 50);
        this.status = SearchSessionStatus.COMPLETED;
    }

    public void replaceMarketReference(SearchMarketReference marketReference) {
        if (marketReference != null && marketReference.getSearchSession() != this) {
            throw new IllegalArgumentException("다른 검색 세션의 시세 기준을 연결할 수 없습니다.");
        }
        this.marketReference = marketReference;
    }

    public void fail(String failureMessage) {
        if (status != SearchSessionStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 검색 세션만 실패 처리할 수 있습니다.");
        }
        this.lastMessage = normalizeNullableText(failureMessage, 2_000);
        this.status = SearchSessionStatus.FAILED;
    }

    private void replaceConditions(Collection<ProductCondition> parsedConditions) {
        conditions.clear();
        if (parsedConditions == null) {
            return;
        }
        parsedConditions.forEach(this::addCondition);
    }

    private void addCondition(ProductCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("상품 상태 조건은 null일 수 없습니다.");
        }
        if (conditions.stream().anyMatch(saved -> saved.getCondition() == condition)) {
            return;
        }
        conditions.add(SearchSessionCondition.create(this, condition));
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

    private static Long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
