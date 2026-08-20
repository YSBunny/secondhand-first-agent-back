package com.hackathon.second_hand_first.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * AI 서버 호출용 RestClient.
 *
 * <p>ai.base-url 이 있을 때만 만든다. 없으면 UnconfiguredAiSearchClient 가
 * 대신 동작해 502를 준다 — 주소가 없는데 임의의 결과를 지어내지 않는다.
 */
@Configuration
@ConditionalOnProperty(name = "ai.base-url")
public class AiRestClientConfig {

    /**
     * 읽기 타임아웃을 길게 잡는다. AI는 한 요청에 LLM을 최대 6회 부른다
     * (parse 1 + validate 1 + rerank 4). 카카오 호출과 같은 3초를 쓰면
     * 정상 요청이 끊긴다.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);

        // AI 서버에 AI_API_KEY 가 설정돼 있으면 이 헤더를 요구한다.
        // 양쪽이 같은 값을 써야 한다.
        if (!apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }
        return builder.build();
    }
}
