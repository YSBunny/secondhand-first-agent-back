package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDistanceRankingServiceTest {

    private final ProductDistanceRankingService service =
            new ProductDistanceRankingService();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void calculatesKnownDistanceWithHaversineFormula() {
        double distance = service.calculateDistanceKm(
                37.5665,
                126.9780,
                35.1796,
                129.0756
        );

        assertThat(distance).isBetween(324.0, 326.0);
    }

    @Test
    void selectsNearestCoordinatesAndRanksAllProducts() throws Exception {
        JsonNode sameLocation = product("""
                {
                  "platform_product_id": "same",
                  "location": {
                    "coordinates": {
                      "latitude": 37.5665,
                      "longitude": 126.9780
                    }
                  }
                }
                """);
        JsonNode multipleLocations = product("""
                {
                  "platform_product_id": "multiple",
                  "location": {
                    "coordinates": [
                      {"latitude": 35.1796, "longitude": 129.0756},
                      {"latitude": 37.5765, "longitude": 126.9780}
                    ]
                  }
                }
                """);
        JsonNode farLocation = product("""
                {
                  "platform_product_id": "far",
                  "location": {
                    "coordinates": {
                      "latitude": 35.1796,
                      "longitude": 129.0756
                    }
                  }
                }
                """);
        JsonNode noLocation = product("""
                {
                  "platform_product_id": "none",
                  "location": {
                    "precision": "NONE",
                    "coordinates": null
                  }
                }
                """);

        List<ObjectNode> result = service.rankByDistance(
                new CoordinateResponse("서울특별시 중구", 37.5665, 126.9780),
                List.of(farLocation, noLocation, multipleLocations, sameLocation)
        );

        assertThat(result)
                .extracting(node -> node.path("platform_product_id").asString())
                .containsExactly("same", "multiple", "far", "none");
        assertThat(result)
                .extracting(node -> node.path("distance_rank").isNull()
                        ? null
                        : node.path("distance_rank").intValue())
                .containsExactly(1, 2, 3, null);
        assertThat(result.get(1).path("nearest_coordinates").path("latitude").doubleValue())
                .isEqualTo(37.5765);
        assertThat(result.get(1).path("distance_km").doubleValue())
                .isLessThan(2.0);
        assertThat(result.get(3).path("distance_km").isNull()).isTrue();
        assertThat(farLocation.has("distance_rank")).isFalse();
    }

    @Test
    void readsCoordinatesFromEveryRegion() throws Exception {
        JsonNode product = product("""
                {
                  "platform_product_id": "regions",
                  "location": {
                    "coordinates": null,
                    "regions": [
                      {
                        "name": "부산",
                        "coordinates": {"latitude": 35.1796, "longitude": 129.0756}
                      },
                      {
                        "name": "서울",
                        "coordinates": {"latitude": 37.5665, "longitude": 126.9780}
                      }
                    ]
                  }
                }
                """);

        List<ObjectNode> result = service.rankByDistance(
                new CoordinateResponse("서울특별시 중구", 37.5665, 126.9780),
                List.of(product)
        );

        assertThat(result.getFirst().path("distance_km").doubleValue()).isZero();
        assertThat(result.getFirst().path("nearest_coordinates").path("latitude").doubleValue())
                .isEqualTo(37.5665);
    }

    private JsonNode product(String json) throws Exception {
        return jsonMapper.readTree(json);
    }
}
