package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.domain.ProductCategory;
import com.hackathon.second_hand_first.product.domain.ProductCondition;
import com.hackathon.second_hand_first.product.domain.ProductStatus;
import com.hackathon.second_hand_first.product.domain.ProductTradeRegion;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import com.hackathon.second_hand_first.product.support.ProductFixture;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiLocationResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRegionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductLocationEnrichmentServiceTest {

    private static final String PANGYO = "판교";
    private static final String GANGNAM = "서울특별시 강남구 역삼동";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KakaoLocalService kakaoLocalService;

    private ProductLocationEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new ProductLocationEnrichmentService(
                productRepository,
                kakaoLocalService
        );
    }

    @Test
    void reusesSavedCoordinatesForSameProductAndAddress() {
        Product existing = ProductFixture.airPodsPro2();
        existing.updateCoordinates(37.3947, 127.1112);
        AiProductResponse source = product(
                new AiLocationResponse(
                        PANGYO,
                        PANGYO,
                        ProductLocationGeocodeRequest.Precision.FULL,
                        List.of(),
                        null
                )
        );
        when(productRepository.findByPlatformAndExternalProductId(
                source.platform(),
                source.externalProductId()
        )).thenReturn(Optional.of(existing));

        AiProductResponse result = service.enrich(source);

        assertThat(result.location().coordinates())
                .isEqualTo(new GeographicCoordinates(37.3947, 127.1112));
        verify(kakaoLocalService, never()).findCoordinates(PANGYO);
    }

    @Test
    void geocodesMainAddressAndEveryDistinctRegionOnlyOnce() {
        GeographicCoordinates pangyoCoordinates =
                new GeographicCoordinates(37.3947, 127.1112);
        GeographicCoordinates gangnamCoordinates =
                new GeographicCoordinates(37.5007, 127.0365);
        AiProductResponse source = product(
                new AiLocationResponse(
                        PANGYO,
                        PANGYO,
                        ProductLocationGeocodeRequest.Precision.FULL,
                        List.of(
                                new AiRegionResponse(
                                        PANGYO,
                                        PANGYO,
                                        "pangyo-code",
                                        null
                                ),
                                new AiRegionResponse(
                                        "역삼동",
                                        GANGNAM,
                                        "gangnam-code",
                                        null
                                )
                        ),
                        null
                )
        );
        when(productRepository.findByPlatformAndExternalProductId(
                source.platform(),
                source.externalProductId()
        )).thenReturn(Optional.empty());
        when(kakaoLocalService.findCoordinates(PANGYO))
                .thenReturn(Optional.of(pangyoCoordinates));
        when(kakaoLocalService.findCoordinates(GANGNAM))
                .thenReturn(Optional.of(gangnamCoordinates));

        AiProductResponse result = service.enrich(source);

        assertThat(result.location().coordinates())
                .isEqualTo(pangyoCoordinates);
        assertThat(result.location().regions())
                .extracting(AiRegionResponse::coordinates)
                .containsExactly(pangyoCoordinates, gangnamCoordinates);
        verify(kakaoLocalService, times(1)).findCoordinates(PANGYO);
        verify(kakaoLocalService, times(1)).findCoordinates(GANGNAM);
    }

    @Test
    void skipsAllGeocodingWhenPrecisionIsNone() {
        AiProductResponse source = product(
                new AiLocationResponse(
                        PANGYO,
                        PANGYO,
                        ProductLocationGeocodeRequest.Precision.NONE,
                        List.of(new AiRegionResponse(
                                PANGYO,
                                PANGYO,
                                "pangyo-code",
                                new GeographicCoordinates(37.3947, 127.1112)
                        )),
                        new GeographicCoordinates(37.3947, 127.1112)
                )
        );

        AiProductResponse result = service.enrich(source);

        assertThat(result.location().coordinates()).isNull();
        assertThat(result.location().regions())
                .extracting(AiRegionResponse::coordinates)
                .containsExactly((GeographicCoordinates) null);
        verify(productRepository, never())
                .findByPlatformAndExternalProductId(
                        source.platform(),
                        source.externalProductId()
                );
        verify(kakaoLocalService, never()).findCoordinates(PANGYO);
    }

    @Test
    void reusesSavedCoordinatesForSameRegionAddress() {
        Product existing = ProductFixture.airPodsPro2();
        GeographicCoordinates savedCoordinates =
                new GeographicCoordinates(37.5007, 127.0365);
        existing.replaceTradeRegions(List.of(ProductTradeRegion.create(
                "역삼동",
                GANGNAM,
                "gangnam-code",
                savedCoordinates.latitude(),
                savedCoordinates.longitude()
        )));
        AiProductResponse source = product(new AiLocationResponse(
                PANGYO,
                PANGYO,
                ProductLocationGeocodeRequest.Precision.FULL,
                List.of(new AiRegionResponse(
                        "역삼동", GANGNAM, "gangnam-code", null
                )),
                new GeographicCoordinates(37.3947, 127.1112)
        ));
        when(productRepository.findByPlatformAndExternalProductId(
                source.platform(), source.externalProductId()
        )).thenReturn(Optional.of(existing));

        AiProductResponse result = service.enrich(source);

        assertThat(result.location().regions().getFirst().coordinates())
                .isEqualTo(savedCoordinates);
        verify(kakaoLocalService, never()).findCoordinates(GANGNAM);
    }

    private AiProductResponse product(AiLocationResponse location) {
        return new AiProductResponse(
                Platform.NAVER_FLEAMARKET,
                "mock_1",
                "AirPods Pro 2",
                "상품 설명",
                ProductCategory.EARPHONES,
                170_000L,
                299_000L,
                ProductCondition.LIKE_NEW,
                ProductStatus.SELLING,
                location,
                true,
                true,
                null,
                true,
                "https://fleamarket.naver.com/products/mock_1",
                150L,
                OffsetDateTime.parse("2026-08-20T09:00:00+09:00"),
                List.of(),
                null
        );
    }
}
