package com.hackathon.second_hand_first.search.domain;

import com.hackathon.second_hand_first.product.domain.ProductCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchSessionTest {

    @Test
    void 검색세션을_처리중_상태로_생성한다() {
        SearchSession session = SearchSession.create(
                "ss_01",
                1L,
                "30만원으로 에어팟 사고 싶어, 중고 괜찮아"
        );

        assertThat(session.getSessionId()).isEqualTo("ss_01");
        assertThat(session.getKeyword()).isEqualTo("30만원으로 에어팟 사고 싶어, 중고 괜찮아");
        assertThat(session.getStatus()).isEqualTo(SearchSessionStatus.PROCESSING);
        assertThat(session.getResultCount()).isZero();
    }

    @Test
    void 분석결과와_조건을_반영해_검색세션을_완료한다() {
        SearchSession session = SearchSession.create("ss_01", 1L, "30만원으로 에어팟 사고 싶어");

        session.complete(
                "에어팟",
                "30만원 이하, 중고 가능, 최고 가성비",
                "당근·번개장터·중고나라에서 12개 매물을 찾았어요.",
                300_000L,
                SearchPriority.BEST_VALUE,
                List.of(ProductCondition.LIKE_NEW, ProductCondition.GOOD),
                12
        );

        assertThat(session.getStatus()).isEqualTo(SearchSessionStatus.COMPLETED);
        assertThat(session.getKeyword()).isEqualTo("에어팟");
        assertThat(session.getMaxPrice()).isEqualTo(300_000L);
        assertThat(session.getPriority()).isEqualTo(SearchPriority.BEST_VALUE);
        assertThat(session.getConditions())
                .extracting(SearchSessionCondition::getCondition)
                .containsExactly(ProductCondition.LIKE_NEW, ProductCondition.GOOD);
        assertThat(session.getResultCount()).isEqualTo(12);
    }

    @Test
    void 중복된_상품상태조건은_한번만_저장한다() {
        SearchSession session = SearchSession.create("ss_01", 1L, "에어팟");

        session.complete(
                "에어팟",
                null,
                null,
                null,
                null,
                List.of(ProductCondition.GOOD, ProductCondition.GOOD),
                0
        );

        assertThat(session.getConditions()).hasSize(1);
    }

    @Test
    void 처리중인_검색세션을_실패상태로_변경한다() {
        SearchSession session = SearchSession.create("ss_01", 1L, "에어팟");

        session.fail("외부 플랫폼 조회에 실패했습니다.");

        assertThat(session.getStatus()).isEqualTo(SearchSessionStatus.FAILED);
        assertThat(session.getLastMessage()).isEqualTo("외부 플랫폼 조회에 실패했습니다.");
    }

    @Test
    void 완료된_검색세션은_다시_완료하거나_실패처리할_수_없다() {
        SearchSession session = SearchSession.create("ss_01", 1L, "에어팟");
        session.complete("에어팟", null, null, null, null, List.of(), 0);

        assertThatThrownBy(() -> session.fail("실패"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("처리 중인 검색 세션만 실패 처리할 수 있습니다.");
    }
}
