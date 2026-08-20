package com.hackathon.second_hand_first.search.infrastructure;

import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchRequest;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;

/**
 * AI 서버 주소가 없을 때 쓰는 구현.
 *
 * <p>주소를 모르는 상태에서 임의의 분석 결과를 만들지 않는다. 502를 주고 끝낸다.
 * 등록 조건은 AiSearchClientConfig 에 있다.
 */
public class UnconfiguredAiSearchClient implements AiSearchClient {

    @Override
    public AiSearchResponse search(AiSearchRequest request) {
        throw new AiServerUnavailableException("AI 서버 연동이 필요합니다.");
    }
}
