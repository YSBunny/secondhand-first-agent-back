package com.hackathon.second_hand_first.search.infrastructure;

import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchRequest;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;
import org.springframework.stereotype.Component;

/**
 * AI 서버 연동 명세가 연결되기 전까지 임의의 분석 결과를 만들지 않습니다.
 */
@Component
public class UnconfiguredAiSearchClient implements AiSearchClient {

    @Override
    public AiSearchResponse search(AiSearchRequest request) {
        throw new AiServerUnavailableException("AI 서버 연동이 필요합니다.");
    }
}
