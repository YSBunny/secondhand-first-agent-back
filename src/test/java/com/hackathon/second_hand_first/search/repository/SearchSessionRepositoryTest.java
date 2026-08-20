package com.hackathon.second_hand_first.search.repository;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.search.domain.SearchPriority;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SearchSessionRepositoryTest {

    @Autowired
    private SearchSessionRepository searchSessionRepository;

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

    private SearchSession completedSession(String sessionId, Long userId, String keyword) {
        SearchSession session = SearchSession.create(sessionId, userId, keyword);
        session.complete(
                keyword,
                "최고 가성비",
                "저장된 상품을 찾았어요.",
                null,
                SearchPriority.BEST_VALUE,
                List.of(ProductCondition.LIKE_NEW),
                1
        );
        return session;
    }
}
