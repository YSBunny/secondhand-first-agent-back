package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.dto.CarbonQuestResponse;
import com.hackathon.second_hand_first.activity.dto.DashboardStatsResponse;
import com.hackathon.second_hand_first.activity.dto.UserDashboardResponse;
import com.hackathon.second_hand_first.activity.repository.PlatformRedirectHistoryRepository;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.repository.SearchSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardService {

    private final PlatformRedirectHistoryRepository redirectHistoryRepository;
    private final SearchSessionRepository searchSessionRepository;
    private final CarbonQuestService carbonQuestService;

    public UserDashboardResponse getDashboard(Long userId) {
        DashboardStatsResponse stats = new DashboardStatsResponse(
                redirectHistoryRepository.countByUserId(userId),
                searchSessionRepository.countByUserIdAndStatus(userId, SearchSessionStatus.COMPLETED)
        );
        CarbonQuestResponse carbonQuest = carbonQuestService.getTodayQuest(userId);
        return new UserDashboardResponse(stats, carbonQuest);
    }
}
