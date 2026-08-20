package com.hackathon.second_hand_first.search.application;

import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchRequest;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;

/**
 * 자연어 분석, 외부 상품 탐색, 추천 순위 계산을 AI 서버에 위임하기 위한 포트입니다.
 */
public interface AiSearchClient {

    AiSearchResponse search(AiSearchRequest request);
}
