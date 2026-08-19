package com.hackathon.second_hand_first.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    private RefreshToken(Long userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(Long userId, String token, Instant expiresAt) {
        return new RefreshToken(userId, token, expiresAt);
    }

    public RefreshToken update(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
        return this;
    }

    public boolean matches(String token) {
        return this.token.equals(token);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
