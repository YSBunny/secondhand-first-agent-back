package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.DeliveryCarrier;
import com.hackathon.second_hand_first.product.domain.DeliveryMethod;
import tools.jackson.databind.JsonNode;

public record AiDeliveryOptionResponse(
        DeliveryMethod method,
        DeliveryCarrier carrier,
        Boolean requiresPickupPoint,
        Long fee,
        JsonNode rawCode
) {
}
