package com.hackathon.second_hand_first.activity.service;

import com.hackathon.second_hand_first.activity.domain.PlatformRedirectHistory;
import com.hackathon.second_hand_first.activity.dto.PlatformRedirectResponse;
import com.hackathon.second_hand_first.activity.exception.RedirectForbiddenException;
import com.hackathon.second_hand_first.activity.repository.PlatformRedirectHistoryRepository;
import com.hackathon.second_hand_first.product.domain.Platform;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.activity.exception.RedirectTargetNotFoundException;
import com.hackathon.second_hand_first.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlatformRedirectService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ProductRepository productRepository;
    private final PlatformRedirectHistoryRepository redirectHistoryRepository;
    private final Clock clock;

    @Transactional
    public PlatformRedirectResponse record(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("요청 값이 올바르지 않습니다.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(RedirectTargetNotFoundException::new);
        validatePlatformUrl(product.getPlatform(), product.getPlatformUrl());

        PlatformRedirectHistory saved = redirectHistoryRepository.save(
                PlatformRedirectHistory.create(
                        userId,
                        product,
                        product.getPlatformUrl(),
                        LocalDateTime.now(clock.withZone(SEOUL))
                )
        );
        return PlatformRedirectResponse.from(saved);
    }

    private void validatePlatformUrl(Platform platform, String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !isAllowedHost(platform, host.toLowerCase(Locale.ROOT))) {
                throw new RedirectForbiddenException();
            }
        } catch (IllegalArgumentException exception) {
            throw new RedirectForbiddenException();
        }
    }

    private boolean isAllowedHost(Platform platform, String host) {
        return switch (platform) {
            case BUNJANG -> matchesDomain(host, "bunjang.co.kr");
            case JOONGNA -> matchesDomain(host, "joongna.com");
            case NAVER_FLEAMARKET -> matchesDomain(host, "naver.com");
            case ELEVENST -> matchesDomain(host, "11st.co.kr");
        };
    }

    private boolean matchesDomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }
}
