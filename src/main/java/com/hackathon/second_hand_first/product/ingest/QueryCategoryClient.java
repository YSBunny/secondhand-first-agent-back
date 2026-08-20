package com.hackathon.second_hand_first.product.ingest;

import com.hackathon.second_hand_first.product.domain.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 검색어의 카테고리를 AI 서버에 묻는다.
 *
 * <p>크롤러 출력은 파일 하나가 검색어 하나다. 그 검색어를 {@code /internal/parse-query} 로
 * 보내 카테고리를 받아 <b>파일 전체에 적용</b>한다. Bedrock 호출이 파일당 1회다 —
 * {@code /internal/search} 는 최대 6회라 조건만 필요한 여기서 부르면 낭비다.
 *
 * <p>카테고리를 검색 한 건에 하나로 두는 것은 이미 정해진 방식이다.
 * {@code AiSearchResponseMapper} 도 검색 한 건의 카테고리를 후보 전부에 적용한다 —
 * <b>카테고리는 상품이 아니라 «무엇을 찾는가»에서 나오는 값</b>이기 때문이다.
 *
 * <p>판단 기준은 AI 프롬프트 한 곳에만 둔다. 적재 쪽에서 키워드 규칙을 따로 만들면
 * 같은 상품이 경로에 따라 다른 카테고리를 받는다.
 *
 * <p><b>실패해도 적재를 멈추지 않는다.</b> AI 가 없거나 응답이 이상하면
 * {@link ProductCategory#OTHER} 로 떨어뜨린다 — 카테고리 하나 때문에 상품 수백 건이
 * 들어가지 못하는 편이 더 나쁘다.
 */
@Component
@RequiredArgsConstructor
public class QueryCategoryClient {

    private static final Logger log = LoggerFactory.getLogger(QueryCategoryClient.class);

    private static final String PARSE_QUERY_PATH = "/internal/parse-query";

    /**
     * AI 호출용 RestClient. {@code ai.base-url} 이 없으면 빈 자체가 없으므로
     * 필수 주입이 아니라 ObjectProvider 로 받는다 — <b>AI 없이도 적재는 돌아야 한다.</b>
     */
    private final ObjectProvider<RestClient> aiRestClient;

    public ProductCategory categoryOf(String query) {
        if (query == null || query.isBlank()) {
            log.warn("검색어가 비어 있어 카테고리를 묻지 않는다 — OTHER 로 적재한다");
            return ProductCategory.OTHER;
        }

        RestClient client = aiRestClient.getIfAvailable();
        if (client == null) {
            log.warn("ai.base-url 이 없어 카테고리를 묻지 못한다 — OTHER 로 적재한다");
            return ProductCategory.OTHER;
        }

        try {
            JsonNode body = client.post()
                    .uri(PARSE_QUERY_PATH)
                    .body(Map.of("raw_query", query))
                    .retrieve()
                    .body(JsonNode.class);
            return categoryFrom(body, query);
        } catch (Exception exception) {
            log.warn("카테고리 질의 실패 — query={} error={}", query, exception.toString());
            return ProductCategory.OTHER;
        }
    }

    private ProductCategory categoryFrom(JsonNode body, String query) {
        if (body == null) {
            log.warn("카테고리 응답이 비어 있다 — query={}", query);
            return ProductCategory.OTHER;
        }
        JsonNode value = body.path("query_parsed").path("category");
        // AI 는 확신이 없으면 null 을 준다. 억지로 가까운 값에 넣지 않는 규칙이라
        // 이때 OTHER 는 «분류하지 못했다»는 정상 결과다.
        if (value.isMissingNode() || value.isNull()) {
            log.info("AI 가 카테고리를 판정하지 못했다 — query={}", query);
            return ProductCategory.OTHER;
        }
        try {
            ProductCategory category = ProductCategory.valueOf(
                    value.asString().trim().toUpperCase()
            );
            log.info("카테고리 판정 — query={} category={}", query, category);
            return category;
        } catch (IllegalArgumentException exception) {
            log.warn("모르는 카테고리 값 — query={} value={}", query, value.asString());
            return ProductCategory.OTHER;
        }
    }
}
