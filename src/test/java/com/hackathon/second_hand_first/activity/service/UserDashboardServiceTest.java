package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.dto.CarbonQuestResponse;
import com.hackathon.second_hand_first.activity.dto.UserDashboardResponse;
import com.hackathon.second_hand_first.activity.repository.PlatformRedirectHistoryRepository;
import com.hackathon.second_hand_first.search.domain.SearchSessionStatus;
import com.hackathon.second_hand_first.search.repository.SearchSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    @Mock
    private PlatformRedirectHistoryRepository redirectHistoryRepository;
    @Mock
    private SearchSessionRepository searchSessionRepository;
    @Mock
    private CarbonQuestService carbonQuestService;

    private UserDashboardService userDashboardService;

    @BeforeEach
    void setUp() {
        userDashboardService = new UserDashboardService(
                redirectHistoryRepository,
                searchSessionRepository,
                carbonQuestService
        );
    }

    @Test
    void 활동_통계와_오늘의_탄소미션을_함께_조회한다() {
        CarbonQuestResponse carbonQuest = new CarbonQuestResponse(
                LocalDate.of(2026, 8, 20), 2, 3, false, 0
        );
        when(redirectHistoryRepository.countByUserId(1L)).thenReturn(8L);
        when(searchSessionRepository.countByUserIdAndStatus(1L, SearchSessionStatus.COMPLETED))
                .thenReturn(27L);
        when(carbonQuestService.getTodayQuest(1L)).thenReturn(carbonQuest);

        UserDashboardResponse response = userDashboardService.getDashboard(1L);

        assertThat(response.stats().platformRedirectCount()).isEqualTo(8L);
        assertThat(response.stats().aiSearchCount()).isEqualTo(27L);
        assertThat(response.carbonQuest()).isEqualTo(carbonQuest);
    }
}
