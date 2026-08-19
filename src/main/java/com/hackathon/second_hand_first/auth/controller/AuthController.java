package com.hackathon.second_hand_first.auth.controller;

import com.hackathon.second_hand_first.auth.dto.request.LoginRequest;
import com.hackathon.second_hand_first.auth.dto.request.PasswordChangeRequest;
import com.hackathon.second_hand_first.auth.dto.request.SignupRequest;
import com.hackathon.second_hand_first.auth.dto.response.LoginResponse;
import com.hackathon.second_hand_first.auth.dto.response.TokenRefreshResponse;
import com.hackathon.second_hand_first.auth.dto.response.UserSummaryResponse;
import com.hackathon.second_hand_first.auth.security.CustomUserDetails;
import com.hackathon.second_hand_first.auth.service.AuthService;
import com.hackathon.second_hand_first.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE =
            "refreshToken";

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request))
        );
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthService.LoginResult result =
                authService.login(request);

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                result.rememberMe()
        );

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그인에 성공했습니다.",
                        result.response()
                )
        );
    }

    /**
     * accessToken 재발급
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>>
    refreshAccessToken(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            ) String refreshToken
    ) {
        TokenRefreshResponse response =
                authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "accessToken을 재발급했습니다.",
                        response
                )
        );
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            HttpServletResponse servletResponse
    ) {
        authService.logout(userDetails.getUserId());

        ResponseCookie expiredCookie =
                createExpiredRefreshTokenCookie();

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                expiredCookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그아웃되었습니다.",
                        null
                )
        );
    }

    /**
     * 비밀번호 변경
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            PasswordChangeRequest request
    ) {
        authService.changePassword(
                userDetails.getUserId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "비밀번호를 변경했습니다.",
                        null
                )
        );
    }

    private ResponseCookie createRefreshTokenCookie(
            String refreshToken,
            boolean rememberMe
    ) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(
                                REFRESH_TOKEN_COOKIE,
                                refreshToken
                        )
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/auth");

        if (rememberMe) {
            builder.maxAge(Duration.ofDays(14));
        }

        return builder.build();
    }

    private ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        ""
                )
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
