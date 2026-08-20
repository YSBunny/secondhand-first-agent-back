package com.hackathon.second_hand_first.activity.dto;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;
import com.hackathon.second_hand_first.activity.domain.CarbonQuestCountedReason;

import java.time.LocalDate;

public record ProductViewResponse(
        boolean counted,
        CarbonQuestCountedReason countedReason,
        boolean rewarded,
        CarbonQuestResponse carbonQuest
) {
    public static ProductViewResponse of(
            boolean counted,
            CarbonQuestCountedReason countedReason,
            boolean rewarded,
            CarbonQuest quest
    ) {
        return new ProductViewResponse(
                counted,
                countedReason,
                rewarded,
                CarbonQuestResponse.from(quest)
        );
    }

    public static ProductViewResponse empty(
            CarbonQuestCountedReason countedReason,
            LocalDate date
    ) {
        return new ProductViewResponse(
                false,
                countedReason,
                false,
                CarbonQuestResponse.empty(date)
        );
    }
}
