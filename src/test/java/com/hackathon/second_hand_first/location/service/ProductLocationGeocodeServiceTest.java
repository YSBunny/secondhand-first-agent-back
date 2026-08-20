package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import com.hackathon.second_hand_first.location.dto.response.ProductLocationGeocodeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductLocationGeocodeServiceTest {

    private final KakaoLocalService kakaoLocalService = mock(KakaoLocalService.class);
    private final ProductLocationGeocodeService service =
            new ProductLocationGeocodeService(kakaoLocalService);

    @Test
    void fillsCoordinatesFromFullAddress() {
        ProductLocationGeocodeRequest request = request(
                "서창동",
                "인천광역시 남동구 서창동",
                ProductLocationGeocodeRequest.Precision.FULL
        );
        GeographicCoordinates coordinates =
                new GeographicCoordinates(37.435, 126.750);
        when(kakaoLocalService.findCoordinates(request.fullAddress()))
                .thenReturn(Optional.of(coordinates));

        ProductLocationGeocodeResponse response = service.geocode(request);

        assertThat(response.name()).isEqualTo("서창동");
        assertThat(response.fullAddress()).isEqualTo("인천광역시 남동구 서창동");
        assertThat(response.coordinates()).isEqualTo(coordinates);
        verify(kakaoLocalService).findCoordinates("인천광역시 남동구 서창동");
    }

    @Test
    void skipsGeocodingWhenPrecisionIsNone() {
        ProductLocationGeocodeRequest request = request(
                null,
                null,
                ProductLocationGeocodeRequest.Precision.NONE
        );

        ProductLocationGeocodeResponse response = service.geocode(request);

        assertThat(response.coordinates()).isNull();
        verify(kakaoLocalService, never()).findCoordinates(null);
    }

    @Test
    void keepsCoordinatesNullWhenAddressIsNotFound() {
        ProductLocationGeocodeRequest request = request(
                "청라동",
                "인천광역시 서해구 청라동",
                ProductLocationGeocodeRequest.Precision.FULL
        );
        when(kakaoLocalService.findCoordinates(request.fullAddress()))
                .thenReturn(Optional.empty());

        ProductLocationGeocodeResponse response = service.geocode(request);

        assertThat(response.coordinates()).isNull();
    }

    @Test
    void geocodesDuplicateMainAndRegionAddressOnlyOnce() {
        ProductLocationGeocodeRequest request = request(
                "서창동",
                "인천광역시  남동구 서창동 ",
                ProductLocationGeocodeRequest.Precision.FULL
        );
        GeographicCoordinates coordinates =
                new GeographicCoordinates(37.435, 126.750);
        when(kakaoLocalService.findCoordinates("인천광역시 남동구 서창동"))
                .thenReturn(Optional.of(coordinates));

        ProductLocationGeocodeResponse response = service.geocode(request);

        assertThat(response.coordinates()).isEqualTo(coordinates);
        assertThat(response.regions().getFirst().coordinates())
                .isEqualTo(coordinates);
        verify(kakaoLocalService, times(1))
                .findCoordinates("인천광역시 남동구 서창동");
    }

    private ProductLocationGeocodeRequest request(
            String name,
            String fullAddress,
            ProductLocationGeocodeRequest.Precision precision
    ) {
        List<ProductLocationGeocodeRequest.Region> regions =
                fullAddress == null
                        ? List.of()
                        : List.of(new ProductLocationGeocodeRequest.Region(
                                name,
                                fullAddress,
                                null,
                                null
                        ));
        return new ProductLocationGeocodeRequest(
                name,
                fullAddress,
                precision,
                regions,
                null
        );
    }
}
