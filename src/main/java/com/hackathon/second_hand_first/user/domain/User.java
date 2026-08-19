package com.hackathon.second_hand_first.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * PasswordEncoder를 통해 암호화된 비밀번호만 저장합니다.
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 프로필 사진이 없는 경우 null입니다.
     */
    @Column(length = 1_000)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean termsAgreed;

    @Column(nullable = false)
    private boolean marketingConsent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private User(
            String name,
            String email,
            String encodedPassword,
            String profileImageUrl,
            boolean termsAgreed,
            boolean marketingConsent
    ) {
        this.name = validateName(name);
        this.email = normalizeEmail(email);
        this.password = validatePassword(encodedPassword);
        this.profileImageUrl = normalizeProfileImageUrl(profileImageUrl);
        this.termsAgreed = termsAgreed;
        this.marketingConsent = marketingConsent;
    }

    public static User create(
            String name,
            String email,
            String encodedPassword,
            String profileImageUrl,
            boolean termsAgreed,
            boolean marketingConsent
    ) {
        return new User(
                name,
                email,
                encodedPassword,
                profileImageUrl,
                termsAgreed,
                marketingConsent
        );
    }

    public void updateProfile(
            String name,
            String profileImageUrl
    ) {
        this.name = validateName(name);
        this.profileImageUrl = normalizeProfileImageUrl(profileImageUrl);
    }

    public void changePassword(String encodedNewPassword) {
        this.password = validatePassword(encodedNewPassword);
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = normalizeProfileImageUrl(profileImageUrl);
    }

    public void deleteProfileImage() {
        this.profileImageUrl = null;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() > 50) {
            throw new IllegalArgumentException("이름은 50자 이하여야 합니다.");
        }

        return trimmedName;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String validatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("암호화된 비밀번호는 필수입니다.");
        }

        return encodedPassword;
    }

    private static String normalizeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }

        return profileImageUrl.trim();
    }
}
