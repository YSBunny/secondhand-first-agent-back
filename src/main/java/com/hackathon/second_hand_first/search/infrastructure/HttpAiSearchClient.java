package com.hackathon.second_hand_first.search.infrastructure;

import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.search.exception.AiServerUnavailableException;
import com.hackathon.second_hand_first.search.integration.ai.AiSearchResponseMapper;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiGraphSearchResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchRequest;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 서버를 HTTP로 호출한다.
 *
 * <p>등록 조건은 AiSearchClientConfig 에 있다. ai.base-url 이 없으면
 * {@link UnconfiguredAiSearchClient} 가 대신 동작한다.
 *
 * <p>계약 정의: ai/docs/백엔드_연동_계약.md
 */
@RequiredArgsConstructor
public class HttpAiSearchClient implements AiSearchClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAiSearchClient.class);
    private static final String SEARCH_PATH = "/internal/search";

    private final RestClient aiRestClient;

    @Override
    public AiSearchResponse search(AiSearchRequest request) {
        long start = System.currentTimeMillis();
        AiGraphSearchResponse graph;
        try {
            graph = aiRestClient.post()
                    .uri(SEARCH_PATH)
                    .body(request)
                    .retrieve()
                    .body(AiGraphSearchResponse.class);
        } catch (RestClientException exception) {
            // 타임아웃·연결 실패·4xx·5xx 를 모두 여기서 받는다.
            log.warn("AI 검색 호출 실패 sessionId={} 원인={}",
                    request.sessionId(), exception.getMessage());
            throw new AiServerUnavailableException("AI 서버 호출에 실패했습니다.");
        }

        if (graph == null) {
            throw new AiServerUnavailableException("AI 서버가 빈 응답을 반환했습니다.");
        }

        log.info("AI 검색 완료 sessionId={} items={} elapsed={}ms",
                request.sessionId(),
                graph.items() == null ? 0 : graph.items().size(),
                System.currentTimeMillis() - start);

        return AiSearchResponseMapper.toSearchResponse(graph);
    }
}
