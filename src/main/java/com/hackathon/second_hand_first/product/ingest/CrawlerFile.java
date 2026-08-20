package com.hackathon.second_hand_first.product.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 통합 크롤러가 만든 JSON 파일 한 개.
 *
 * <p><b>파일 하나가 검색어 하나다.</b> {@code query} 가 그 검색어이며, 적재는 이 값으로
 * 카테고리를 한 번만 판정해 {@code items} 전체에 적용한다.
 *
 * <p>스키마 원본은 data-analysis/docs/통합_스키마_정의.md 이다. 여기서 읽지 않는 필드
 * (generatedAt · sourceCounts · count)는 무시한다 — 크롤러가 필드를 더 붙여도
 * 적재가 깨지지 않아야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlerFile(
        String query,
        List<CrawlerItem> items
) {
    public List<CrawlerItem> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }
}
