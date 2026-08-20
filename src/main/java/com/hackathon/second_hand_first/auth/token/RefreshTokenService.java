package com.hackathon.second_hand_first.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void save(Long userId, String token, Instant expiresAt) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .map(existing -> existing.update(token, expiresAt))
                .orElseGet(() -> RefreshToken.create(userId, token, expiresAt));
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public boolean matches(Long userId, String token) {
        return refreshTokenRepository.findByUserId(userId)
                .filter(saved -> !saved.isExpired())
                .map(saved -> saved.matches(token))
                .orElse(false);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
