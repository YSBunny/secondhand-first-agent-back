package com.hackathon.second_hand_first.location.dto.response;

public record LocationCandidateResponse(
        String address,
        String regionCode,
        String region,
        double latitude,
        double longitude
) {
}