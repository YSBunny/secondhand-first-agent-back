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
            case NEW -> 20;
            case LIKE_NEW -> 16;
            case LIGHTLY_USED -> 10;
            case USED -> 4;
            // TODO(팀 논의) UNSPECIFIED/UNKNOWN 점수는 잠정값이다.
            // UNSPECIFIED는 판매자가 상태를 안 적은 것이고 UNKNOWN은 우리가 해석하지
            // 못한 것이라, 둘 다 "상태가 나쁘다"는 뜻이 아니다. USED(4)로 두면 모르는
            // 것을 나쁜 상태로 단정하게 되고, LIKE_NEW(16)로 두면 없는 정보를 좋게
            // 지어내게 된다. 우선 중간값을 둔다.
            // 참고: AI 쪽(tools.py)은 미상을 50으로 두어 USED(60)보다 낮게 잡는다.
            // 두 기준이 서로 달라 정렬 결과가 갈릴 수 있으므로 합의가 필요하다.
            case UNSPECIFIED, UNKNOWN -> 10;
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
