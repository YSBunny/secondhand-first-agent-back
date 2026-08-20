package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.domain.CarbonQuest;
import com.hackathon.second_hand_first.activity.domain.CarbonQuestCountedReason;
import com.hackathon.second_hand_first.activity.dto.ProductViewResponse;
import com.hackathon.second_hand_first.activity.repository.CarbonQuestRepository;
import com.hackathon.second_hand_first.activity.repository.ProductViewRecordRepository;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonQuestServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductViewRecordRepository productViewRecordRepository;
    @Mock
    private CarbonQuestRepository carbonQuestRepository;

    private CarbonQuestService carbonQuestService;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        carbonQuestService = new CarbonQuestService(
                userRepository,
                productRepository,
                productViewRecordRepository,
                carbonQuestRepository,
                CLOCK
        );
        user = User.create("김민재", "test@example.com", "encoded", null, true, false);
        product = ProductFixture.airPodsPro2();
    }

    @Test
    void 세번째_신규상품_조회는_미션을_완료하고_보상완료를_반환한다() {
        CarbonQuest quest = CarbonQuest.create(1L, LocalDate.of(2026, 8, 20));
        quest.countView(java.time.LocalDateTime.of(2026, 8, 20, 10, 0));
        quest.countView(java.time.LocalDateTime.of(2026, 8, 20, 11, 0));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(carbonQuestRepository.findByUserIdAndQuestDate(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(Optional.of(quest));
        when(productViewRecordRepository.existsByUserIdAndProductIdAndViewedDate(
                1L, 10L, LocalDate.of(2026, 8, 20)
        )).thenReturn(false);

        ProductViewResponse response = carbonQuestService.recordProductView(1L, 10L);

        assertThat(response.counted()).isTrue();
        assertThat(response.rewarded()).isTrue();
        assertThat(response.carbonQuest().viewedCount()).isEqualTo(3);
        verify(productViewRecordRepository).save(any());
        verify(carbonQuestRepository).save(quest);
    }

    @Test
    void 같은_날짜에_이미_조회한_상품은_중복_집계하지_않는다() {
        CarbonQuest quest = CarbonQuest.create(1L, LocalDate.of(2026, 8, 20));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(carbonQuestRepository.findByUserIdAndQuestDate(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(Optional.of(quest));
        when(productViewRecordRepository.existsByUserIdAndProductIdAndViewedDate(
                1L, 10L, LocalDate.of(2026, 8, 20)
        )).thenReturn(true);

        ProductViewResponse response = carbonQuestService.recordProductView(1L, 10L);

        assertThat(response.counted()).isFalse();
        assertThat(response.countedReason()).isEqualTo(CarbonQuestCountedReason.ALREADY_VIEWED);
        assertThat(quest.getViewedCount()).isZero();
    }

    @Test
    void 오늘_미션이_없으면_0회_진행도로_조회한다() {
        when(carbonQuestRepository.findByUserIdAndQuestDate(1L, LocalDate.of(2026, 8, 20)))
                .thenReturn(Optional.empty());

        var response = carbonQuestService.getTodayQuest(1L);

        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(response.viewedCount()).isZero();
        assertThat(response.goal()).isEqualTo(3);
        assertThat(response.completed()).isFalse();
    }
}
