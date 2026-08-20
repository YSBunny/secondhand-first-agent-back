package com.hackathon.second_hand_first.search.config;

import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.search.infrastructure.HttpAiSearchClient;
import com.hackathon.second_hand_first.search.infrastructure.UnconfiguredAiSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * AI 검색 클라이언트를 하나만 고른다.
 *
 * <p>ai.base-url 이 있으면 실제 HTTP 호출을, 없으면 502를 주는 구현을 쓴다.
 * 주소를 모르는 상태에서 임의의 결과를 만들지 않기 위해서다.
 *
 * <p>두 구현을 @Component 로 두고 @ConditionalOnMissingBean 을 붙이면
 * 컴포넌트 스캔 순서에 따라 결과가 달라질 수 있다. 그래서 @Bean 으로 선언한다.
 */
@Configuration
public class AiSearchClientConfig {

    @Bean
    @ConditionalOnProperty(name = "ai.base-url")
    public AiSearchClient httpAiSearchClient(RestClient aiRestClient) {
        return new HttpAiSearchClient(aiRestClient);
    }

    @Bean
    @ConditionalOnMissingBean(AiSearchClient.class)
    public AiSearchClient unconfiguredAiSearchClient() {
        return new UnconfiguredAiSearchClient();
    }
}
