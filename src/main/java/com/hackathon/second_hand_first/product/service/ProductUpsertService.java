package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.product.domain.DeliveryFee;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductTradeRegion;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSellerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductUpsertService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ProductRepository productRepository;

    @Transactional
    public Product upsert(AiProductResponse source) {
        validateRequiredFields(source);
        LocalDateTime refreshedAt = LocalDateTime.now(SEOUL);
        LocalDateTime publishedAt = source.publishedAt() == null
                ? null
                : source.publishedAt().atZoneSameInstant(SEOUL).toLocalDateTime();

        Product product = productRepository
                .findByPlatformAndExternalProductId(source.platform(), source.externalProductId())
                .orElseGet(() -> Product.create(
                        source.platform(),
                        source.externalProductId(),
                        source.title(),
                        source.description(),
                        source.category(),
                        source.price(),
                        source.referencePrice(),
                        source.condition(),
                        source.status(),
                        source.location() == null
                                ? null
                                : source.location().fullAddress(),
                        source.directTradeAvailable(),
                        source.shippingAvailable(),
                        toDeliveryFee(source.deliveryFee()),
                        source.carbonReductionEligible(),
                        source.platformUrl(),
                        source.externalViewCount(),
                        publishedAt,
                        refreshedAt
                ));

        product.refresh(
                source.title(),
                source.description(),
                source.category(),
                source.price(),
                source.referencePrice(),
                source.condition(),
                source.status(),
                source.location() == null
                        ? null
                        : source.location().fullAddress(),
                source.directTradeAvailable(),
                source.shippingAvailable(),
                toDeliveryFee(source.deliveryFee()),
                source.carbonReductionEligible(),
                source.platformUrl(),
                source.externalViewCount(),
                publishedAt,
                refreshedAt
        );
        updateCoordinates(product, source.location());
        product.replaceTradeRegions(toTradeRegions(source.location()));
        product.replaceImages(source.imageUrls());
        updateSeller(product, source.seller(), refreshedAt);
        return productRepository.save(product);
    }

    private List<ProductTradeRegion> toTradeRegions(AiLocationResponse location) {
        if (location == null || location.regions() == null) {
            return List.of();
        }
        return location.regions().stream()
                .filter(region -> region != null)
                .map(region -> ProductTradeRegion.create(
                        region.name(), region.fullAddress(), region.code(),
                        region.coordinates() == null
                                ? null : region.coordinates().latitude(),
                        region.coordinates() == null
                                ? null : region.coordinates().longitude()
                ))
                .toList();
    }

    private void updateCoordinates(
            Product product,
            AiLocationResponse location
    ) {
        if (location == null || location.coordinates() == null) {
            product.updateCoordinates(null, null);
            return;
        }

        GeographicCoordinates coordinates = location.coordinates();

        product.updateCoordinates(
                coordinates.latitude(),
                coordinates.longitude()
        );
    }

    private void updateSeller(Product product, AiSellerResponse seller, LocalDateTime capturedAt) {
        if (seller == null) {
            product.clearSellerSnapshot();
            return;
        }
        product.updateSellerSnapshot(
                seller.externalSellerId(),
                seller.name(),
                defaultZero(seller.trustScore()),
                defaultZero(seller.tradeCount()),
                seller.mannerTemperature(),
                capturedAt
        );
    }

    /**
     * AI가 준 배송비를 도메인 값 객체로 옮긴다.
     *
     * <p>없거나 택배 불가면 빈 값으로 둔다. 0으로 채우면 "배송비를 모른다"와
     * "무료배송"이 구분되지 않아 그 매물이 총액 1위로 올라간다.
     */
    private DeliveryFee toDeliveryFee(AiDeliveryFeeResponse source) {
        if (source == null || !"AVAILABLE".equals(source.status())) {
            return DeliveryFee.unavailable();
        }
        return DeliveryFee.of(source.minFee(), source.homeDeliveryFee(), source.payer());
    }

    private void validateRequiredFields(AiProductResponse source) {
        if (source == null
                || source.platform() == null
                || source.price() == null
                || source.category() == null
                || source.condition() == null
                || source.status() == null
                || source.directTradeAvailable() == null
                || source.shippingAvailable() == null
                || source.carbonReductionEligible() == null
                || source.externalViewCount() == null) {
            throw new IllegalArgumentException("AI 상품 응답의 필수 값이 누락되었습니다.");
        }
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

}
