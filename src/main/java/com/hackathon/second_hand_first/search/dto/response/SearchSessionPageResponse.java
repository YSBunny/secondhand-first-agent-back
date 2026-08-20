package com.hackathon.second_hand_first.search.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchSessionPageResponse(
        List<RecentSearchSessionResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static SearchSessionPageResponse from(Page<RecentSearchSessionResponse> result) {
        return new SearchSessionPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.hasNext()
        );
    }
}
