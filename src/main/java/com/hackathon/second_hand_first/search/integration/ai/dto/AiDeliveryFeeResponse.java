package com.hackathon.second_hand_first.search.integration.ai.dto;

import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.DeliveryStatus;

import java.util.List;

public record AiDeliveryFeeResponse(
        DeliveryStatus status,
        DeliveryPayer payer,
        Long minFee,
        Long homeDeliveryFee,
        AiDeliveryExtraCostResponse extraCost,
        List<AiDeliveryOptionResponse> options
) {
}
