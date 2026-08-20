package com.hackathon.second_hand_first.location.controller;

import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.location.dto.request.UpdateLocationRequest;
import com.hackathon.second_hand_first.location.dto.response.CoordinateResponse;
import com.hackathon.second_hand_first.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/location")
public class LocationController {

    private final LocationService locationService;

    @PatchMapping
    public ResponseEntity<ApiResponse<CoordinateResponse>> updateLocation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        CoordinateResponse response = locationService.updateLocation(
                userDetails.getUserId(),
                request.region()
        );

        return ResponseEntity.ok(
                ApiResponse.success("활동 지역을 변경했습니다.", response)
        );
    }
}