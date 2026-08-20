package com.hackathon.second_hand_first.product.controller;

import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductDetailResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductRefreshResponse;
import com.hackathon.second_hand_first.product.dto.response.SimilarProductResponse;
import com.hackathon.second_hand_first.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: AUTH 머지 후 @AuthenticationPrincipal CustomUserDetails 인증 처리 추가
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @PathVariable Long productId
    ) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(
                ApiResponse.success("상품 상세를 조회했습니다.", response)
        );
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<ApiResponse<SimilarProductResponse>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        SimilarProductResponse response = productService.getSimilarProducts(productId, limit);
        return ResponseEntity.ok(
                ApiResponse.success("비슷한 매물을 조회했습니다.", response)
        );
    }

    @PostMapping("/{productId}/refresh")
    public ResponseEntity<ApiResponse<ProductRefreshResponse>> refreshProduct(
            @PathVariable Long productId
    ) {
        ProductRefreshResponse response = productService.refreshProduct(productId);
        return ResponseEntity.ok(
                ApiResponse.success("상품 정보를 갱신했습니다.", response)
        );
    }
}