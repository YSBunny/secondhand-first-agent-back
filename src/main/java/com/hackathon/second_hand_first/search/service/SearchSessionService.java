package com.hackathon.second_hand_first.search.service;

import com.hackathon.second_hand_first.search.application.AiSearchClient;
import com.hackathon.second_hand_first.product.domain.Product;
import com.hackathon.second_hand_first.product.service.ProductUpsertService;
import com.hackathon.second_hand_first.search.domain.SearchResult;
import com.hackathon.second_hand_first.search.domain.SearchMessage;
import com.hackathon.second_hand_first.search.domain.SearchSession;
import com.hackathon.second_hand_first.search.dto.request.SearchSessionCreateRequest;
import com.hackathon.second_hand_first.search.dto.response.RecentSearchSessionResponse;
import com.hackathon.second_hand_first.search.dto.response.SearchSessionCreateResponse;
import com.hackathon.second_hand_first.search.dto.response.SearchSessionPageResponse;
import com.hackathon.second_hand_first.search.dto.response.SearchSessionDetailResponse;
import com.hackathon.second_hand_first.search.exception.SearchSessionNotFoundException;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiParsedConditionsResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiRecommendedProductResponse;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchRequest;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiUserContext;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiUserLocation;
import com.hackathon.second_hand_first.user.domain.User;
import com.hackathon.second_hand_first.user.repository.UserRepository;
import com.hackathon.second_hand_first.search.integration.ai.dto.AiSearchResponse;
import com.hackathon.second_hand_first.search.repository.SearchSessionRepository;
import com.hackathon.second_hand_first.search.repository.SearchResultRepository;
import com.hackathon.second_hand_first.search.repository.SearchMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSessionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SearchSessionRepository searchSessionRepository;
    private final SearchResultRepository searchResultRepository;
    private final SearchMessageRepository searchMessageRepository;
    private final AiSearchClient aiSearchClient;
    private final ProductUpsertService productUpsertService;
    private final UserRepository userRepository;

    @Transactional
    public SearchSessionCreateResponse create(
            Long userId,
            SearchSessionCreateRequest request
    ) {
        String sessionId = generateSessionId();
        AiSearchResponse aiResponse = aiSearchClient.search(
                new AiSearchRequest(
                        request.query(),
                        null,
                        sessionId,
                        null,
                        buildUserContext(userId)
                )
        );
        validateAiResponse(aiResponse);
        AiParsedConditionsResponse analysis = aiResponse.parsedConditions();
        SearchSession session = SearchSession.create(
                sessionId,
                userId,
                request.query()
        );
        session.complete(
                analysis.keyword(),
                analysis.querySummary(),
                aiResponse.assistantMessage(),
                analysis.maxPrice(),
                analysis.priority(),
                analysis.conditions(),
                aiResponse.resultCount()
        );
        SearchSession saved = searchSessionRepository.save(session);
        searchMessageRepository.save(SearchMessage.create(
                generateMessageId(),
                saved,
                aiResponse.assistantMessage()
        ));
        saveSearchResults(saved, aiResponse.products());
        return SearchSessionCreateResponse.of(saved, aiResponse);
    }

    public SearchSessionPageResponse getRecentSessions(
            Long userId,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        Page<RecentSearchSessionResponse> result = searchSessionRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page, size))
                .map(RecentSearchSessionResponse::from);
        return SearchSessionPageResponse.from(result);
    }

    public SearchSessionDetailResponse getSession(Long userId, String sessionId) {
        SearchSession session = searchSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(SearchSessionNotFoundException::new);
        List<SearchMessage> messages = searchMessageRepository
                .findBySearchSessionIdOrderByCreatedAtAscIdAsc(session.getId());
        return SearchSessionDetailResponse.of(session, messages);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("요청 값이 올바르지 않습니다.");
        }
    }

    private void saveSearchResults(
            SearchSession session,
            List<AiRecommendedProductResponse> recommendations
    ) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        validateRecommendations(recommendations);
        List<SearchResult> results = recommendations.stream()
                .map(recommendation -> {
                    Product product = productUpsertService.upsert(recommendation.product());
                    return SearchResult.create(
                            session,
                            product,
                            recommendation.rank(),
                            recommendation.recommendationScore(),
                            recommendation.recommendationReason()
                    );
                })
                .toList();
        searchResultRepository.saveAll(results);
    }

    /**
     * 사용자 프로필에서 위치를 읽어 AI에 넘길 맥락을 만든다.
     *
     * <p>프론트는 검색할 때 위치를 보내지 않는다. 로그인 사용자이므로 여기서 채운다.
     * 위치를 등록하지 않은 사용자면 null을 준다 — 없는 좌표를 지어내면 AI 쪽에서
     * 엉뚱한 상품이 "가장 가까운 상품"이 된다.
     *
     * <p>편의점 픽업 가능 여부는 아직 수집하지 않아 null로 둔다. 기본값 true로 두면
     * 편의점 반값택배를 포함한 최저 배송비가 쓰이는데, 주변에 그 편의점이 없는
     * 사용자에게는 존재하지 않는 가격이다.
     */
    private AiUserContext buildUserContext(Long userId) {
        return userRepository.findById(userId)
                .map(SearchSessionService::toUserContext)
                .orElse(null);
    }

    private static AiUserContext toUserContext(User user) {
        if (user.getRegion() == null && user.getLatitude() == null) {
            return null;
        }
        return new AiUserContext(
                new AiUserLocation(user.getRegion(), user.getLatitude(), user.getLongitude()),
                null
        );
    }

    private void validateAiResponse(AiSearchResponse response) {
        if (response == null
                || response.parsedConditions() == null
                || response.assistantMessage() == null
                || response.assistantMessage().isBlank()
                || response.resultCount() < 0) {
            throw new IllegalArgumentException("AI 검색 응답이 올바르지 않습니다.");
        }
    }

    private void validateRecommendations(List<AiRecommendedProductResponse> recommendations) {
        Set<Integer> ranks = new HashSet<>();
        Set<String> productKeys = new HashSet<>();
        for (AiRecommendedProductResponse recommendation : recommendations) {
            if (recommendation == null || recommendation.product() == null) {
                throw new IllegalArgumentException("AI 추천 상품 응답이 올바르지 않습니다.");
            }
            if (!ranks.add(recommendation.rank())) {
                throw new IllegalArgumentException("AI 추천 순위가 중복되었습니다.");
            }
            String productKey = recommendation.product().platform()
                    + ":" + recommendation.product().externalProductId();
            if (!productKeys.add(productKey)) {
                throw new IllegalArgumentException("AI 추천 상품이 중복되었습니다.");
            }
        }
    }

    private String generateSessionId() {
        return "ss_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }
}
