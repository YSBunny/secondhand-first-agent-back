package com.hackathon.second_hand_first.activity.dto;

public record UserDashboardResponse(
        DashboardStatsResponse stats,
        CarbonQuestResponse carbonQuest
) {
}
