package com.hackathon.second_hand_first.search.repository;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.domain.SearchMarketReference;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SearchSessionRepositoryTest {

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 사용자의_검색세션만_페이지단위로_조회한다() {
        searchSessionRepository.save(completedSession("ss_01", 1L, "에어팟"));
        searchSessionRepository.save(completedSession("ss_02", 1L, "맥북"));
        searchSessionRepository.save(completedSession("ss_other", 2L, "아이폰"));
        searchSessionRepository.flush();

        Page<SearchSession> result = searchSessionRepository.findByUserIdOrderByUpdatedAtDesc(
                1L,
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getUserId()).isEqualTo(1L);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 공개세션아이디와_사용자아이디를_함께_검증한다() {
        searchSessionRepository.saveAndFlush(completedSession("ss_01", 1L, "에어팟"));

        assertThat(searchSessionRepository.findBySessionIdAndUserId("ss_01", 1L)).isPresent();
        assertThat(searchSessionRepository.findBySessionIdAndUserId("ss_01", 2L)).isEmpty();
    }

    @Test
    void 검색세션의_점수버전과_시세기준을_저장한다() {
        SearchSession session = completedSession("ss_market", 1L, "에어팟");
        session.replaceMarketReference(SearchMarketReference.create(
                session,
                "에어팟 프로 2",
                Platform.ELEVENST,
                "11번가",
                "POPULAR_NEW_PRODUCT",
                377_825L,
                4,
                LocalDateTime.of(2026, 8, 21, 10, 0),
                "https://www.11st.co.kr/products/9490377615"
        ));
        searchSessionRepository.saveAndFlush(session);
        entityManager.clear();

        SearchSession found = searchSessionRepository
                .findBySessionIdAndUserId("ss_market", 1L)
                .orElseThrow();

        assertThat(found.getScoringVersion()).isEqualTo("v1");
        assertThat(found.getMarketReference().getMedianPrice()).isEqualTo(377_825L);
        assertThat(found.getMarketReference().getSampleCount()).isEqualTo(4);
    }

    private SearchSession completedSession(String sessionId, Long userId, String keyword) {
        SearchSession session = SearchSession.create(sessionId, userId, keyword);
        session.complete(
                keyword,
                "최고 가성비",
                "저장된 상품을 찾았어요.",
                null,
                SearchPriority.BEST_VALUE,
                List.of(ProductCondition.LIKE_NEW),
                1,
                "v1"
        );
        return session;
    }
}
