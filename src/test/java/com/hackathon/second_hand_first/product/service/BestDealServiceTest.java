package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BestDealServiceTest {

    private final BestDealService bestDealService = new BestDealService();

    @Test
    void AI_연동_전에는_임의의_Best_Deal을_계산하지_않는다() {
        assertThatThrownBy(() -> bestDealService.getBestDeals("ALL", "AI_RECOMMENDED", 0, 12))
                .isInstanceOf(AiServerUnavailableException.class)
                .hasMessage("AI Best Deal 연동이 필요합니다.");
    }
}
