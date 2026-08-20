package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRegionResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSellerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUpsertServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductUpsertService productUpsertService;

    @BeforeEach
    void setUp() {
        productUpsertService = new ProductUpsertService(productRepository);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 처음_수집한_외부상품을_저장한다() {
        AiProductResponse response = aiProduct("mock_new", 170_000L, List.of("https://cdn.example.com/new.jpg"));
        when(productRepository.findByPlatformAndExternalProductId(Platform.NAVER_FLEAMARKET, "mock_new"))
                .thenReturn(Optional.empty());

        Product saved = productUpsertService.upsert(response);

        assertThat(saved.getExternalProductId()).isEqualTo("mock_new");
        assertThat(saved.getPrice()).isEqualTo(170_000L);
        assertThat(saved.getImages()).extracting("imageUrl")
                .containsExactly("https://cdn.example.com/new.jpg");
        assertThat(saved.getSellerSnapshot().getSellerName()).isEqualTo("판교 판매자");
        assertThat(saved.getLatitude()).isEqualTo(37.3947);
        assertThat(saved.getLongitude()).isEqualTo(127.1112);
        assertThat(saved.getTradeRegions()).hasSize(1);
        assertThat(saved.getTradeRegions().getFirst().getFullAddress())
                .isEqualTo("경기도 성남시 분당구 판교동");
    }

    @Test
    void 이미_수집한_외부상품의_가격과_이미지를_갱신한다() {
        Product existing = ProductFixture.airPodsPro2();
        AiProductResponse response = aiProduct(
                "mock_1",
                160_000L,
                List.of("https://cdn.example.com/updated-1.jpg", "https://cdn.example.com/updated-2.jpg")
        );
        when(productRepository.findByPlatformAndExternalProductId(Platform.NAVER_FLEAMARKET, "mock_1"))
                .thenReturn(Optional.of(existing));

        Product saved = productUpsertService.upsert(response);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getPrice()).isEqualTo(160_000L);
        assertThat(saved.getImages()).extracting("imageUrl")
                .containsExactly("https://cdn.example.com/updated-1.jpg", "https://cdn.example.com/updated-2.jpg");
    }

    private AiProductResponse aiProduct(String externalProductId, long price, List<String> imageUrls) {
        return new AiProductResponse(
                Platform.NAVER_FLEAMARKET,
                externalProductId,
                "AirPods Pro 2 (USB-C)",
                "AI 서버가 수집한 상품 설명",
                ProductCategory.EARPHONES,
                price,
                299_000L,
                ProductCondition.LIKE_NEW,
                ProductStatus.SELLING,
                new AiLocationResponse(
                        "판교동",
                        "경기도 성남시 분당구 판교동",
                        ProductLocationGeocodeRequest.Precision.FULL,
                        List.of(new AiRegionResponse(
                                "판교동",
                                "경기도 성남시 분당구 판교동",
                                "4113510800",
                                new GeographicCoordinates(37.3947, 127.1112)
                        )),
                        new GeographicCoordinates(37.3947, 127.1112)
                ),
                true,
                true,
                null,
                true,
                "https://fleamarket.naver.com/products/" + externalProductId,
                150L,
                OffsetDateTime.parse("2026-08-20T09:00:00+09:00"),
                imageUrls,
                new AiSellerResponse("seller_1", "판교 판매자", 92, 32, 48.5)
        );
    }
}
