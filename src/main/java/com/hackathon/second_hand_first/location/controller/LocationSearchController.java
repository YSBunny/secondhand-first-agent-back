package com.hackathon.second_hand_first.location.controller;

import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.location.dto.response.LocationCandidateResponse;
import com.hackathon.second_hand_first.location.service.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationSearchController {

    private final KakaoLocalService kakaoLocalService;

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
}
