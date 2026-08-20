package com.hackathon.second_hand_first.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TokenProvider {

    private static final String HEADER = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    private static final Pattern SUBJECT = Pattern.compile("\\\"sub\\\":\\\"(\\d+)\\\"");
    private static final Pattern TYPE = Pattern.compile("\\\"type\\\":\\\"(access|refresh)\\\"");
    private static final Pattern EXPIRATION = Pattern.compile("\\\"exp\\\":(\\d+)");

    private final byte[] secret;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public TokenProvider(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.access-token-seconds:3600}") long accessTokenExpirationSeconds,
            @Value("${app.auth.refresh-token-seconds:1209600}") long refreshTokenExpirationSeconds
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, "access", accessTokenExpirationSeconds);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, "refresh", refreshTokenExpirationSeconds);
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = parts[2].getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, actual)
                    && getExpiration(token) > Instant.now().getEpochSecond()
                    && getUserId(token) > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(readClaim(token, TYPE));
    }

    public boolean isAccessToken(String token) {
        return "access".equals(readClaim(token, TYPE));
    }

    public Long getUserId(String token) {
        return Long.valueOf(readClaim(token, SUBJECT));
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private String createToken(Long userId, String type, long validitySeconds) {
        long now = Instant.now().getEpochSecond();
        String payload = "{\"sub\":\"" + userId + "\",\"type\":\"" + type
                + "\",\"iat\":" + now + ",\"exp\":" + (now + validitySeconds) + "}";
        String unsignedToken = HEADER + "." + base64Url(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    private long getExpiration(String token) {
        return Long.parseLong(readClaim(token, EXPIRATION));
    }

    private String readClaim(String token, Pattern pattern) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing token claim");
        }
        return matcher.group(1);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("토큰 생성에 실패했습니다.", exception);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
