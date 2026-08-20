package com.hackathon.second_hand_first.activity.controller;

import com.hackathon.second_hand_first.activity.dto.UserDashboardResponse;
import com.hackathon.second_hand_first.activity.service.UserDashboardService;
import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    @GetMapping("/users/me/dashboard")
    public ResponseEntity<ApiResponse<UserDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "마이페이지 요약 정보를 조회했습니다.",
                userDashboardService.getDashboard(userDetails.getUserId())
        ));
    }
}
