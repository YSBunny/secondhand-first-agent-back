package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.product.domain.DeliveryFee;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductDelivery;
import com.hackathon.second_hand_first.product.domain.ProductTradeRegion;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.TradeType;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSellerResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryExtraCostResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
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

    /**
     * 옵션을 비운 뒤 새로 넣기 전에 flush 하는 데 쓴다.
     *
     * <p>{@code product_delivery_options} 에 (배송정보, 순서) 유니크 제약이 있는데,
     * Hibernate 는 한 flush 안에서 <b>지우기보다 넣기를 먼저</b> 한다. 중간에 한 번
     * 비워 주지 않으면 같은 순서 번호가 겹쳐 제약을 위반한다.
     */
    private final EntityManager entityManager;

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
                        null,
                        source.condition(),
                        source.status(),
                        source.location() == null
                                ? null
                                : source.location().fullAddress(),
                        source.supports(TradeType.DIRECT),
                        source.supports(TradeType.DELIVERY),
                        toDeliveryFee(source.deliveryFee()),
                        isCarbonReductionEligible(source.platform(), source.condition()),
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
                null,
                source.condition(),
                source.status(),
                source.location() == null
                        ? null
                        : source.location().fullAddress(),
                source.supports(TradeType.DIRECT),
                source.supports(TradeType.DELIVERY),
                toDeliveryFee(source.deliveryFee()),
                isCarbonReductionEligible(source.platform(), source.condition()),
                source.platformUrl(),
                source.externalViewCount(),
                publishedAt,
                refreshedAt
        );
        updateCoordinates(product, source.location());
        product.replaceTradeRegions(toTradeRegions(source.location()));
        product.replaceImages(source.imageUrls());
        updateDelivery(product, source.deliveryFee());
        updateSeller(product, source.seller(), refreshedAt);
        return productRepository.save(product);
    }

    private void updateDelivery(Product product, AiDeliveryFeeResponse source) {
        if (source == null) {
            product.replaceDelivery(null);
            return;
        }
        if (source.status() == null) {
            throw new IllegalArgumentException("AI 배송 응답의 상태가 누락되었습니다.");
        }

        AiDeliveryExtraCostResponse extraCost = source.extraCost();
        // 이미 있으면 그 행을 갱신한다. 새 인스턴스로 갈아끼우면
        // product_deliveries.product_id 유니크 제약에 걸린다 — Hibernate 가
        // 옛 행을 지우기 전에 새 행을 넣기 때문이다.
        ProductDelivery delivery = product.getDelivery();
        if (delivery == null) {
            delivery = ProductDelivery.create(
                    product,
                    source.status(),
                    source.payer(),
                    source.minFee(),
                    source.homeDeliveryFee(),
                    extraCost == null ? null : extraCost.jejuFee(),
                    extraCost == null ? null : extraCost.remoteAreaFee(),
                    extraCost == null ? null : extraCost.description()
            );
        } else {
            delivery.update(
                    source.status(),
                    source.payer(),
                    source.minFee(),
                    source.homeDeliveryFee(),
                    extraCost == null ? null : extraCost.jejuFee(),
                    extraCost == null ? null : extraCost.remoteAreaFee(),
                    extraCost == null ? null : extraCost.description()
            );
            // 비운 옵션을 지금 지운다. 새 옵션을 넣은 뒤에 지우면 순서 번호가 겹친다.
            entityManager.flush();
        }

        if (source.options() != null) {
            for (int index = 0; index < source.options().size(); index++) {
                var option = source.options().get(index);
                if (option == null) {
                    continue;
                }
                delivery.addOption(
                        option.method(),
                        option.carrier(),
                        option.requiresPickupPoint(),
                        option.fee(),
                        option.rawCode() == null ? null : option.rawCode().toString(),
                        index
                );
            }
        }
        product.replaceDelivery(delivery);
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
        if (source == null || source.status() != com.hackathon.second_hand_first.product.domain.DeliveryStatus.AVAILABLE) {
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
                || source.tradeTypes() == null) {
            throw new IllegalArgumentException("AI 상품 응답의 필수 값이 누락되었습니다.");
        }
    }

    private boolean isCarbonReductionEligible(
            Platform platform,
            ProductCondition condition
    ) {
        return platform != Platform.ELEVENST || condition == ProductCondition.USED;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

}
