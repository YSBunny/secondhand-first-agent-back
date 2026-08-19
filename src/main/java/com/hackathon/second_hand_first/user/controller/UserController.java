package com.hackathon.second_hand_first.user.controller;

import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.user.dto.request.UserProfileUpdateRequest;
import com.hackathon.second_hand_first.user.dto.response.UserProfileResponse;
import com.hackathon.second_hand_first.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "프로필을 조회했습니다.", userService.getProfile(userDetails.getUserId())
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "프로필을 수정했습니다.",
                userService.updateProfile(userDetails.getUserId(), request)
        ));
    }
}
