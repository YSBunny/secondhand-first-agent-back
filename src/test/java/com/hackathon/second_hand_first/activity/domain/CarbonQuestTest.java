package com.hackathon.second_hand_first.activity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonQuestTest {

    @Test
    void 세번째_상품을_조회할_때_미션을_완료하고_포인트를_지급한다() {
        CarbonQuest quest = CarbonQuest.create(1L, LocalDate.of(2026, 8, 20));
        LocalDateTime viewedAt = LocalDateTime.of(2026, 8, 20, 12, 0);

        assertThat(quest.countView(viewedAt)).isFalse();
        assertThat(quest.countView(viewedAt.plusMinutes(1))).isFalse();
        assertThat(quest.countView(viewedAt.plusMinutes(2))).isTrue();

        assertThat(quest.getViewedCount()).isEqualTo(3);
        assertThat(quest.isCompleted()).isTrue();
        assertThat(quest.getEarnedPoints()).isEqualTo(100);
    }

    @Test
    void 완료된_미션은_추가로_집계하거나_보상하지_않는다() {
        CarbonQuest quest = CarbonQuest.create(1L, LocalDate.of(2026, 8, 20));
        LocalDateTime viewedAt = LocalDateTime.of(2026, 8, 20, 12, 0);
        quest.countView(viewedAt);
        quest.countView(viewedAt);
        quest.countView(viewedAt);

        boolean rewarded = quest.countView(viewedAt.plusHours(1));

        assertThat(rewarded).isFalse();
        assertThat(quest.getViewedCount()).isEqualTo(3);
        assertThat(quest.getEarnedPoints()).isEqualTo(100);
    }
}
