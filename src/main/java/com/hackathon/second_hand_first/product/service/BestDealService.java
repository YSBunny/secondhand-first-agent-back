package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.dto.response.BestDealItemResponse;
import com.hackathon.second_hand_first.product.dto.response.BestDealPageResponse;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BestDealService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    public BestDealPageResponse getBestDeals(
            String category,
            String sort,
            int page,
            int size
    ) {
        validatePage(page, size);
        ProductCategory categoryFilter = parseCategory(category);
        BestDealSort sortType = BestDealSort.parse(sort);

        List<ScoredProduct> rankedProducts = productRepository
                .findDistinctByStatus(ProductStatus.SELLING)
                .stream()
                .filter(product -> categoryFilter == null || product.getCategory() == categoryFilter)
                .map(product -> new ScoredProduct(product, calculateRecommendationScore(product)))
                .sorted(comparator(sortType))
                .toList();

        int fromIndex = Math.min(page * size, rankedProducts.size());
        int toIndex = Math.min(fromIndex + size, rankedProducts.size());
        List<BestDealItemResponse> content = rankedProducts.subList(fromIndex, toIndex)
                .stream()
                .map(scored -> toResponse(scored, rankedProducts.indexOf(scored) + 1))
                .toList();

        return new BestDealPageResponse(
                content,
                page,
                size,
                rankedProducts.size(),
                toIndex < rankedProducts.size()
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page는 0 이상, size는 1 이상 100 이하여야 합니다.");
        }
    }

    private ProductCategory parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        try {
            return ProductCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 상품 카테고리입니다.");
        }
    }

    private Comparator<ScoredProduct> comparator(BestDealSort sort) {
        if (sort == BestDealSort.PRICE_ASC) {
            return Comparator.comparingLong(scored -> scored.product().getPrice());
        }
        return Comparator.comparingInt(ScoredProduct::score)
                .reversed()
                .thenComparingLong(scored -> scored.product().getPrice());
    }

    /**
     * AI Best Deal 계약 확정 전까지 사용하는 임시 점수입니다.
     * 상품 상태, 판매자 신뢰도, 정가 대비 절감률을 조합합니다.
     */
    private int calculateRecommendationScore(Product product) {
        int conditionScore = switch (product.getCondition()) {
            case UNOPENED -> 20;
            case LIKE_NEW -> 16;
            case GOOD -> 10;
            case USED -> 4;
        };
        int sellerScore = product.getSellerSnapshot() == null
                ? 0
                : (int) Math.round(product.getSellerSnapshot().getTrustScore() * 0.2);
        int savingsScore = Math.min(10, (int) Math.round(product.calculateSavingsRate() * 0.2));
        return Math.min(100, 50 + conditionScore + sellerScore + savingsScore);
    }

    private BestDealItemResponse toResponse(ScoredProduct scored, int rank) {
        Product product = scored.product();
        String imageUrl = product.getImages().isEmpty()
                ? null
                : product.getImages().getFirst().getImageUrl();
        long officialPrice = product.getReferencePrice() == null
                ? product.getPrice()
                : product.getReferencePrice();

        return new BestDealItemResponse(
                product.getId().toString(),
                rank,
                product.getPlatform(),
                product.getCategory(),
                product.getTitle(),
                product.getPrice(),
                officialPrice,
                product.calculateSavingsAmount(),
                product.calculateSavingsRate(),
                product.getCondition(),
                product.getLocation() == null ? "지역 정보 없음" : product.getLocation(),
                recommendationReason(product),
                scored.score(),
                imageUrl,
                false
        );
    }

    private String recommendationReason(Product product) {
        if (product.getSellerSnapshot() != null && product.getSellerSnapshot().getTrustScore() >= 90) {
            return "판매자 신뢰도와 상품 상태를 함께 고려하면 가장 합리적입니다.";
        }
        if (product.calculateSavingsRate() >= 35) {
            return "정가 대비 절감 폭이 크고 상품 상태도 합리적입니다.";
        }
        return "가격과 상품 상태를 종합해 추천하는 매물입니다.";
    }

    private enum BestDealSort {
        AI_RECOMMENDED,
        PRICE_ASC;

        private static BestDealSort parse(String value) {
            if (value == null || value.isBlank()) {
                return AI_RECOMMENDED;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
            }
        }
    }

    private record ScoredProduct(Product product, int score) {
    }
}
