package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.dto.response.ProductDetailResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductRefreshResponse;
import com.hackathon.second_hand_first.product.dto.response.SimilarProductResponse;
import com.hackathon.second_hand_first.product.exception.ProductNotFoundException;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-08-20T02:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        productService = new ProductService(productRepository, fixedClock);
    }

    @Test
    void 상품_상세_조회_성공() {
        Product product = ProductFixture.airPodsPro2();
        ReflectionTestUtils.setField(product, "id", 1L);
        when(productRepository.findDetailById(1L)).thenReturn(Optional.of(product));

        ProductDetailResponse response = productService.getProductDetail(1L);

        assertThat(response.title()).isEqualTo("AirPods Pro 2 (USB-C)");
        assertThat(response.price()).isEqualTo(180_000L);
        assertThat(response.officialPrice()).isEqualTo(299_000L);
        assertThat(response.savingsAmount()).isEqualTo(119_000L);
        assertThat(response.savingsRate()).isEqualTo(40); // round(119000*100/299000) = 40
        assertThat(response.images()).containsExactlyInAnyOrder(
                "https://cdn.example.com/airpods-1.jpg",
                "https://cdn.example.com/airpods-2.jpg"
        );
        assertThat(response.tradeTypes()).containsExactlyInAnyOrder("DIRECT", "DELIVERY");
        assertThat(response.seller().tradeCount()).isEqualTo(32);
        assertThat(response.seller().temperature()).isEqualTo(92.0);
        assertThat(response.distanceKm()).isNull();
        assertThat(response.rank()).isNull();
    }

    @Test
    void 존재하지_않는_상품_상세_조회_시_예외() {
        when(productRepository.findDetailById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetail(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 유사_매물_조회_성공() {
        when(productRepository.existsById(1L)).thenReturn(true);

        SimilarProductResponse response = productService.getSimilarProducts(1L, 3);

        assertThat(response.products()).isEmpty();
    }

    @Test
    void 유사_매물_조회_시_limit_범위_초과_예외() {
        assertThatThrownBy(() -> productService.getSimilarProducts(1L, 11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 이하");
    }

    @Test
    void 유사_매물_조회_시_limit_0_이하_예외() {
        assertThatThrownBy(() -> productService.getSimilarProducts(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상");
    }

    @Test
    void 유사_매물_존재하지_않는_상품_예외() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productService.getSimilarProducts(999L, 3))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 상품_갱신_mock_응답_반환() {
        Product product = ProductFixture.airPodsPro2();
        ReflectionTestUtils.setField(product, "id", 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductRefreshResponse response = productService.refreshProduct(1L);

        assertThat(response.changed()).isFalse();
        assertThat(response.changes()).isEmpty();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_상품_갱신_시_예외() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.refreshProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}