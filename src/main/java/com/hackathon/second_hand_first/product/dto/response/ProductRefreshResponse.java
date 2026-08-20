package com.hackathon.second_hand_first.product.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ProductRefreshResponse(
        String productId,
        boolean changed,
        List<FieldChange> changes,
        OffsetDateTime updatedAt
) {
    public record FieldChange(String field, Object before, Object after) {
    }
}