package com.hackathon.second_hand_first.product.dto.response;

import java.util.List;

public record BestDealPageResponse(
        List<BestDealItemResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
