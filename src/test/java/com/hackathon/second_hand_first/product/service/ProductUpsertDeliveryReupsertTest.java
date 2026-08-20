package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.product.domain.DeliveryPayer;
import com.hackathon.second_hand_first.product.domain.DeliveryStatus;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.TradeType;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.domain.DeliveryCarrier;
import com.hackathon.second_hand_first.product.domain.DeliveryMethod;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryFeeResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiDeliveryOptionResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 상품을 다시 적재해도 배송 정보 때문에 터지지 않는지 본다.
 *
 * <p>{@code product_deliveries.product_id} 에 유니크 제약이 있다. 배송 정보를 새
 * 인스턴스로 갈아끼우면 Hibernate 가 <b>옛 행을 지우기 전에 새 행을 넣어</b>
 * 제약을 위반한다. 실제로 같은 검색어를 두 번 넣으면 두 번째가 500 이었다.
 */
@SpringBootTest
@Transactional
class ProductUpsertDeliveryReupsertTest {

    @Autowired
    private ProductUpsertService productUpsertService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private AiProductResponse product(Long minFee, DeliveryPayer payer) {
        return new AiProductResponse(
                Platform.BUNJANG,
                "reupsert-1",
                "에어팟 프로 3",
                "설명",
                ProductCategory.EARPHONES,
                250_000L,
                ProductCondition.NEW,
                ProductStatus.SELLING,
                null,
                List.of(TradeType.DELIVERY),
                new AiDeliveryFeeResponse(
                        DeliveryStatus.AVAILABLE, payer, minFee, minFee, null,
                        // 옵션을 반드시 넣는다. 비워 두면 순서 번호 충돌을 놓친다.
                        List.of(
                                new AiDeliveryOptionResponse(
                                        DeliveryMethod.STANDARD, null, false, minFee, null),
                                new AiDeliveryOptionResponse(
                                        DeliveryMethod.CONVENIENCE_STORE, DeliveryCarrier.CU,
                                        true, minFee - 1_000, null)
                        )
                ),
                "https://m.bunjang.co.kr/products/1",
                null,
                null,
                List.of(),
                null
        );
    }

    @Test
    @DisplayName("같은 상품을 다시 적재해도 실패하지 않는다")
    void reUpsertDoesNotFail() {
        productUpsertService.upsert(product(3_000L, DeliveryPayer.BUYER));
        entityManager.flush();

        productUpsertService.upsert(product(4_000L, DeliveryPayer.SELLER));
        entityManager.flush();

        Product saved = productRepository
                .findByPlatformAndExternalProductId(Platform.BUNJANG, "reupsert-1")
                .orElseThrow();

        assertThat(saved.getDelivery()).isNotNull();
        assertThat(saved.getDelivery().getMinFee())
                .as("두 번째 값으로 갱신돼야 한다")
                .isEqualTo(4_000L);
        assertThat(saved.getDelivery().getPayer()).isEqualTo(DeliveryPayer.SELLER);
        assertThat(saved.getDelivery().getOptions())
                .as("옵션도 새 값으로 갈아끼워진다 — 옛 것이 남으면 안 된다")
                .hasSize(2);
        assertThat(saved.getDelivery().getOptions())
                .extracting(option -> option.getFee())
                .containsExactlyInAnyOrder(4_000L, 3_000L);
    }

    @Test
    @DisplayName("배송 정보가 사라지면 행도 지워진다")
    void deliveryCanBeRemoved() {
        productUpsertService.upsert(product(3_000L, DeliveryPayer.BUYER));
        entityManager.flush();

        AiProductResponse withoutDelivery = new AiProductResponse(
                Platform.BUNJANG, "reupsert-1", "에어팟 프로 3", "설명",
                ProductCategory.EARPHONES, 250_000L, ProductCondition.NEW,
                ProductStatus.SELLING, null, List.of(TradeType.DIRECT),
                null, "https://m.bunjang.co.kr/products/1",
                null, null, List.of(), null
        );
        productUpsertService.upsert(withoutDelivery);
        entityManager.flush();

        Product saved = productRepository
                .findByPlatformAndExternalProductId(Platform.BUNJANG, "reupsert-1")
                .orElseThrow();

        assertThat(saved.getDelivery()).isNull();
    }
}
