package com.hackathon.second_hand_first.search.repository;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.domain.SearchResult;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SearchResultRepositoryTest {

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @Autowired
    private SearchResultRepository searchResultRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void AI_점수구성과_거리를_검색결과에_저장한다() {
        SearchSession session = SearchSession.create("ss_score", 1L, "에어팟");
        session.complete(
                "에어팟",
                "최고 가성비",
                "상품을 찾았어요.",
                300_000L,
                SearchPriority.BEST_VALUE,
                List.of(ProductCondition.LIKE_NEW),
                1,
                "v1"
        );
        searchSessionRepository.saveAndFlush(session);
        var product = productRepository.saveAndFlush(ProductFixture.airPodsPro2());

        searchResultRepository.saveAndFlush(SearchResult.create(
                session,
                product,
                1,
                88.0,
                "합리적입니다.",
                91.0,
                80.0,
                82.0,
                3.2
        ));
        entityManager.clear();

        SearchResult found = searchResultRepository
                .findBySearchSessionSessionIdOrderByRankAsc("ss_score")
                .getFirst();

        assertThat(found.getPriceScore()).isEqualTo(91.0);
        assertThat(found.getQualityScore()).isEqualTo(80.0);
        assertThat(found.getConvenienceScore()).isEqualTo(82.0);
        assertThat(found.getDistanceKm()).isEqualTo(3.2);
    }
}
