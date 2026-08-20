package com.hackathon.second_hand_first.user.controller;

import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import com.hackathon.second_hand_first.user.dto.request.UserProfileUpdateRequest;
import com.hackathon.second_hand_first.user.dto.response.UserProfileResponse;
import com.hackathon.second_hand_first.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final UserService userService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

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

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse servletResponse
    ) {
        userService.deleteAccount(userDetails.getUserId());

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                createExpiredRefreshTokenCookie().toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success("회원 탈퇴가 완료되었습니다.", null)
        );
    }

    private ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
