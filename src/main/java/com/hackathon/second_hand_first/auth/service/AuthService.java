package com.hackathon.second_hand_first.auth.service;

import com.hackathon.second_hand_first.auth.dto.request.LoginRequest;
import com.hackathon.second_hand_first.auth.dto.request.PasswordChangeRequest;
import com.hackathon.second_hand_first.auth.dto.request.SignupRequest;
import com.hackathon.second_hand_first.auth.dto.response.LoginResponse;
import com.hackathon.second_hand_first.auth.dto.response.TokenRefreshResponse;
import com.hackathon.second_hand_first.auth.dto.response.UserSummaryResponse;
import com.hackathon.second_hand_first.auth.exception.UnauthorizedException;
import com.hackathon.second_hand_first.auth.token.RefreshTokenService;
import com.hackathon.second_hand_first.auth.token.TokenProvider;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserSummaryResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = User.create(
                request.name(),
                email,
                passwordEncoder.encode(request.password()),
                request.profileImageUrl(),
                request.termsAgreed(),
                request.marketingConsent()
        );
        User saved = userRepository.save(user);
        return new UserSummaryResponse(
                saved.getId(), saved.getName(), saved.getEmail(), saved.getProfileImageUrl()
        );
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(
                        () -> new UnauthorizedException(
                                "이메일 또는 비밀번호가 일치하지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new UnauthorizedException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        String accessToken =
                tokenProvider.createAccessToken(user.getId());

        String refreshToken =
                tokenProvider.createRefreshToken(user.getId());

        long accessTokenExpiresIn =
                tokenProvider.getAccessTokenExpirationSeconds();

        long refreshTokenExpiresIn =
                tokenProvider.getRefreshTokenExpirationSeconds();

        refreshTokenService.save(
                user.getId(),
                refreshToken,
                Instant.now().plusSeconds(refreshTokenExpiresIn)
        );

        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                "Bearer",
                accessTokenExpiresIn,
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getProfileImageUrl()
                )
        );

        return new LoginResult(
                loginResponse,
                refreshToken,
                request.rememberMe()
        );
    }

    /**
     * accessToken 재발급
     */
    public TokenRefreshResponse refreshAccessToken(
            String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(
                    "다시 로그인해 주세요."
            );
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException(
                    "다시 로그인해 주세요."
            );
        }

        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException(
                    "유효하지 않은 토큰입니다."
            );
        }

        Long userId =
                tokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.matches(
                userId,
                refreshToken
        )) {
            throw new UnauthorizedException(
                    "다시 로그인해 주세요."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UnauthorizedException(
                                "다시 로그인해 주세요."
                        )
                );

        String newAccessToken =
                tokenProvider.createAccessToken(user.getId());

        return new TokenRefreshResponse(
                newAccessToken,
                "Bearer",
                tokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.deleteByUserId(userId);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(
            Long userId,
            PasswordChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UnauthorizedException(
                                "존재하지 않는 사용자입니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()
        )) {
            throw new UnauthorizedException(
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.newPassword());

        user.changePassword(encodedPassword);
    }

    /**
     * Controller에 로그인 응답과 refreshToken을 함께 전달하기 위한
     * 서비스 내부 결과 객체
     */
    public record LoginResult(
            LoginResponse response,
            String refreshToken,
            boolean rememberMe
    ) {
    }
}
