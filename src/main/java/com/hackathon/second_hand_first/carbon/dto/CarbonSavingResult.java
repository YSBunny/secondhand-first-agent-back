package com.hackathon.second_hand_first.carbon.dto;

public record CarbonSavingResult(
        String status,
        Double co2eKg,
        String source,
        String reason
) {
    public static CarbonSavingResult available(double co2eKg, String source) {
        return new CarbonSavingResult("AVAILABLE", co2eKg, source, null);
    }

    public static CarbonSavingResult notAvailable(String reason) {
        return new CarbonSavingResult("NOT_AVAILABLE", null, null, reason);
    }

    public static CarbonSavingResult notApplicable() {
        return new CarbonSavingResult("NOT_APPLICABLE", null, null, null);
    }
}