package com.hackathon.second_hand_first.location.dto.response;

public record CoordinateResponse(
        String region,
        double latitude,
        double longitude
) {
}