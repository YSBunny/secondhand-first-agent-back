package com.hackathon.second_hand_first.location.controller;

import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.location.dto.request.ProductLocationGeocodeRequest;
import com.hackathon.second_hand_first.location.dto.response.LocationCandidateResponse;
import com.hackathon.second_hand_first.location.dto.response.ProductLocationGeocodeResponse;
import com.hackathon.second_hand_first.location.service.KakaoLocalService;
import com.hackathon.second_hand_first.location.service.ProductLocationGeocodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationSearchController {

    private final KakaoLocalService kakaoLocalService;
    private final ProductLocationGeocodeService productLocationGeocodeService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationCandidateResponse>>> searchLocations(
            @RequestParam String query
    ) {
        List<LocationCandidateResponse> candidates =
                kakaoLocalService.searchCandidates(query);

        return ResponseEntity.ok(
                ApiResponse.success("지역 후보를 조회했습니다.", candidates)
        );
    }

    @PostMapping("/geocode")
    public ResponseEntity<ApiResponse<ProductLocationGeocodeResponse>> geocodeProductLocation(
            @Valid @RequestBody ProductLocationGeocodeRequest request
    ) {
        ProductLocationGeocodeResponse response =
                productLocationGeocodeService.geocode(request);

        return ResponseEntity.ok(
                ApiResponse.success("상품 위치의 좌표를 조회했습니다.", response)
        );
    }
}
