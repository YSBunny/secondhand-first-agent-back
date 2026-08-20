package com.hackathon.second_hand_first.location.service;

import com.hackathon.second_hand_first.location.dto.response.GeographicCoordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoLocalServiceTest {

    private MockRestServiceServer server;
    private KakaoLocalService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://dapi.kakao.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KakaoLocalService(builder.build());
    }

    @Test
    void returnsLatitudeAndLongitudeFromKakaoCoordinates() {
        expectAddressSearch("인천광역시 남동구 서창동", kakaoResponse("126.750", "37.435"));

        Optional<GeographicCoordinates> result =
                service.findCoordinates("인천광역시 남동구 서창동");

        assertThat(result).contains(new GeographicCoordinates(37.435, 126.750));
        server.verify();
    }

    @Test
    void retriesWithoutAdministrativeDongNumber() {
        expectAddressSearch("경기도 고양시 덕양구 화정1동", "{\"documents\":[]}");
        expectAddressSearch(
                "경기도 고양시 덕양구 화정동",
                kakaoResponse("126.832", "37.634")
        );

        Optional<GeographicCoordinates> result =
                service.findCoordinates("경기도 고양시 덕양구 화정1동");

        assertThat(result).contains(new GeographicCoordinates(37.634, 126.832));
        server.verify();
    }

    @Test
    void returnsEmptyWhenAddressCannotBeFound() {
        expectAddressSearch("인천광역시 서해구 청라동", "{\"documents\":[]}");

        Optional<GeographicCoordinates> result =
                service.findCoordinates("인천광역시 서해구 청라동");

        assertThat(result).isEmpty();
        server.verify();
    }

    private void expectAddressSearch(String query, String responseBody) {
        server.expect(request -> assertAddressRequest(request.getURI(), query))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void assertAddressRequest(URI uri, String query) {
        var parameters = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        assertThat(uri.getPath()).isEqualTo("/v2/local/search/address.json");
        assertThat(UriUtils.decode(
                parameters.getFirst("query"),
                StandardCharsets.UTF_8
        )).isEqualTo(query);
        assertThat(parameters.getFirst("size")).isEqualTo("1");
    }

    private String kakaoResponse(String longitude, String latitude) {
        return """
                {
                  "documents": [
                    {
                      "address_name": "인천광역시 남동구 서창동",
                      "x": "%s",
                      "y": "%s"
                    }
                  ]
                }
                """.formatted(longitude, latitude);
    }
}
