package com.hackathon.second_hand_first.activity.dto;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;

import java.time.LocalDate;

public record CarbonQuestResponse(
        LocalDate date,
        int viewedCount,
        int goal,
        boolean completed,
        int earnedPoints
) {
    public static CarbonQuestResponse from(CarbonQuest quest) {
        return new CarbonQuestResponse(
                quest.getQuestDate(),
                quest.getViewedCount(),
                quest.getGoal(),
                quest.isCompleted(),
                quest.getEarnedPoints()
        );
    }

    public static CarbonQuestResponse empty(LocalDate date) {
        return new CarbonQuestResponse(date, 0, CarbonQuest.DAILY_GOAL, false, 0);
    }
}
