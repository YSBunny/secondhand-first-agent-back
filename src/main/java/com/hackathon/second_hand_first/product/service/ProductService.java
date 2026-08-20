package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.SellerSnapshot;
import com.hackathon.second_hand_first.product.dto.response.ProductDetailResponse;
import com.hackathon.second_hand_first.product.dto.response.ProductRefreshResponse;
import com.hackathon.second_hand_first.product.dto.response.SimilarProductResponse;
import com.hackathon.second_hand_first.product.exception.ProductNotFoundException;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_SIMILAR_LIMIT = 10;

    private final ProductRepository productRepository;
    private final Clock applicationClock;

    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findDetailById(productId)
                .orElseThrow(ProductNotFoundException::new);

        return toDetailResponse(product);
    }

    public SimilarProductResponse getSimilarProducts(Long productId, int limit) {
        if (limit < 1 || limit > MAX_SIMILAR_LIMIT) {
            throw new IllegalArgumentException("limit는 1 이상 10 이하여야 합니다.");
        }
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException();
        }

        // TODO: 외부 AI 연동 후 실제 유사 매물 조회로 교체
        return new SimilarProductResponse(List.of());
    }

    public ProductRefreshResponse refreshProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        // TODO: 외부 플랫폼 크롤링 연동 후 실제 변경 감지로 교체
        return new ProductRefreshResponse(
                product.getId().toString(),
                false,
                List.of(),
                OffsetDateTime.now(applicationClock)
        );
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        List<String> imageUrls = product.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();

        List<String> tradeTypes = new ArrayList<>();
        if (product.isDirectTradeAvailable()) {
            tradeTypes.add("DIRECT");
        }
        if (product.isShippingAvailable()) {
            tradeTypes.add("DELIVERY");
        }

        SellerSnapshot seller = product.getSellerSnapshot();
        ProductDetailResponse.SellerInfo sellerInfo = seller == null
                ? new ProductDetailResponse.SellerInfo(0, 0.0)
                : new ProductDetailResponse.SellerInfo(
                        seller.getTradeCount(),
                        seller.getMannerTemperature() == null ? 0.0 : seller.getMannerTemperature()
                );

        long officialPrice = product.getReferencePrice() == null
                ? product.getPrice()
                : product.getReferencePrice();

        OffsetDateTime updatedAt = product.getLastRefreshedAt()
                .atZone(SEOUL)
                .toOffsetDateTime();

        return new ProductDetailResponse(
                product.getId().toString(),
                product.getPlatform(),
                product.getExternalProductId(),
                product.getTitle(),
                product.getPrice(),
                officialPrice,
                product.calculateSavingsAmount(),
                product.calculateSavingsRate(),
                imageUrls,
                product.getDescription(),
                product.getCategory(),
                product.getCondition(),
                tradeTypes,
                product.getLocation(),
                null,       // distanceKm: 사용자 위치 연동 전 null
                product.getExternalViewCount(),
                sellerInfo,
                null,       // rank: AI 검색 결과 연동 전 null
                null,       // recommendationReason: AI 연동 전 null
                product.getPlatformUrl(),
                false,      // changedSinceLastViewed: refresh 연동 전 false
                updatedAt
        );
    }
}