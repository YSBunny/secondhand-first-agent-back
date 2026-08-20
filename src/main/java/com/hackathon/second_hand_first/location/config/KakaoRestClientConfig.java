package com.hackathon.second_hand_first.location.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoRestClientConfig {

    @Bean
    public RestClient kakaoRestClient(
            @Value("${kakao.local.base-url}") String baseUrl,
            @Value("${kakao.local.rest-api-key}") String restApiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Authorization",
                        "KakaoAK " + restApiKey
                )
                .requestFactory(requestFactory)
                .build();
    }
}