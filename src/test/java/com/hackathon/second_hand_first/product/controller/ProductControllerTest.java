package com.hackathon.second_hand_first.product.controller;

import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductDetailResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductRefreshResponse;
import com.hackathon.second_hand_first.product.dto.response.SimilarProductResponse;
import com.hackathon.second_hand_first.product.exception.ProductNotFoundException;
import com.hackathon.second_hand_first.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(productService);
    }

    @Test
    void 상품_상세_조회_성공_응답() {
        ProductDetailResponse detail = new ProductDetailResponse(
                "1", null, "mock_1", "AirPods Pro 2", 180_000L, 299_000L,
                119_000L, 39, List.of("https://cdn.example.com/1.jpg"),
                "설명", null, null, List.of("DIRECT"), "판교",
                null, 128L, new ProductDetailResponse.SellerInfo(32, 92.0),
                null, null, "https://www.daangn.com/articles/mock_1",
                false, false, OffsetDateTime.now()
        );
        when(productService.getProductDetail(1L)).thenReturn(detail);

        ResponseEntity<ApiResponse<ProductDetailResponse>> response =
                productController.getProductDetail(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("상품 상세를 조회했습니다.");
        assertThat(response.getBody().data().title()).isEqualTo("AirPods Pro 2");
    }

    @Test
    void 없는_상품_상세_조회_시_예외_전파() {
        when(productService.getProductDetail(999L)).thenThrow(new ProductNotFoundException());

        assertThatThrownBy(() -> productController.getProductDetail(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 유사_매물_조회_성공_응답() {
        SimilarProductResponse similar = new SimilarProductResponse(List.of());
        when(productService.getSimilarProducts(1L, 3)).thenReturn(similar);

        ResponseEntity<ApiResponse<SimilarProductResponse>> response =
                productController.getSimilarProducts(1L, 3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("비슷한 매물을 조회했습니다.");
        assertThat(response.getBody().data().products()).isEmpty();
    }

    @Test
    void 없는_상품_유사_매물_조회_시_예외_전파() {
        when(productService.getSimilarProducts(999L, 3)).thenThrow(new ProductNotFoundException());

        assertThatThrownBy(() -> productController.getSimilarProducts(999L, 3))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 상품_갱신_성공_응답() {
        ProductRefreshResponse refresh = new ProductRefreshResponse(
                "1", false, List.of(), OffsetDateTime.now()
        );
        when(productService.refreshProduct(1L)).thenReturn(refresh);

        ResponseEntity<ApiResponse<ProductRefreshResponse>> response =
                productController.refreshProduct(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("상품 정보를 갱신했습니다.");
        assertThat(response.getBody().data().changed()).isFalse();
    }

    @Test
    void 없는_상품_갱신_시_예외_전파() {
        when(productService.refreshProduct(999L)).thenThrow(new ProductNotFoundException());

        assertThatThrownBy(() -> productController.refreshProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}