package com.hackathon.second_hand_first.search.domain;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
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
        name = "search_session_conditions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_search_session_conditions_session_condition",
                        columnNames = {"search_session_id", "product_condition"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchSessionCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_session_id", nullable = false)
    private SearchSession searchSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 30)
    private ProductCondition condition;

    private SearchSessionCondition(
            SearchSession searchSession,
            ProductCondition condition
    ) {
        if (searchSession == null) {
            throw new IllegalArgumentException("검색 세션은 필수입니다.");
        }
        if (condition == null) {
            throw new IllegalArgumentException("상품 상태 조건은 필수입니다.");
        }
        this.searchSession = searchSession;
        this.condition = condition;
    }

    static SearchSessionCondition create(
            SearchSession searchSession,
            ProductCondition condition
    ) {
        return new SearchSessionCondition(searchSession, condition);
    }
}
