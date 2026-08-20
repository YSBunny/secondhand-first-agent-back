package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.product.dto.response.BestDealPageResponse;
import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import org.springframework.stereotype.Service;

@Service
public class BestDealService {

    /**
     * Best Deal의 점수, 순위, 추천 근거는 AI 서버가 결정한다.
     *
     * <p>AI 응답 계약과 저장 구조가 연결되기 전까지 백엔드가 임의의 추천 결과를
     * 만들지 않는다. 계약 연결 후에는 저장된 AI 추천 결과를 조회하는 구현으로 교체한다.
     */
    public BestDealPageResponse getBestDeals(
            String category,
            String sort,
            int page,
            int size
    ) {
        throw new AiServerUnavailableException("AI Best Deal 연동이 필요합니다.");
    }
}
