package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.domain.PlatformRedirectHistory;
import com.hackathon.second_hand_first.activity.dto.PlatformRedirectResponse;
import com.hackathon.second_hand_first.activity.exception.RedirectForbiddenException;
import com.hackathon.second_hand_first.activity.repository.PlatformRedirectHistoryRepository;
import com.hackathon.second_hand_first.product.domain.DeliveryFee;
import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformRedirectServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T05:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PlatformRedirectHistoryRepository redirectHistoryRepository;

    private PlatformRedirectService platformRedirectService;

    @BeforeEach
    void setUp() {
        platformRedirectService = new PlatformRedirectService(
                productRepository,
                redirectHistoryRepository,
                CLOCK
        );
    }

    @Test
    void 허용된_플랫폼_URL의_이동을_기록한다() {
        Product product = ProductFixture.airPodsPro2();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(redirectHistoryRepository.save(any(PlatformRedirectHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlatformRedirectResponse response = platformRedirectService.record(1L, 10L);

        assertThat(response.platform()).isEqualTo(Platform.NAVER_FLEAMARKET);
        assertThat(response.redirectUrl()).isEqualTo("https://fleamarket.naver.com/products/mock_1");
        assertThat(response.redirectedAt().toString()).isEqualTo("2026-08-20T14:30+09:00");
        verify(redirectHistoryRepository).save(any(PlatformRedirectHistory.class));
    }

    @Test
    void 플랫폼과_일치하지_않는_외부_URL은_차단한다() {
        Product product = Product.create(
                Platform.NAVER_FLEAMARKET,
                "unsafe_1",
                "테스트 상품",
                null,
                ProductCategory.OTHER,
                10_000L,
                null,
                ProductCondition.LIGHTLY_USED,
                ProductStatus.SELLING,
                null,
                true,
                false,
                DeliveryFee.of(3_000L, 3_000L, DeliveryPayer.BUYER),
                true,
                "https://evil.example.com/product/1",
                0L,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                LocalDateTime.of(2026, 8, 20, 11, 0)
        );
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> platformRedirectService.record(1L, 10L))
                .isInstanceOf(RedirectForbiddenException.class);
        verify(redirectHistoryRepository, never()).save(any());
    }
}
