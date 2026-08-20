package com.hackathon.second_hand_first.product.service;

import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductDistanceRankingService {

    private static final double EARTH_RADIUS_KM = 6_371.0088;

    public List<ObjectNode> rankByDistance(
            CoordinateResponse userCoordinates,
            List<JsonNode> products
    ) {
        validateCoordinates(
                userCoordinates.latitude(),
                userCoordinates.longitude(),
                "사용자"
        );

        List<RankedProduct> rankedProducts = new ArrayList<>();
        for (JsonNode product : products) {
            if (!(product instanceof ObjectNode productObject)) {
                throw new IllegalArgumentException("각 상품은 JSON 객체여야 합니다.");
            }

            ObjectNode copiedProduct = productObject.deepCopy();
            NearestLocation nearestLocation = findNearestLocation(
                    userCoordinates.latitude(),
                    userCoordinates.longitude(),
                    copiedProduct
            );
            rankedProducts.add(new RankedProduct(copiedProduct, nearestLocation));
        }

        rankedProducts.sort(Comparator.comparing(
                ranked -> ranked.nearestLocation() == null
                        ? Double.POSITIVE_INFINITY
                        : ranked.nearestLocation().distanceKm()
        ));

        int rank = 1;
        for (RankedProduct rankedProduct : rankedProducts) {
            writeDistanceResult(rankedProduct, rank);
            if (rankedProduct.nearestLocation() != null) {
                rank++;
            }
        }

        return rankedProducts.stream()
                .map(RankedProduct::product)
                .toList();
    }

    double calculateDistanceKm(
            double firstLatitude,
            double firstLongitude,
            double secondLatitude,
            double secondLongitude
    ) {
        double latitudeDelta = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDelta = Math.toRadians(secondLongitude - firstLongitude);
        double firstLatitudeRadians = Math.toRadians(firstLatitude);
        double secondLatitudeRadians = Math.toRadians(secondLatitude);

        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(firstLatitudeRadians)
                * Math.cos(secondLatitudeRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double boundedHaversine = Math.min(1, haversine);
        double centralAngle = 2 * Math.atan2(
                Math.sqrt(boundedHaversine),
                Math.sqrt(1 - boundedHaversine)
        );
        return EARTH_RADIUS_KM * centralAngle;
    }

    private NearestLocation findNearestLocation(
            double userLatitude,
            double userLongitude,
            ObjectNode product
    ) {
        List<Coordinates> productCoordinates = extractCoordinates(product);
        NearestLocation nearest = null;

        for (Coordinates coordinates : productCoordinates) {
            double distanceKm = calculateDistanceKm(
                    userLatitude,
                    userLongitude,
                    coordinates.latitude(),
                    coordinates.longitude()
            );
            if (nearest == null || distanceKm < nearest.distanceKm()) {
                nearest = new NearestLocation(coordinates, distanceKm);
            }
        }
        return nearest;
    }

    private List<Coordinates> extractCoordinates(ObjectNode product) {
        List<Coordinates> coordinates = new ArrayList<>();
        JsonNode location = product.path("location");
        addCoordinates(location.path("coordinates"), coordinates);

        JsonNode regions = location.path("regions");
        if (regions.isArray()) {
            for (JsonNode region : regions) {
                addCoordinates(region.path("coordinates"), coordinates);
            }
        }
        return coordinates;
    }

    private void addCoordinates(JsonNode node, List<Coordinates> coordinates) {
        if (node.isArray()) {
            for (JsonNode coordinate : node) {
                addCoordinates(coordinate, coordinates);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        JsonNode latitudeNode = node.path("latitude");
        JsonNode longitudeNode = node.path("longitude");
        if (!latitudeNode.isNumber() || !longitudeNode.isNumber()) {
            return;
        }

        double latitude = latitudeNode.doubleValue();
        double longitude = longitudeNode.doubleValue();
        if (isValidCoordinates(latitude, longitude)) {
            coordinates.add(new Coordinates(latitude, longitude));
        }
    }

    private void writeDistanceResult(RankedProduct rankedProduct, int rank) {
        ObjectNode product = rankedProduct.product();
        NearestLocation nearest = rankedProduct.nearestLocation();
        if (nearest == null) {
            product.putNull("distance_km");
            product.putNull("distance_rank");
            product.putNull("nearest_coordinates");
            return;
        }

        product.put("distance_km", nearest.distanceKm());
        product.put("distance_rank", rank);
        ObjectNode nearestCoordinates = product.putObject("nearest_coordinates");
        nearestCoordinates.put("latitude", nearest.coordinates().latitude());
        nearestCoordinates.put("longitude", nearest.coordinates().longitude());
    }

    private void validateCoordinates(
            double latitude,
            double longitude,
            String target
    ) {
        if (!isValidCoordinates(latitude, longitude)) {
            throw new IllegalArgumentException(
                    target + "의 위도 또는 경도가 올바르지 않습니다."
            );
        }
    }

    private boolean isValidCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private record Coordinates(double latitude, double longitude) {
    }

    private record NearestLocation(
            Coordinates coordinates,
            double distanceKm
    ) {
    }

    private record RankedProduct(
            ObjectNode product,
            NearestLocation nearestLocation
    ) {
    }
}
